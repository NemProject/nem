package org.nem.nis.poi.graph;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.internal.util.collections.Sets;
import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.harvesting.CanHarvestPredicate;
import org.nem.nis.poi.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.test.NisUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NxtGraphClusteringITCase {
	private static final Logger LOGGER = Logger.getLogger(NxtGraphClusteringITCase.class.getName());
	private static final PoiOptionsBuilder DEFAULT_POI_OPTIONS_BUILDER = new PoiOptionsBuilder();
	private static final PoiOptions DEFAULT_POI_OPTIONS = DEFAULT_POI_OPTIONS_BUILDER.create();

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
		final HashMap<Long, Long> stakes = this.getStakes(transactionData);
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
	public void canWriteImportancesToFile() throws IOException {
		final String options = String.format(
				"_%smin_%dmu_%fepsilon",
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				DEFAULT_POI_OPTIONS.getMuClusteringValue(),
				DEFAULT_POI_OPTIONS.getEpsilonClusteringValue());

		// Arrange
		final int endHeight = 225000;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);

		// 0. Load account states.
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

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

		String output = "'address', 'stake', 'importance', 'outlinkCount', 'outlinkSum'\n";
		for (int i = 0; i < importances.size(); ++i) {
			output += addresses.get(i) + "," + stakes.get(i) + "," + importances.getAt(i) + "," + outlinkCounts.get(i) + "," + outlinkSums.get(i) + "\n";
		}

		FileUtils.writeStringToFile(new File("kaiseki/importances" + options + ".csv"), output);
	}

	@Ignore
	@Test
	public void canWriteImportancesToFileForManyDifferentParameters() throws IOException {

		// compute Cartesian product of considered parameters
		final Set<Long> minHarvesterBalances = Sets.newSet(1l, 100l, 500l, 1000l, 10000l, 100000l);
		final Set<Long> minOutlinkWeights = Sets.newSet(0l, 1l, 100l, 1000l, 10000l);
		final Set<Double> negativeOutlinkWeights = Sets.newSet(0., 20., 40., 60., 80., 100.);
		final Set<Double> outlierWeights = Sets.newSet(0.85, 0.9, 0.95, 1.0);
		final Set<Integer> mus = Sets.newSet(1, 2, 3, 4, 5);
		final Set<Double> epsilons = Sets.newSet(0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95);
		final Set<TeleportationProbabilities> teleporationProbabilities = Sets.newSet(
				new TeleportationProbabilities(0.75, 0.1),
				new TeleportationProbabilities(0.65, 0.1),
				new TeleportationProbabilities(0.55, 0.1),
				new TeleportationProbabilities(0.75, 0.2),
				new TeleportationProbabilities(0.65, 0.2),
				new TeleportationProbabilities(0.55, 0.2));

		// how I learned to stop worrying and love the loop
		for (final long minHarvesterBalance : minHarvesterBalances) {
			for (final long minOutlinkWeight : minOutlinkWeights) {
				for (final double negativeOutlinkWeight : negativeOutlinkWeights) {
					for (final double outlierWeight : outlierWeights) {
						for (final int mu : mus) {
							for (final double epsilon : epsilons) {
								for (final TeleportationProbabilities teleporationPair : teleporationProbabilities) {
									final PoiOptionsBuilder optionsBuilder = createBuilderWithCustomOptions(
											minHarvesterBalance,
											minOutlinkWeight,
											negativeOutlinkWeight,
											outlierWeight,
											mu,
											epsilon,
											teleporationPair.teleporationProb,
											teleporationPair.interLevelTeleporationProb);

									final String options = String.format(
											"_%sminBalance_%sminOutlink_%snegOutlink_%soutlierWeight_%smu_%sepsilon_%stelPro_%sinterLevelProb",
											minHarvesterBalance,
											minOutlinkWeight,
											negativeOutlinkWeight,
											outlierWeight,
											mu,
											epsilon,
											teleporationPair.teleporationProb,
											teleporationPair.interLevelTeleporationProb);

									// Arrange
									final int endHeight = 225000;
									final BlockHeight endBlockHeight = new BlockHeight(endHeight);

									// 0. Load account states.
									final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

									// 1. calc importances
									final ColumnVector importances = getAccountImportances(
											new BlockHeight(endHeight),
											eligibleAccountStates,
											optionsBuilder);

									final List<Long> stakes = eligibleAccountStates.stream()
											.map(acct -> acct.getWeightedBalances().getVested(endBlockHeight).add(acct.getWeightedBalances().getUnvested(
													endBlockHeight)).getNumMicroNem())
											.collect(Collectors.toList());

									final List<String> addresses = eligibleAccountStates.stream()
											.map(acct -> acct.getAddress().getEncoded())
											.collect(Collectors.toList());

									final List<Integer> outlinkCounts = eligibleAccountStates.stream()
											.map(acct -> acct.getImportanceInfo().getOutlinksSize(endBlockHeight))
											.collect(Collectors.toList());

									final List<Long> outlinkSums = eligibleAccountStates.stream()
											.map(acct -> {
												final ArrayList<Long> amounts = new ArrayList<>();
												acct.getImportanceInfo()
														.getOutlinksIterator(endBlockHeight)
														.forEachRemaining(i -> amounts.add(i.getAmount().getNumMicroNem()));
												return amounts.stream().mapToLong(i -> i).sum();
											})
											.collect(Collectors.toList());

									String output = "'address', 'stake', 'importance', 'outlinkCount', 'outlinkSum'\n";
									for (int i = 0; i < importances.size(); ++i) {
										output += addresses.get(i) + "," + stakes.get(i) + "," + importances.getAt(i) + "," + outlinkCounts.get(i) + "," +
												outlinkSums.get(i) + "\n";
									}

									FileUtils.writeStringToFile(new File("kaiseki/importances" + options + ".csv"), output);
								}
							}
						}
					}
				}
			}
		}
	}

	//endregion

	//region min harvesting balance

	/**
	 * Using L2 distance as a proxy for importance sensitivity to min harvesting balance.
	 * TODO 20141014 J-J: recalculate differences using pearson r
	 * TODO 20141015 BR -> J: nice test. I agree to raise the min harvest balance to the suggested value.
	 *
	 * min-balance - distance from stakes
	 * 1:      0.013472
	 * 100:    0.013472
	 * 1000:   0.013472
	 * 10000:  0.012213 <-- seems reasonable to bump up to 10000 with minimal changes
	 * 100000: 0.008058
	 */
	@Test
	public void minHarvestingBalanceVariance() {
		// Arrange:
		final int endHeight = 225000;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);
		final Set<Long> minHarvesterBalances = Sets.newSet(1L, 100L, 500L, 1000L, 10000L, 100000L);
		final Map<Long, ColumnVector> minBalanceToImportanceMap = new HashMap<>();

		// calculate importances
		for (final Long minHarvesterBalance : minHarvesterBalances) {
			final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
			optionsBuilder.setMinHarvesterBalance(Amount.fromNem(minHarvesterBalance));

			final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);
			final ColumnVector importances = getAccountImportances(endBlockHeight, eligibleAccountStates, optionsBuilder);
			minBalanceToImportanceMap.put(minHarvesterBalance, importances);
		}

		// calculate balances
		final ColumnVector balances = getBalances(endBlockHeight, loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER));
		balances.normalize();

		for (final Map.Entry<Long, ColumnVector> entry : minBalanceToImportanceMap.entrySet()) {
			final double distance = balances.l2Distance(entry.getValue());
			LOGGER.info(String.format("distance between stakes and %d: %f", entry.getKey(), distance));
		}
	}

	private static ColumnVector getBalances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		final List<Amount> balances = accountStates.stream()
				.map(a -> a.getWeightedBalances().getVested(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector balancesVector = new ColumnVector(balances.size());
		for (int i = 0; i < balances.size(); ++i) {
			balancesVector.setAt(i, balances.get(i).getNumNem());
		}

		LOGGER.info(String.format("balances: %s", balancesVector));
		return balancesVector;
	}

	//endregion

	//region poiComparisonTest

	// TODO 20141015 BR: this test used to pass but now it fails. What changed?
	@Test
	public void poiComparisonTest() {
		// Arrange:
		final int endHeight = 300000;//225000;

		// a) This is the warm up phase.
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
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

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

	//endregion

	//region influence of epsilon

	/**
	 * Analyzes the influence of the value of epsilon on the number of clusters, the average cluster size and the number of hubs.
	 * The minimum harvester balance is set to 10000.
	 * (unfortunately cluster information is only internally available, so it is only logged. You have to look for the entries yourself).
	 *
	 * epsilon = 0.75
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     1    |    3.00   |          1.00           |  0
	 *   50000   |     6    |    3.00   |          1.20           |  0
	 *  100000   |     8    |    3.12   |          0.80           |  5
	 *  150000   |     9    |    3.11   |          0.60           |  5
	 *  200000   |    13    |    3.15   |          0.65           |  5
	 *
	 * epsilon = 0.65 (standard value)
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     2    |    3.00   |          3.00           |  0
	 *   50000   |    14    |    3.29   |          2.80           |  3
	 *  100000   |    16    |    3.19   |          1.60           |  8
	 *  150000   |    24    |    3.33   |          1.60           |  5
	 *  200000   |    33    |    3.33   |          1.65           |  8
	 *
	 * epsilon = 0.55
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     6    |    3.67   |          6.00           |  1
	 *   50000   |    26    |    3.73   |          5.20           |  9
	 *  100000   |    29    |    3.45   |          2.90           | 10
	 *  150000   |    44    |    3.70   |          2.93           | 13
	 *  200000   |    53    |    2.65   |          2.65           | 19
	 *
	 * epsilon = 0.45
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |    10    |    4.70   |         10.00           |  5
	 *   50000   |    37    |    4.03   |          7.40           | 17
	 *  100000   |    47    |    3.96   |          4.70           | 24
	 *  150000   |    64    |    4.25   |          4.26           | 39
	 *  200000   |    77    |    3.86   |          3.85           | 47
	 *
	 * epsilon = 0.35
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     8    |    8.88   |          8.00           |  5
	 *   50000   |    38    |    6.03   |          7.60           | 18
	 *  100000   |    44    |    6.98   |          4.40           | 30
	 *  150000   |    63    |    6.52   |          4.26           | 29
	 *  200000   |    81    |    5.91   |          4.05           | 48
	 *
	 * epsilon = 0.30
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     6    |   13.67   |          6.00           |  3
	 *   50000   |    22    |   11.45   |          4.40           |  6
	 *  100000   |    33    |   11.18   |          3.30           | 54
	 *  150000   |    48    |    9.77   |          3.20           | 16
	 *  200000   |    58    |    9.91   |          2.90           | 27
	 *
	 * epsilon = 0.25
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     4    |   21.00   |          4.00           |  0
	 *   50000   |    16    |   17.44   |          3.20           |  4
	 *  100000   |    22    |   18.86   |          2.20           | 38
	 *  150000   |    28    |   18.61   |          1.87           | 12
	 *  200000   |    38    |   17.63   |          1.90           |  9
	 *
	 * epsilon = 0.20
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     3    |   34.67   |          3.00           |  0
	 *   50000   |    12    |   28.83   |          2.40           |  1
	 *  100000   |    17    |   27.71   |          1.70           |  0
	 *  150000   |    20    |   31.15   |          1.33           |  5
	 *  200000   |    28    |   26.64   |          1.40           |  5
	 *
	 * epsilon = 0.15
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     3    |   37.33   |          3.00           |  0
	 *   50000   |    10    |   39.00   |          2.00           |  0
	 *  100000   |    13    |   40.77   |          1.30           |  0
	 *  150000   |    14    |   49.29   |          0.93           |  0
	 *  200000   |    20    |   39.60   |          1.00           |  0
	 *
	 * epsilon = 0.05
	 * endheight | clusters | avg. size | new clusters/10k blocks | hubs
	 *   10000   |     2    |   56.00   |          2.00           |  0
	 *   50000   |     8    |   55.62   |          1.60           |  0
	 *  100000   |     2    |  485.50   |          0.20           |  0
	 *  150000   |     7    |  175.14   |          0.47           |  0
	 *  200000   |    10    |  110.60   |          0.50           |  0
	 *
	 */
	@Test
	public void epsilonInfluenceOnNumberOfClustersAndClusterSize() {
		// Arrange:
		final int endHeight = 200000;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);
		final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
		optionsBuilder.setMinHarvesterBalance(Amount.fromNem(10000));
		optionsBuilder.setEpsilonClusteringValue(0.20);

		// Act:
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(0, endHeight, optionsBuilder);
		getAccountImportances(endBlockHeight, eligibleAccountStates, optionsBuilder);
	}

	// endregion

	private static Collection<PoiAccountState> loadEligibleHarvestingAccountStates(
			final long startHeight,
			final long endHeight,
			final PoiOptionsBuilder optionsBuilder) {
		return loadEligibleHarvestingAccountStates(startHeight, endHeight, optionsBuilder.create().getMinHarvesterBalance());
	}

	private static Collection<PoiAccountState> loadEligibleHarvestingAccountStates(
			final long startHeight,
			final long endHeight,
			final Amount minHarvesterBalance) {
		final Collection<NxtTransaction> transactionData = loadTransactionData(startHeight, endHeight);
		final Map<Address, PoiAccountState> accountStateMap = createAccountStatesFromTransactionData(transactionData);
		return selectHarvestingEligibleAccounts(accountStateMap, new BlockHeight(endHeight), minHarvesterBalance);
	}

	private static Map<Address, PoiAccountState> createAccountStatesFromTransactionData(final Collection<NxtTransaction> transactions) {
		LOGGER.info("Creating PoiAccountStates from transaction data...");

		final Map<Address, PoiAccountState> accountStateMap = new HashMap<>();

		// 1. Create accounts in the genesis block.
		final Amount nxtGenesisAmount = Amount.fromNem(1000000000); // 10^9
		final PoiAccountState genesis = createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1, nxtGenesisAmount);
		accountStateMap.put(genesis.getAddress(), genesis);

		// 2. Iterate through transactions, creating new accounts as needed.
		for (final NxtTransaction trans : transactions) {
			final Amount amount = Amount.fromNem(trans.getAmount() / 100000000); // NXT stores NXT * 10^8 (ignore micro nem)
			final Address sender = Address.fromEncoded(Long.toString(trans.getSenderId()));
			final Address recipient = Address.fromEncoded(Long.toString(trans.getRecipientId()));
			final BlockHeight blockHeight = new BlockHeight(trans.getHeight() + 1); // NXT blocks start at 0 but NEM blocks start at 1

			if (!accountStateMap.containsKey(recipient)) {
				accountStateMap.put(recipient, new PoiAccountState(recipient));
			}

			final PoiAccountState senderAccountState = accountStateMap.get(sender);
			final PoiAccountState recipientAccountState = accountStateMap.get(recipient);
			final long balance = senderAccountState.getWeightedBalances().getVested(blockHeight).getNumMicroNem() +
					senderAccountState.getWeightedBalances().getUnvested(blockHeight).getNumMicroNem();

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
			final BlockHeight height,
			final Amount minHarvesterBalance) {
		final CanHarvestPredicate canHarvestPredicate = new CanHarvestPredicate(minHarvesterBalance);
		return accountStateMap.values().stream()
				.filter(accountState -> canHarvestPredicate.canHarvest(accountState, height))
				.collect(Collectors.toList());
	}

	private static double calculateDifference(final ColumnVector lhs, final ColumnVector rhs) {
		Assert.assertThat(lhs.size(), IsEqual.equalTo(rhs.size()));
		final ColumnVector ratios = new ColumnVector(lhs.size());
		double diff = 0;
		double maxRatio = 1.0;
		for (int i = 0; i < rhs.size(); ++i) {
			diff += Math.abs(rhs.getAt(i) - lhs.getAt(i));
			if (lhs.getAt(i) > 0.0) {
				ratios.setAt(i, rhs.getAt(i) / lhs.getAt(i));
			} else if (rhs.getAt(i) > 0.0) {
				ratios.setAt(i, Double.MAX_VALUE);
			} else {
				ratios.setAt(i, 1.0);
			}
			maxRatio = ratios.getAt(i) > maxRatio? ratios.getAt(i) : maxRatio;
			if (ratios.getAt(i) > 1.001 || ratios.getAt(i) < 0.999) {
				LOGGER.info("Account " + i + " importance ratio is " + ratios.getAt(i));
			}
		}

		LOGGER.info(String.format("diff: %f; ratios: %s", diff, ratios));
		LOGGER.info(String.format("maximal ratio is: %f", maxRatio));
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

		final SparseMatrix outlinkMatrix = this.createNetOutlinkMatrix(startHeight, stopHeight);
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

		final SparseMatrix outlinkMatrix = this.createNetOutlinkMatrix(startHeight, stopHeight);
		final ClusteringResult result = calculateClusteringResult(new FastScanClusteringStrategy(), outlinkMatrix);
		LOGGER.info(String.format("The clusterer found %d regular clusters, %d hubs, and %d outliers",
				result.getClusters().size(),
				result.getHubs().size(),
				result.getOutliers().size()));
	}

	private SparseMatrix createNetOutlinkMatrix(final long startHeight, final long endHeight) {
		final Collection<PoiAccountState> eligibleAccountStates = loadEligibleHarvestingAccountStates(startHeight, endHeight, DEFAULT_POI_OPTIONS_BUILDER);
		final PoiContext poiContext = new PoiContext(eligibleAccountStates, new BlockHeight(endHeight), DEFAULT_POI_OPTIONS);
		return poiContext.getOutlinkMatrix();
	}

	private static ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final SparseMatrix outlinkMatrix) {
		final long start = System.currentTimeMillis();
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final long stop = System.currentTimeMillis();
		LOGGER.info("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighborMap, strategy);
		return clusterer.cluster(neighborhood);
	}

	//endregion

	private static Collection<NxtTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		return ExceptionUtils.propagate(() -> {
			// Arrange:
			try (final NxtDatabaseRepository repository = new NxtDatabaseRepository()) {
				// Act:
				return repository.loadTransactionData(startHeight, stopHeight);
			}
		});
	}

	private static PoiAccountState createAccountWithBalance(final Address address, final long blockHeight, final Amount amount) {
		final PoiAccountState state = new PoiAccountState(address);
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), amount);
		return state;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> acctStates,
			final GraphClusteringStrategy clusteringStrategy) {
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setClusteringStrategy(clusteringStrategy);
		return getAccountImportances(blockHeight, acctStates, poiOptionsBuilder);
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> acctStates,
			final PoiOptionsBuilder poiOptionsBuilder) {
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(new PoiScorer(), poiOptionsBuilder.create());
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

	private static PoiOptionsBuilder createBuilderWithCustomOptions(
			final long minHarvesterBalance,
			final long minOutlinkWeight,
			final double negativeOutlinkWeight,
			final double outlierWeight,
			final int mu,
			final double epsilon,
			final double teleporationProb,
			final double interLevelTeleportationProb) {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setMinHarvesterBalance(Amount.fromNem(minHarvesterBalance));
		builder.setMinOutlinkWeight(Amount.fromNem(minOutlinkWeight));
		builder.setNegativeOutlinkWeight(negativeOutlinkWeight);
		builder.setOutlierWeight(outlierWeight);
		builder.setMuClusteringValue(mu);
		builder.setEpsilonClusteringValue(epsilon);
		builder.setTeleportationProbability(teleporationProb);
		builder.setInterLevelTeleportationProbability(interLevelTeleportationProb);
		return builder;
	}

	private class TeleportationProbabilities {
		final double teleporationProb;
		final double interLevelTeleporationProb;

		TeleportationProbabilities(final double teleporationProb, final double interLevelTeleporationProb) {
			this.teleporationProb = teleporationProb;
			this.interLevelTeleporationProb = interLevelTeleporationProb;
		}
	}
}
