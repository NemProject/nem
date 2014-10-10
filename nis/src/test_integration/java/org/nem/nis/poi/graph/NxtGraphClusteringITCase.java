package org.nem.nis.poi.graph;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.harvesting.CanHarvestPredicate;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;

import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NxtGraphClusteringITCase {
	private static final Logger LOGGER = Logger.getLogger(NxtGraphClusteringITCase.class.getName());
	private static final PoiOptions DEFAULT_POI_OPTIONS = new PoiOptionsBuilder().create();

	@Test
	public void canQueryNxtTransactionTable() {
		// Act:
		final Collection<NxtTransaction> transactions = loadTransactionData(0, 0);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(73));
	}

	//region print (non-tests)

	@Ignore
	@Test
	public void canPrintStakes() {
		// Act:
		final Collection<NxtTransaction> transactionData = loadTransactionData(0, 300000);
		final HashMap<Long, Long> stakes = getStakes(transactionData);
		LOGGER.info(stakes.toString());
	}

	private HashMap<Long, Long> getStakes(final Collection<NxtTransaction> transactionData) {
		final HashMap<Long, Long> acctStakes = new HashMap<>();

		for (final NxtTransaction trans : transactionData) {
			final long recipientId = trans.getRecipientId();
			final long senderId = trans.getSenderId();
			final long amount = trans.getAmount();
			acctStakes.put(recipientId, acctStakes.getOrDefault(recipientId, 0L) + amount);
			acctStakes.put(senderId, acctStakes.getOrDefault(senderId, 0L) - amount);
		}

		return acctStakes;
	}

	@Ignore
	@Test
	public void canPrintImportances() throws IOException {
		final String options = String.format(
				"_%smin_%dmu_%fepsilon",
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				GraphConstants.MU,
				GraphConstants.EPSILON);

		// Arrange
		final int endHeight = 225000;//was 300k
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);

		// 0. Load account states.
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight);

		// 1. calc importances
		final ColumnVector importances = getAccountImportances(
				new BlockHeight(endHeight),
				eligibleAccountStates,
				new FastScanClusteringStrategy());

		final List<Long> stakes = eligibleAccountStates.stream()
				.map(acct -> acct.getWeightedBalances().getVested(endBlockHeight).add(acct.getWeightedBalances().getUnvested(endBlockHeight)).getNumMicroNem())
				.collect(Collectors.toList());

		final List<String> addresses = eligibleAccountStates.stream()
				.map(acct -> acct.getAddress().getEncoded())
				.collect(Collectors.toList());

		final List<Integer> outlinkCounts = eligibleAccountStates.stream()
				.map(acct -> acct.getImportanceInfo().getOutlinksSize(endBlockHeight))
				.collect(Collectors.toList());

		final List<Long> outlinkSums = eligibleAccountStates.stream()
				.map(acct -> {
							final ArrayList<Long> amts = new ArrayList<>();
							acct.getImportanceInfo()
									.getOutlinksIterator(endBlockHeight)
									.forEachRemaining(i -> amts.add(i.getAmount().getNumMicroNem()));
							return amts.stream().mapToLong(i -> i).sum();
				})
				.collect(Collectors.toList());

		FileUtils.writeStringToFile(new File("importances" + options + ".csv"), Arrays.toString(importances.getRaw()));
		FileUtils.writeStringToFile(new File("stakes" + options + ".csv"), stakes.toString());
		FileUtils.writeStringToFile(new File("addresses" + options + ".csv"), addresses.toString());
		FileUtils.writeStringToFile(new File("outlinkCount" + options + ".csv"), outlinkCounts.toString());
		FileUtils.writeStringToFile(new File("outlinkSums" + options + ".csv"), outlinkSums.toString());
	}

	//endregion

	//region poiComparisonTest

	@Test
	public void poiComparisonTest() {
		// Arrange:
		final int endHeight = 5000;//225000;

		// a) This is the warm up phase. I dunno why it is needed but the first time java needs a lot longer for the calculation
		getAccountImportances(endHeight, new OutlierScan(), "WARM UP");

		// Act:
		// b) these are actual importances
		final ColumnVector fastScanImportances = getAccountImportances(endHeight, new FastScanClusteringStrategy(), "FastScan");
		final ColumnVector outlierScanImportances = getAccountImportances(endHeight, new OutlierScan(), "OutlierScan");
		final ColumnVector singleClusterScanImportances = getAccountImportances(endHeight, new SingleClusterScan(), "SingleClusterScan");

		// Assert:
		assertDifference("FastScan vs SingleClusterScan", fastScanImportances, singleClusterScanImportances);
		assertDifference("FastScan vs OutlierScan", fastScanImportances, outlierScanImportances);
		assertDifference("SingleClusterScan vs OutlierScan", singleClusterScanImportances, outlierScanImportances);
	}

	private static void assertDifference(final String message, final ColumnVector lhs, final ColumnVector rhs) {
		// Assert:
		LOGGER.info(message);
		final double difference = calculateDifference(lhs, rhs);
		Assert.assertTrue(message, difference > 0.01);
	}

	private static ColumnVector getAccountImportances(
			final long endHeight,
			final GraphClusteringStrategy clusteringStrategy,
			final String name) {
		// 0. Load transactions.
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight);

		LOGGER.info(String.format("*** Poi calculation: %s **", name));
		final long start = System.currentTimeMillis();
		final ColumnVector importanceVector = getAccountImportances(
				new BlockHeight(endHeight),
				eligibleAccountStates,
				clusteringStrategy);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Calculating importances needed %d ms.", stop - start));
		return importanceVector;
	}

	private static Collection<PoiAccountState> loadEligibleHarvestingAccountStates(final long startHeight, final long endHeight) {
		final Collection<NxtTransaction> transactionData = loadTransactionData(startHeight, endHeight);
		final Map<Address, PoiAccountState> accountStateMap = createAccountStatesFromTransactionData(transactionData);
		return selectHarvestingEligibleAccounts(accountStateMap, new BlockHeight(endHeight));
	}

	private static Map<Address, PoiAccountState> createAccountStatesFromTransactionData(final Collection<NxtTransaction> transactions) {
		LOGGER.info("Creating PoiAccountStates from transaction data...");

		final Map<Address, PoiAccountState> accountStateMap = new HashMap<>();

		// 1. Create accounts in the genesis block.
		final PoiAccountState genesis = createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1, 1000000000000000l);
		accountStateMap.put(genesis.getAddress(), genesis);

		// 2. Iterate through transactions, creating new accounts as needed.
		for (final NxtTransaction trans : transactions) {
			final Amount amount = Amount.fromMicroNem(trans.getAmount() / 100); // TODO 20141006 J-M why / 100 ?
			final Address sender = Address.fromEncoded(Long.toString(trans.getSenderId()));
			final Address recipient = Address.fromEncoded(Long.toString(trans.getRecipientId()));
			final BlockHeight blockHeight = new BlockHeight(trans.getHeight() + 1); // TODO 20141006 J-M why + 1 ?

			if (!accountStateMap.containsKey(recipient)) {
				accountStateMap.put(recipient, new PoiAccountState(recipient));
			}

			final PoiAccountState senderAccountState = accountStateMap.get(sender);
			final PoiAccountState recipientAccountState = accountStateMap.get(recipient);
			final long balance = senderAccountState.getWeightedBalances().getVested(blockHeight).getNumMicroNem() + senderAccountState.getWeightedBalances().getUnvested(blockHeight).getNumMicroNem();

			// We need to add some balance sometimes because the transactions don't account for fees earned from forged blocks
			final long remainingBalance = balance - amount.getNumMicroNem();
			if (remainingBalance < 0) {
				//System.out.println("balance: " + balance);
				senderAccountState.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight.getRaw()), Amount.fromMicroNem(amount.getNumMicroNem()));
				//final long balance2 = senderAccountState.getWeightedBalances().getVested(blockHeight).getNumMicroNem() + senderAccountState.getWeightedBalances().getUnvested(blockHeight).getNumMicroNem();
				//System.out.println("balance2: " + balance2);
				//System.out.println("amount: " + amount.getNumMicroNem());
				//System.out.println("amount*2: " + amount.getNumMicroNem()*2);
			}


			senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
			senderAccountState.getImportanceInfo().addOutlink(
					new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

			recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);

			//this.addOutlink(sender, recipient, new BlockHeight(blockHeight), amount / 100);

			// add some random outlinks to make things more interesting, because no one uses nxt :(
			//			if (Math.random() < 0.1) {
			//				// send 1337 NEM
			//				String randSender = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				String randReceiver = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				this.addOutlink(acctMap.get(randSender + ""), acctMap.get(randReceiver + ""), new BlockHeight(blockHeight), 1337000000);
			//			}
		}

		LOGGER.info("Creating PoiAccountStates finished...");
		return accountStateMap;
	}

	private static Collection<PoiAccountState> selectHarvestingEligibleAccounts(
			final Map<Address, PoiAccountState> accountStateMap,
			final BlockHeight height) {
		final CanHarvestPredicate canHarvestPredicate = new CanHarvestPredicate(DEFAULT_POI_OPTIONS.getMinHarvesterBalance());
		return  accountStateMap.values().stream()
				.filter(accountState -> canHarvestPredicate.canHarvest(accountState, height))
				.collect(Collectors.toList());
	}

	private static double calculateDifference(final ColumnVector lhs, final ColumnVector rhs) {
		Assert.assertThat(lhs.size(), IsEqual.equalTo(rhs.size()));
		final ColumnVector ratios = new ColumnVector(lhs.size());
		double diff = 0;
		for (int i = 0; i < rhs.size(); ++i) {
			diff += Math.abs(rhs.getAt(i) - lhs.getAt(i));
			if (lhs.getAt(i) > 0.0) {
				ratios.setAt(i, rhs.getAt(i) / lhs.getAt(i));
			} else if (rhs.getAt(i) > 0.0) {
				ratios.setAt(i, Double.MAX_VALUE);
			} else {
				ratios.setAt(i, 1.0);
			}
			if (ratios.getAt(i) > 1.001 || ratios.getAt(i) < 0.999) {
				LOGGER.info("Account " + i + " importance ratio is " + ratios.getAt(i));
			}
		}

		LOGGER.info(String.format("diff: %f; ratios: %s", diff, ratios));
		LOGGER.finest(lhs.toString());
		LOGGER.finest(rhs.toString());
		return diff;
	}

	//endregion

	//region graph (non-tests)

	@Ignore
	@Test
	public void nxtGraphViewTest() throws SQLException, IOException {
		final long startHeight = 0;
		final long stopHeight = 10000;

		final SparseMatrix outlinkMatrix = createNetOutlinkMatrix(startHeight, stopHeight);
		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
		viewer.saveGraph();
		//viewer.showGraph();
	}

	@Ignore
	@Test
	public void nxtGraphClusteringTest() throws SQLException {
		final int startHeight = 100000;
		final int stopHeight = 200000;//300000;

		final SparseMatrix outlinkMatrix = createNetOutlinkMatrix(startHeight, stopHeight);
		final ClusteringResult result = calculateClusteringResult(new FastScanClusteringStrategy(), outlinkMatrix);
		LOGGER.info(String.format("The clusterer found %d regular clusters, %d hubs, and %d outliers",
				result.getClusters().size(),
				result.getHubs().size(),
				result.getOutliers().size()));
	}

	private SparseMatrix createNetOutlinkMatrix(final long startHeight, final long endHeight) {
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(startHeight, endHeight);
		final PoiContext poiContext = new PoiContext(eligibleAccountStates, new BlockHeight(endHeight), new FastScanClusteringStrategy(), DEFAULT_POI_OPTIONS);
		return poiContext.getOutlinkMatrix();
	}

	private static ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final SparseMatrix outlinkMatrix) {
		final long start = System.currentTimeMillis();
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final long stop = System.currentTimeMillis();
		LOGGER.info("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighborMap, strategy);
		return clusterer.cluster(neighborhood);
	}

	//endregion

	private static Collection<NxtTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		return ExceptionUtils.propagate(() -> {
			// Arrange:
			try (final NxtDatabaseRepository repository = new NxtDatabaseRepository())  {
				// Act:
				return repository.loadTransactionData(startHeight, stopHeight);
			}
		});
	}

	private static PoiAccountState createAccountWithBalance(final Address address, final long blockHeight, final long numNEM) {
		final PoiAccountState state = new PoiAccountState(address);
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), Amount.fromMicroNem(numNEM));
		return state;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> acctStates,
			final GraphClusteringStrategy clusteringStrategy) {
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(new PoiScorer(), clusteringStrategy, DEFAULT_POI_OPTIONS);
		importanceCalculator.recalculate(blockHeight, acctStates);
		final List<Double> importances = acctStates.stream()
				.map(a -> a.getImportanceInfo().getImportance(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		return importancesVector;
	}
}
