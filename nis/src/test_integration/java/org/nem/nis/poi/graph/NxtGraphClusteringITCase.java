package org.nem.nis.poi.graph;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.AccountLink;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NxtGraphClusteringITCase {
	private static final Logger LOGGER = Logger.getLogger(NxtGraphClusteringITCase.class.getName());

	static final String JDBC_DRIVER = "org.h2.Driver";
	static Connection conn;
	static Statement stmt;

	@Test
	public void canOpenAndCloseNxtDatabase() {
		// Act:
		conn = openNxtDatabase(getDbUrl());

		// Assert
		Assert.assertThat(conn, IsNull.notNullValue());
		Assert.assertThat(closeH2Database(), IsEqual.equalTo(true));
	}

	@Test
	public void canPrintStakes() throws SQLException {
		final int startHeight = 0;
		final int stopHeight = 300000;

		final long start = System.currentTimeMillis();
		final List<NxtTransaction> transactionData = getTransactionData(startHeight, stopHeight);
		final HashMap<Long, Long> stakes = getStakes(transactionData);
		System.out.println(stakes);
	}

	@Test
	public void canPrintImportances() throws SQLException, IOException {
		final String options = "_" + PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem() + "min_" + /*"100MinTrans_0.05transProb_" +*/ GraphConstants.MU + "mu_" +
				GraphConstants.EPSILON + "epsilon";

		// Arrange
		final int startHeight = 0;
		final int endHeight = 225000;//was 300k
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);

		final List<NxtTransaction> transData = getTransactionData(startHeight, endHeight);
		final HashMap<String, PoiAccountState> acctMap = new HashMap<>();

		// 1. create accounts in the genesis block
		final PoiAccountState genesis = createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1000000000000000l);
		acctMap.put(genesis.getAddress().getEncoded(), genesis);

		// 2. go through blocks, recreating all the transactions up until now, creating new accts as needed
		for (final NxtTransaction trans : transData) {
			final long amount = trans.amount / 100;
			final long sender = trans.senderId;
			final long recip = trans.recipientId;
			final long blockHeight = trans.height + 1;
			System.out.println("blockheight: " + blockHeight);

			if (!acctMap.containsKey(recip + "")) {
				acctMap.put(recip + "", createAccountWithBalance(Address.fromEncoded(recip + ""), 0));
			}
			//            if (amount < 100000000L) {
			//                continue;
			//            }
			this.addOutlink(acctMap.get(sender + ""), acctMap.get(recip + ""), new BlockHeight(blockHeight), amount);

			// add some random outlinks to make things more interesting, because no one uses nxt :(
			//			if (Math.random() < 0.05) {
			//				// send 1337 NEM
			//				String randSender = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				String randReceiver = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				this.addOutlink(acctMap.get(randSender + ""), acctMap.get(randReceiver + ""), new BlockHeight(blockHeight), 1337000000);
			//			}
		}

		// 3. calc importances
		final ColumnVector importances = getAccountImportances(
				endBlockHeight,
				acctMap.values().stream()
						.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem()
						/*&& acct.getImportanceInfo().getOutlinksSize(endBlockHeight) > 1*/) // 1000 NXT
						.collect(Collectors.toList()),
				new FastScanClusteringStrategy());

		final List<Long> stakes = acctMap.values().stream()
				.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem()
					/*&& acct.getImportanceInfo().getOutlinksSize(endBlockHeight) > 1*/)
				.map(acct ->
								acct.getWeightedBalances().getVested(endBlockHeight).getNumMicroNem() +
										acct.getWeightedBalances().getUnvested(endBlockHeight).getNumMicroNem()
				).collect(Collectors.toList());

		final List<String> addresses = acctMap.values().stream()
				.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem()
						/*&& acct.getImportanceInfo().getOutlinksSize(endBlockHeight) > 1*/)
				.map(acct ->
								acct.getAddress().getEncoded()
				).collect(Collectors.toList());

		final List<Integer> outlinkCounts = acctMap.values().stream()
				.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem()
						/*&& acct.getImportanceInfo().getOutlinksSize(endBlockHeight) > 1*/)
				.map(acct ->
								acct.getImportanceInfo().getOutlinksSize(endBlockHeight)
				).collect(Collectors.toList());

		//		acctMap.values().iterator().forEachRemaining(action);
		final List<Long> outlinkSums = acctMap.values().stream()
				.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem()
						/*&& acct.getImportanceInfo().getOutlinksSize(endBlockHeight) > 1*/)
				.map(acct ->
						{
							final ArrayList<Long> amts = new ArrayList<>();

							acct.getImportanceInfo()
									.getOutlinksIterator(endBlockHeight)
									.forEachRemaining(i -> amts.add(i.getAmount().getNumMicroNem()));
							return amts.stream().mapToLong(i -> i).sum();
						}
				)//.collect(Collectors.summingLong(AccountLink::getAmount.getNumMicroNem))
				.collect(Collectors.toList());

		FileUtils.writeStringToFile(new File("importances" + options + ".csv"), Arrays.toString(importances.getRaw()));
		FileUtils.writeStringToFile(new File("stakes" + options + ".csv"), stakes.toString());
		FileUtils.writeStringToFile(new File("addresses" + options + ".csv"), addresses.toString());
		FileUtils.writeStringToFile(new File("outlinkCount" + options + ".csv"), outlinkCounts.toString());
		FileUtils.writeStringToFile(new File("outlinkSums" + options + ".csv"), outlinkSums.toString());
	}

	@Test
	public void poiComparisonTest() throws SQLException {
		final int startHeight = 0;
		final int endHeight = 30000;//225000;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);

		// 0. Load transactions.
		System.out.println("Loading transaction data from nxt database...");
		final List<NxtTransaction> transactionData = getTransactionData(startHeight, endHeight);
		System.out.println("Loading finished: found " + transactionData.size() + " transactions.");

		final HashMap<String, PoiAccountState> acctMap = new HashMap<>();

		// 1. Create accounts in the genesis block.
		final PoiAccountState genesis = createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1000000000000000l);
		acctMap.put(genesis.getAddress().getEncoded(), genesis);

		// 2. Iterate through transactions, creating new accounts as needed.
		System.out.println("Creating PoiAccountStates from transaction data...");
		for (final NxtTransaction trans : transactionData) {
			final long amount = trans.amount;
			final long sender = trans.senderId;
			final long recip = trans.recipientId;
			final long blockHeight = trans.height + 1;

			if (!acctMap.containsKey(recip + "")) {
				acctMap.put(recip + "", createAccountWithBalance(Address.fromEncoded(recip + ""), 0));
			}
			this.addOutlink(acctMap.get(sender + ""), acctMap.get(recip + ""), new BlockHeight(blockHeight), amount / 100);

			// add some random outlinks to make things more interesting, because no one uses nxt :(
			//			if (Math.random() < 0.1) {
			//				// send 1337 NEM
			//				String randSender = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				String randReceiver = (String)acctMap.keySet().toArray()[((int)(Math.random() * acctMap.size()))];
			//				this.addOutlink(acctMap.get(randSender + ""), acctMap.get(randReceiver + ""), new BlockHeight(blockHeight), 1337000000);
			//			}
		}
		System.out.println("Creating PoiAccountStates finished...");

		// 3. Calculate importances.
		//    a) This is the warm up phase. I dunno why it is needed but the first time java needs a lot longer for the calculation
		System.out.println("Warm up phase started");
		getAccountImportances(
				endBlockHeight,
				acctMap.values().stream()
						.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem())
						.collect(Collectors.toList()),
				new SingleClusterScan());

		//    b) With clustering
		System.out.println("Warm up phase ended");
		System.out.println("");
		System.out.println("*** Poi calculation: FastScan **");
		long start = System.currentTimeMillis();
		final ColumnVector importancesWithClustering = getAccountImportances(
				endBlockHeight,
				acctMap.values().stream()
						.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem())
						.collect(Collectors.toList()),
				new FastScanClusteringStrategy());
		long stop = System.currentTimeMillis();
		System.out.println("Calculating importances needed " + (stop - start) + "ms.");
		System.out.println("");

		//    c) Without real clustering: every account is an outlier.
		System.out.println("*** Poi calculation: OutlierScan **");
		start = System.currentTimeMillis();
		final ColumnVector importancesWithOutlierClustering = getAccountImportances(
				endBlockHeight,
				acctMap.values().stream()
						.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem())
						.collect(Collectors.toList()),
				new OutlierScan());
		stop = System.currentTimeMillis();
		System.out.println("Calculating importances needed " + (stop - start) + "ms.");
		System.out.println("");

		//    d) Without real clustering: every account is in the same cluster.
		System.out.println("*** Poi calculation: UniqueClusterScan **");
		start = System.currentTimeMillis();
		final ColumnVector importancesWithoutClustering = getAccountImportances(
				endBlockHeight,
				acctMap.values().stream()
						.filter(acct -> acct.getWeightedBalances().getVested(endBlockHeight).getNumNem() > PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumNem())
						.collect(Collectors.toList()),
				new SingleClusterScan());
		stop = System.currentTimeMillis();
		System.out.println("Calculating importances needed " + (stop - start) + "ms.");

		Assert.assertThat(importancesWithoutClustering.size(), IsEqual.equalTo(importancesWithClustering.size()));
		final ColumnVector ratios = new ColumnVector(importancesWithoutClustering.size());
		double diff = 0;
		for (int i = 0; i < importancesWithClustering.size(); ++i) {
			diff += Math.abs(importancesWithClustering.getAt(i) - importancesWithoutClustering.getAt(i));
			if (importancesWithoutClustering.getAt(i) > 0.0) {
				ratios.setAt(i, importancesWithClustering.getAt(i) / importancesWithoutClustering.getAt(i));
			} else if (importancesWithClustering.getAt(i) > 0.0) {
				ratios.setAt(i, Double.MAX_VALUE);
			} else {
				ratios.setAt(i, 1.0);
			}
			if (ratios.getAt(i) > 1.001 || ratios.getAt(i) < 0.999) {
				System.out.println("Account " + i + " importance ratio is " + ratios.getAt(i));
			}
		}
		System.out.println("ratios: " + ratios);
		System.out.println("diff: " + diff);
	}

	@Test
	public void canQueryNxtTransactionTable() throws SQLException {
		// Arrange:
		conn = openNxtDatabase(getDbUrl());
		stmt = conn.createStatement();

		// Act:
		final String sql = "SELECT height, sender_id, recipient_id, amount FROM TRANSACTION WHERE height = 0 ORDER BY height ASC";
		final ResultSet rs = stmt.executeQuery(sql);
		int size = 0;
		System.out.println("Height | Sender | Recipient | Amount");
		while (rs.next()) {
			System.out.println(String.format("%d | %s | %s | %d",
					rs.getLong("HEIGHT"),
					rs.getLong("SENDER_ID"),
					rs.getLong("RECIPIENT_ID"),
					rs.getLong("AMOUNT")));
			size++;
		}
		// Assert
		Assert.assertThat(rs, IsNull.notNullValue());
		Assert.assertThat(size, IsEqual.equalTo(73));

		// Cleanup
		Assert.assertThat(closeH2Database(), IsEqual.equalTo(true));
	}

	@Test
	public void nxtGraphViewTest() throws SQLException, IOException {
		final int startHeight = 0;
		final int stopHeight = 10000;

		final List<NxtTransaction> transactionData = getTransactionData(startHeight, stopHeight);
		final SparseMatrix outlinkMatrix = createNetOutlinkMatrix(transactionData);
		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		//params.set("layout", Integer.toString(PoiGraphViewer.SPRING_LAYOUT));
		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
		viewer.saveGraph();
		//viewer.showGraph();
	}

	@Test
	public void nxtGraphClusteringTest() throws SQLException {
		final int startHeight = 100000;
		final int stopHeight = 200000;//300000;

		final long start = System.currentTimeMillis();
		final List<NxtTransaction> transactionData = getTransactionData(startHeight, stopHeight);
		final long stop = System.currentTimeMillis();
		System.out.println("Loading transaction data (" + transactionData.size() + " transactions) needed " + (stop - start) + "ms.");
		final SparseMatrix outlinkMatrix = createNetOutlinkMatrix(transactionData);
		outlinkMatrix.removeNegatives();
		outlinkMatrix.normalizeColumns();
		final List<GraphClusteringStrategy> clusterers = getClusterers();
		for (final GraphClusteringStrategy clusterer : clusterers) {
			final ClusteringResult result = calculateClusteringResult(clusterer, outlinkMatrix);
			System.out.println("The clusterer found " + result.getClusters().size() + " regular clusters, " + result.getHubs().size() + " hubs and " +
					result.getOutliers().size() + " outliers.");
			//logClusteringResult(result);
		}
	}

	private SparseMatrix createNetOutlinkMatrix(final List<NxtTransaction> transactionData) {
		final Map<Long, Integer> idToIndexMap = new HashMap<>();
		final int[] index = new int[1];
		transactionData.stream().forEach(data -> {
			if (!idToIndexMap.containsKey(data.senderId)) {
				idToIndexMap.putIfAbsent(data.senderId, index[0]++);
			}
			if (!idToIndexMap.containsKey(data.recipientId)) {
				idToIndexMap.putIfAbsent(data.recipientId, index[0]++);
			}
		});
		final SparseMatrix outlinkMatrix = new SparseMatrix(idToIndexMap.size(), idToIndexMap.size(), 8);
		transactionData.stream().forEach(data -> {
			final int row = idToIndexMap.get(data.recipientId);
			final int col = idToIndexMap.get(data.senderId);
			outlinkMatrix.incrementAt(row, col, data.amount);
			outlinkMatrix.incrementAt(col, row, -data.amount);
		});

		return outlinkMatrix;
	}

	private String getDbUrl() {
		return String.format("jdbc:h2:%s/nem/nxt_db/nxt", System.getProperty("user.home"));
	}

	private List<PoiAccountState> getGenesisAccounts() throws SQLException {
		final List<PoiAccountState> genesisAccts = new ArrayList<>();

		conn = openNxtDatabase(getDbUrl());
		stmt = conn.createStatement();

		// Act:
		final String sql = "SELECT height, sender_id, recipient_id, amount FROM TRANSACTION WHERE height = 0 ORDER BY height ASC";
		final ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getLong("sender_id") + "");
			genesisAccts.add(createAccountWithBalance(Address.fromEncoded(rs.getLong("recipient_id") + ""), 1, rs.getLong("AMOUNT")));
		}

		return genesisAccts;
	}

	private List<NxtTransaction> getTransactionData(final int startHeight, final int stopHeight) throws SQLException {
		final List<NxtTransaction> transactionData = new ArrayList<>(stopHeight - startHeight);
		conn = openNxtDatabase(getDbUrl());
		stmt = conn.createStatement();
		final String sql = String.format(
				"SELECT height, sender_id, recipient_id, amount FROM TRANSACTION WHERE height >= %d AND height <= %d ORDER BY height ASC",
				startHeight,
				stopHeight);
		final ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			transactionData.add(new NxtTransaction(
					rs.getLong("HEIGHT"),
					rs.getLong("SENDER_ID"),
					rs.getLong("RECIPIENT_ID"),
					rs.getLong("AMOUNT")));
		}
		closeH2Database();

		return transactionData;
	}

	private List<NxtTransaction> createAccountsFromTransactions(final int startHeight, final int stopHeight) throws SQLException {
		final List<NxtTransaction> transactionData = new ArrayList<>(stopHeight - startHeight);
		openNxtDatabase(getDbUrl());
		stmt = conn.createStatement();
		final String sql = String.format(
				"SELECT height, sender_id, recipient_id, amount FROM TRANSACTION WHERE height >= %d AND height < %d ORDER BY height ASC",
				startHeight,
				stopHeight);
		final ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			if (rs.getLong("SENDER_ID") == 0 || rs.getLong("RECIPIENT_ID") == 0 || rs.getLong("AMOUNT") == 0) {
				continue;
			}
			transactionData.add(new NxtTransaction(
					rs.getLong("HEIGHT"),
					rs.getLong("SENDER_ID"),
					rs.getLong("RECIPIENT_ID"),
					rs.getLong("AMOUNT")));
		}
		closeH2Database();

		return transactionData;
	}

	private Connection openNxtDatabase(final String path) {
		if (conn != null) {
			return conn;
		}
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(path, "sa", "sa");
		} catch (final Exception e) {
			conn = null;
			e.printStackTrace();
		}

		return conn;
	}

	private boolean closeH2Database() {
		boolean result = true;
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (final SQLException e) {
			conn = null;
			e.printStackTrace();
			result = false;
		}
		conn = null;
		stmt = null;

		return result;
	}

	private List<GraphClusteringStrategy> getClusterers() {
		final List<GraphClusteringStrategy> clusterers = new ArrayList<>();
		clusterers.add(new ScanClusteringStrategy());
		clusterers.add(new FastScanClusteringStrategy());

		return clusterers;
	}

	private HashMap<Long, Long> getStakes(final List<NxtTransaction> transactionData) {
		final HashMap<Long, Long> acctStakes = new HashMap<>();

		for (final NxtTransaction trans : transactionData) {
			acctStakes.put(trans.recipientId, acctStakes.getOrDefault(trans.recipientId, 0L) + trans.amount);
			acctStakes.put(trans.senderId, acctStakes.getOrDefault(trans.senderId, 0L) - trans.amount);
		}

		return acctStakes;
	}

	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final SparseMatrix outlinkMatrix) {
		final long start = System.currentTimeMillis();
		final NodeNeighborMap nodeNeighbordMap = new NodeNeighborMap(outlinkMatrix);
		final long stop = System.currentTimeMillis();
		System.out.println("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighbordMap);
		final Neighborhood neighborhood = new Neighborhood(nodeNeighbordMap, strategy);
		final ClusteringResult result = clusterer.cluster(neighborhood);
		return result;
	}

	private class NxtTransaction {
		final private long height;
		final private long senderId;
		final private long recipientId;
		final private long amount;

		private NxtTransaction(final long height, final long senderId, final long recipientId, final long amount) {
			this.height = height;
			this.senderId = senderId;
			this.recipientId = recipientId;
			this.amount = amount;
		}
	}

	private static PoiAccountState createAccountWithBalance(final Address address, final long numNEM) {
		return createAccountWithBalance(address, 1, numNEM);
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

		final PoiImportanceGenerator poi = new PoiAlphaImportanceGeneratorImpl();
		poi.updateAccountImportances(blockHeight, acctStates, new PoiScorer(), clusteringStrategy);

		final List<Double> importances = acctStates.stream()
				.map(a -> a.getImportanceInfo().getImportance(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		System.out.println("importances: " + importances);
		return importancesVector;
	}

	private void addOutlink(final PoiAccountState a, final PoiAccountState b, final BlockHeight blockHeight, final long amount) {
		//		System.out.println("sending:" + (amount/1000000));
		//		System.out.println("blockheight: " + blockHeight);
		//		System.out.println("a balance: " + ((a.getWeightedBalances().getUnvested(blockHeight).getNumMicroNem() + a.getWeightedBalances().getVested(blockHeight).getNumMicroNem())/1000000));

		final long aBalance = a.getWeightedBalances().getUnvested(blockHeight).getNumMicroNem() + a.getWeightedBalances().getVested(blockHeight).getNumMicroNem();
		if (aBalance < amount) {
			//            System.out.println("adding: " + (amount-aBalance)/1000000);
			a.getWeightedBalances().addReceive(blockHeight, Amount.fromMicroNem(amount - aBalance + 1));
		}

		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromMicroNem(amount), b.getAddress()));
		b.getWeightedBalances().addReceive(blockHeight, Amount.fromMicroNem(amount));
		//		System.out.println("before: " + (a.getWeightedBalances().getUnvested(blockHeight).getNumNem() + a.getWeightedBalances().getVested(blockHeight).getNumNem()));
		a.getWeightedBalances().addSend(blockHeight, Amount.fromMicroNem(amount));
		//		System.out.println("after: " + (a.getWeightedBalances().getUnvested(blockHeight).getNumNem() + a.getWeightedBalances().getVested(blockHeight).getNumNem()));
	}

	private void addVestedOutlink(final PoiAccountState a, final PoiAccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromMicroNem(amount), b.getAddress()));
		b.getWeightedBalances().addFullyVested(blockHeight, Amount.fromMicroNem(amount));
	}
}
