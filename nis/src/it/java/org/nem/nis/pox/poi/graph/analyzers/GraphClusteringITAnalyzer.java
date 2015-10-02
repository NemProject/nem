package org.nem.nis.pox.poi.graph.analyzers;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.nem.core.math.ColumnVector;
import org.nem.core.math.SparseMatrix;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.ExceptionUtils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.harvesting.CanHarvestPredicate;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.pox.poi.*;
import org.nem.nis.pox.poi.graph.*;
import org.nem.nis.pox.poi.graph.repository.CachedDatabaseRepository;
import org.nem.nis.pox.poi.graph.repository.DatabaseRepository;
import org.nem.nis.pox.poi.graph.repository.GraphClusteringTransaction;
import org.nem.nis.pox.poi.graph.utils.BlockChainAdapter;
import org.nem.nis.pox.poi.graph.utils.GraphAnalyzerTestUtils;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract class for testing POI. Specific implementations should load data
 * from various blockchains (e.g., Nem, Btc, Nxt).
 * <p>
 * Note that even though this class has JUnit Test methods, it isn't really a test class.
 * The tests don't have any asserts and are only used to analyze and print out
 * blockchain statistics.
 */
@Ignore
public abstract class GraphClusteringITAnalyzer {
	protected static final Logger LOGGER = Logger.getLogger(GraphClusteringITAnalyzer.class.getName());
	private static final long OUTLINK_HISTORY = NisTestConstants.ESTIMATED_BLOCKS_PER_MONTH;

	private static final PoiOptionsBuilder DEFAULT_POI_OPTIONS_BUILDER = new PoiOptionsBuilder();
	private static final PoiOptions DEFAULT_POI_OPTIONS = DEFAULT_POI_OPTIONS_BUILDER.create();
	private static final ImportanceScorer DEFAULT_IMPORTANCE_SCORER = new PoiScorer();
	private static final ImportanceScorer PAGE_RANK_SCORER = new PageRankScorer();

	private final int defaultEndHeight;
	private final String blockchainType;
	private final DatabaseRepository repository;
	private final BlockChainAdapter blockChainAdapter;

	protected GraphClusteringITAnalyzer(
			final DatabaseRepository repository,
			final BlockChainAdapter blockChainAdapter) {
		this.defaultEndHeight = blockChainAdapter.getDefaultEndHeight();
		this.blockchainType = blockChainAdapter.getBlockChainType();
		this.repository = new CachedDatabaseRepository(repository);
		this.blockChainAdapter = blockChainAdapter;
	}

	//region print (non-tests)

	@Test
	public void canPrintStakes() {
		// Act:
		final Collection<GraphClusteringTransaction> transactionData = this.loadTransactionData(0, this.defaultEndHeight);
		final HashMap<Long, Long> stakes = this.getStakes(transactionData);
		LOGGER.info(stakes.toString());
	}

	private HashMap<Long, Long> getStakes(final Collection<GraphClusteringTransaction> transactionData) {
		final HashMap<Long, Long> acctStakes = new HashMap<>();

		for (final GraphClusteringTransaction trans : transactionData) {
			final long recipientId = trans.getRecipientId();
			final long senderId = trans.getSenderId();
			final long amount = trans.getAmount();
			acctStakes.put(recipientId, acctStakes.getOrDefault(recipientId, 0L) + amount);
			acctStakes.put(senderId, acctStakes.getOrDefault(senderId, 0L) - amount);
		}

		return acctStakes;
	}

	@Test
	public void canWriteImportancesToFile() throws IOException {
		// Arrange:
		final int endHeight = this.defaultEndHeight;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);
		final Collection<AccountState> dbAccountStates = this.loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

		// Act:
		this.outputImportancesCsv(
				DEFAULT_POI_OPTIONS_BUILDER,
				copy(dbAccountStates),
				endBlockHeight);
	}

	@Test
	public void canWriteImportancesToFileForManyDifferentParameters() throws IOException {
		// compute Cartesian product of considered parameters
		final Set<Long> minHarvesterBalances = Sets.newSet(1l, 100l, 500l, 1000l, 10000l, 100000l);
		final Set<Long> minOutlinkWeights = Sets.newSet(0l, 1l, 100l, 1000l, 10000l);
		final Set<Double> negativeOutlinkWeights = Sets.newSet(0., 0.2, 0.4, 0.6, 0.8, 1.0);
		final Set<Double> outlierWeights = Sets.newSet(0.8, 0.85, 0.9, 0.95, 1.0);
		final Set<Integer> mus = Sets.newSet(1, 2, 3, 4, 5);
		final Set<Double> epsilons = Sets.newSet(0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95);
		final Set<TeleportationProbabilities> teleporationProbabilities = Sets.newSet(
				new TeleportationProbabilities(0.75, 0.1),
				new TeleportationProbabilities(0.65, 0.1),
				new TeleportationProbabilities(0.55, 0.1),
				new TeleportationProbabilities(0.75, 0.2),
				new TeleportationProbabilities(0.65, 0.2),
				new TeleportationProbabilities(0.55, 0.2));

		// load account states from the database
		final int endHeight = this.defaultEndHeight;
		final BlockHeight endBlockHeight = new BlockHeight(endHeight);
		final Collection<AccountState> dbAccountStates = this.loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

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
									this.outputImportancesCsv(
											optionsBuilder,
											copy(dbAccountStates),
											endBlockHeight);
								}
							}
						}
					}
				}
			}
		}
	}

	private void outputImportancesCsv(
			final PoiOptionsBuilder optionsBuilder,
			final Collection<AccountState> eligibleAccountStates,
			final BlockHeight endBlockHeight) throws IOException {
		final PoiOptions options = optionsBuilder.create();
		final String optionsDescription = String.format(
				"_%sminBalance_%sminOutlink_%snegOutlink_%soutlierWeight_%smu_%sepsilon_%stelPro_%sinterLevelProb",
				options.getMinHarvesterBalance().getNumNem(),
				options.getMinOutlinkWeight().getNumNem(),
				options.getNegativeOutlinkWeight(),
				options.getOutlierWeight(),
				options.getMuClusteringValue(),
				options.getEpsilonClusteringValue(),
				options.getTeleportationProbability(),
				options.getInterLevelTeleportationProbability());

		// 1. calc importances
		final ColumnVector importances = getAccountImportances(
				endBlockHeight,
				eligibleAccountStates,
				optionsBuilder,
				new PoiScorer());

		final List<Long> stakes = eligibleAccountStates.stream()
				.map(state -> mapPoiAccountStateToBalance(state, endBlockHeight).getNumMicroNem())
				.collect(Collectors.toList());

		final List<String> addresses = eligibleAccountStates.stream()
				.map(acct -> acct.getAddress().getEncoded())
				.collect(Collectors.toList());

		final List<Integer> outlinkCounts = eligibleAccountStates.stream()
				.map(acct -> acct.getImportanceInfo().getOutlinksSize(endBlockHeight))
				.collect(Collectors.toList());

		final List<Long> outlinkSums = eligibleAccountStates.stream()
				.map(acct -> {
					final BlockHeight startHeight = new BlockHeight(Math.max(1, endBlockHeight.getRaw() - OUTLINK_HISTORY));
					final ArrayList<Long> amounts = new ArrayList<>();
					acct.getImportanceInfo()
							.getOutlinksIterator(startHeight, endBlockHeight)
							.forEachRemaining(i -> amounts.add(i.getAmount().getNumMicroNem()));
					return amounts.stream().mapToLong(i -> i).sum();
				})
				.collect(Collectors.toList());

		String output = "'address', 'stake', 'importance', 'outlinkCount', 'outlinkSum'\n";
		for (int i = 0; i < importances.size(); ++i) {
			output += addresses.get(i) + "," + stakes.get(i) + "," + importances.getAt(i) + "," + outlinkCounts.get(i) + "," +
					outlinkSums.get(i) + "\n";
		}

		FileUtils.writeStringToFile(new File(String.format("kaiseki/" + this.blockchainType + "importances%s.csv", optionsDescription)), output);
	}

	//endregion

	//region sensitivity tests

	/**
	 * This test prints out a table of correlations between balances and NcdAwareRank scores, at
	 * various minimum harvesting balance settings.
	 */
	@Test
	public void minHarvestingBalancePageRankVariance() {
		// Act:
		this.runMinHarvestingBalanceVariance(PAGE_RANK_SCORER);
	}

	/**
	 * This test prints out a table of correlations between balances and importance scores, at
	 * various minimum harvesting balance settings.
	 */
	@Test
	public void minHarvestingBalanceImportanceVariance() {
		// Act:
		this.runMinHarvestingBalanceVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runMinHarvestingBalanceVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapValuesToImportanceVectors(
				Arrays.asList(1L, 100L, 1000L, 10000L, 100000L, 100000L),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setMinHarvesterBalance(Amount.fromNem(v));
					return optionsBuilder;
				},
				scorer);
		harness.renderAsList();
	}

	@Test
	public void outlierWeightImportanceVariance() {
		// Act:
		this.runOutlierWeightImportanceVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runOutlierWeightImportanceVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapValuesToImportanceVectors(
				Arrays.asList(100L, 80L, 60L, 40L, 20L, 1L),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setOutlierWeight(v / 100.0);
					return optionsBuilder;
				},
				scorer);
		harness.renderAsTable(
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				k -> FormatUtils.format(k / 100.0, 2));
	}

	@Test
	public void clusteringOptionsImportanceVariance() {
		// Act:
		this.runClusteringOptionsImportanceVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runClusteringOptionsImportanceVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapClusteringOptionsToImportanceVectors(
				Arrays.asList(new ClusteringOptions(3, 0.3),
						new ClusteringOptions(3, 0.4),
						new ClusteringOptions(3, 0.5),
						new ClusteringOptions(3, 0.6),
						new ClusteringOptions(3, 0.7),
						new ClusteringOptions(4, 0.3),
						new ClusteringOptions(4, 0.4),
						new ClusteringOptions(4, 0.5)),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setMuClusteringValue(v.mu);
					optionsBuilder.setEpsilonClusteringValue(v.epsilon);
					return optionsBuilder;
				},
				scorer);
		harness.renderAsTable(
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				k -> FormatUtils.format(k / 100.0, 2));
	}

	@Test
	public void teleportationProbabilityImportanceVariance() {
		// Act:
		this.runTeleportationProbabilityImportanceVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runTeleportationProbabilityImportanceVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapTeleportationProbabilitiesToImportanceVectors(
				Arrays.asList(new TeleportationProbabilities(0.75, 0.1),
						new TeleportationProbabilities(0.65, 0.1),
						new TeleportationProbabilities(0.55, 0.1),
						new TeleportationProbabilities(0.75, 0.2),
						new TeleportationProbabilities(0.65, 0.2),
						new TeleportationProbabilities(0.55, 0.2)),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setTeleportationProbability(v.teleporationProb);
					optionsBuilder.setInterLevelTeleportationProbability(v.interLevelTeleporationProb);
					return optionsBuilder;
				},
				scorer);
		harness.renderAsTable(
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				k -> FormatUtils.format(k / 100.0, 2));
	}

	/**
	 * This test prints out a table showing the correlation between account balance (stake) and
	 * importance scores, for various values of negative outlink weights.
	 * <br>
	 * Using correlation as a proxy for importance sensitivity to negOutlinkWeight.
	 */
	@Test
	public void negOutlinkWeightBalanceImportanceVariance() {
		// Act:
		this.runNegOutlinkWeightBalanceVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runNegOutlinkWeightBalanceVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapValuesToImportanceVectors(
				Arrays.asList(0L, 20L, 40L, 60L, 80L, 100L),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setNegativeOutlinkWeight(v / 100.0);
					return optionsBuilder;
				},
				scorer);
		harness.renderAsTable(
				DEFAULT_POI_OPTIONS.getMinHarvesterBalance(),
				k -> FormatUtils.format(k / 100.0, 2));
	}

	/**
	 * This test prints out a table of correlations between account balances (stake) and
	 * NcdAwareRank scores, for various minimum outlink weights (below the threshold, outlinks
	 * are not included in the outlink matrix). The tables also shows the correlations between the
	 * NcdAwareRank scores at various minimum outlink weights.
	 */
	@Test
	public void minOutlinkWeightPageRankVariance() {
		// Act:
		this.runMinOutlinkWeightVariance(PAGE_RANK_SCORER);
	}

	/**
	 * This test prints out a table of correlations between account balances (stake) and
	 * importance scores, for various minimum outlink weights (below the threshold, outlinks
	 * are not included in the outlink matrix). The tables also shows the correlations between the
	 * importances at various minimum outlink weights.
	 */
	@Test
	public void minOutlinkWeightImportanceVariance() {
		// Act:
		this.runMinOutlinkWeightVariance(DEFAULT_IMPORTANCE_SCORER);
	}

	private void runMinOutlinkWeightVariance(final ImportanceScorer scorer) {
		final SensitivityTestHarness harness = new SensitivityTestHarness();
		harness.mapValuesToImportanceVectors(
				Arrays.asList(1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L),
				v -> {
					final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
					optionsBuilder.setMinOutlinkWeight(Amount.fromNem(v));
					return optionsBuilder;
				},
				scorer);
		harness.renderAsTable(DEFAULT_POI_OPTIONS.getMinHarvesterBalance());
	}

	private class SensitivityTestHarness {
		private final int endHeight = GraphClusteringITAnalyzer.this.defaultEndHeight;
		private final BlockHeight endBlockHeight = new BlockHeight(this.endHeight);
		private final Map<Long, ColumnVector> parameterToImportanceMap = new HashMap<>();
		private final Collection<AccountState> dbAccountStates;

		public SensitivityTestHarness() {
			// load account states
			this.dbAccountStates = GraphClusteringITAnalyzer.this.loadEligibleHarvestingAccountStates(0, this.endHeight, Amount.ZERO);
		}

		public void mapValuesToImportanceVectors(
				final Collection<Long> values,
				final Function<Long, PoiOptionsBuilder> createOptionsBuilder,
				final ImportanceScorer scorer) {
			// calculate importances
			for (final Long value : values) {
				final PoiOptionsBuilder optionsBuilder = createOptionsBuilder.apply(value);
				final PoiOptions options = optionsBuilder.create();

				final Collection<AccountState> eligibleAccountStates = this.copyAndFilter(options.getMinHarvesterBalance());
				final ColumnVector importances = getAccountImportances(this.endBlockHeight, eligibleAccountStates, optionsBuilder, scorer);
				this.parameterToImportanceMap.put(value, importances);
			}
		}

		public void mapClusteringOptionsToImportanceVectors(
				final Collection<ClusteringOptions> values,
				final Function<ClusteringOptions, PoiOptionsBuilder> createOptionsBuilder,
				final ImportanceScorer scorer) {
			// calculate importances
			for (final ClusteringOptions value : values) {
				final PoiOptionsBuilder optionsBuilder = createOptionsBuilder.apply(value);
				final PoiOptions options = optionsBuilder.create();

				final Collection<AccountState> eligibleAccountStates = this.copyAndFilter(options.getMinHarvesterBalance());
				final ColumnVector importances = getAccountImportances(this.endBlockHeight, eligibleAccountStates, optionsBuilder, scorer);
				this.parameterToImportanceMap.put(Long.parseLong("" + value.mu + ((int)(value.epsilon * 100))), importances);
			}
		}

		public void mapTeleportationProbabilitiesToImportanceVectors(
				final Collection<TeleportationProbabilities> values,
				final Function<TeleportationProbabilities, PoiOptionsBuilder> createOptionsBuilder,
				final ImportanceScorer scorer) {
			// calculate importances
			for (final TeleportationProbabilities value : values) {
				final PoiOptionsBuilder optionsBuilder = createOptionsBuilder.apply(value);
				final PoiOptions options = optionsBuilder.create();

				final Collection<AccountState> eligibleAccountStates = this.copyAndFilter(options.getMinHarvesterBalance());
				final ColumnVector importances = getAccountImportances(this.endBlockHeight, eligibleAccountStates, optionsBuilder, scorer);
				this.parameterToImportanceMap.put(
						Long.parseLong("" + ((int)(value.teleporationProb * 100)) + ((int)(value.interLevelTeleporationProb * 100))),
						importances);
			}
		}

		private Collection<AccountState> copyAndFilter(final Amount minHarvesterBalance) {
			return this.dbAccountStates.stream()
					.map(AccountState::copy)
					.filter(accountState -> {
						final Amount balance = mapPoiAccountStateToBalance(accountState, this.endBlockHeight);
						return balance.compareTo(minHarvesterBalance) >= 0;
					})
					.collect(Collectors.toList());
		}

		private void renderAsList() {
			final List<Long> keys = this.parameterToImportanceMap.keySet().stream().sorted().collect(Collectors.toList());
			final List<String> keyNames = keys.stream()
					.map(GraphClusteringITAnalyzer::getFriendlyLabel)
					.collect(Collectors.toList());

			final StringBuilder builder = new StringBuilder();
			final DecimalFormat decimalFormat = FormatUtils.getDecimalFormat(4);

			for (int i = 0; i < keyNames.size(); ++i) {
				builder.append(System.lineSeparator());
				builder.append(String.format("* %s |", keyNames.get(i)));

				final Collection<AccountState> eligibleAccountStates = this.copyAndFilter(Amount.fromNem(keys.get(i)));
				final ColumnVector balances = getBalances(this.endBlockHeight, eligibleAccountStates);
				final ColumnVector importances = this.parameterToImportanceMap.get(keys.get(i));
				final double correlation = balances.correlation(importances);
				builder.append(String.format(" %s |", decimalFormat.format(correlation)));
				builder.append(String.format(" %6d |", balances.size()));
			}

			LOGGER.info(builder.toString());
		}

		private void renderAsTable(final Amount minHarvesterBalance) {
			this.renderAsTable(minHarvesterBalance, GraphClusteringITAnalyzer::getFriendlyLabel);
		}

		private void renderAsTable(final Amount minHarvesterBalance, final Function<Long, String> keyToKeyName) {
			final Collection<AccountState> eligibleAccountStates = this.copyAndFilter(minHarvesterBalance);
			final ColumnVector balances = getBalances(this.endBlockHeight, eligibleAccountStates);
			this.parameterToImportanceMap.put(0L, balances);

			final List<Long> keys = this.parameterToImportanceMap.keySet().stream().sorted().collect(Collectors.toList());
			final List<String> keyNames = keys.stream()
					.map(keyToKeyName::apply)
					.collect(Collectors.toList());

			final StringBuilder builder = new StringBuilder();
			builder.append(System.lineSeparator());
			builder.append("|      |");
			for (final String keyName : keyNames) {
				builder.append(String.format("  %s  |", keyName));
			}

			final DecimalFormat decimalFormat = FormatUtils.getDecimalFormat(4);
			for (int i = 0; i < keyNames.size(); ++i) {
				builder.append(System.lineSeparator());
				builder.append(String.format("| %s |", keyNames.get(i)));

				final ColumnVector vector1 = this.parameterToImportanceMap.get(keys.get(i));
				for (int j = 0; j < keyNames.size(); ++j) {
					if (j > i) {
						builder.append("        |");
						continue;
					}

					final ColumnVector vector2 = this.parameterToImportanceMap.get(keys.get(j));
					final double correlation = vector1.correlation(vector2);
					builder.append(String.format(" %s |", decimalFormat.format(correlation)));
				}
			}

			LOGGER.info(builder.toString());
		}
	}

	private static Collection<AccountState> copy(final Collection<AccountState> accountStates) {
		return accountStates.stream().map(AccountState::copy).collect(Collectors.toList());
	}

	private static String getFriendlyLabel(final long key) {
		return 0 == key ? "STK " : "10^" + (long)Math.log10(key);
	}

	private static ColumnVector getBalances(
			final BlockHeight blockHeight,
			final Collection<AccountState> accountStates) {
		final List<Amount> balances = accountStates.stream()
				.map(state -> mapPoiAccountStateToBalance(state, blockHeight))
				.collect(Collectors.toList());

		final ColumnVector balancesVector = new ColumnVector(balances.size());
		for (int i = 0; i < balances.size(); ++i) {
			balancesVector.setAt(i, balances.get(i).getNumNem());
		}

		LOGGER.info(String.format("balances: %s", balancesVector));
		return balancesVector;
	}

	protected static Amount mapPoiAccountStateToBalance(final AccountState accountState, final BlockHeight blockHeight) {
		return GraphAnalyzerTestUtils.mapPoiAccountStateToBalance(accountState, blockHeight);
	}

	//endregion

	//region influence of epsilon

	/**
	 * Analyzes the influence of the value of epsilon on the number of clusters, the average cluster size and the number of hubs.
	 * The minimum harvester balance is set to 10000.
	 * (unfortunately cluster information is only internally available, so it is only logged. You have to look for the entries yourself).
	 */
	@Test
	public void epsilonInfluenceOnNumberOfClustersAndClusterSize() {
		// Arrange:
		final BlockHeight endBlockHeight = new BlockHeight(this.defaultEndHeight);
		final PoiOptionsBuilder optionsBuilder = new PoiOptionsBuilder();
		optionsBuilder.setMinHarvesterBalance(Amount.fromNem(10000));
		optionsBuilder.setEpsilonClusteringValue(0.20);

		// Act:
		final Collection<AccountState> eligibleAccountStates = this.loadEligibleHarvestingAccountStates(0, this.defaultEndHeight, optionsBuilder);
		getAccountImportances(endBlockHeight, eligibleAccountStates, optionsBuilder, DEFAULT_IMPORTANCE_SCORER);
	}

	// endregion

	private Collection<AccountState> loadEligibleHarvestingAccountStates(
			final long startHeight,
			final long endHeight,
			final PoiOptionsBuilder optionsBuilder) {
		return this.loadEligibleHarvestingAccountStates(startHeight, endHeight, optionsBuilder.create().getMinHarvesterBalance());
	}

	private Collection<AccountState> loadEligibleHarvestingAccountStates(
			final long startHeight,
			final long endHeight,
			final Amount minHarvesterBalance) {
		final Collection<GraphClusteringTransaction> transactionData = this.loadTransactionData(startHeight, endHeight);
		final Map<Address, AccountState> accountStateMap = this.blockChainAdapter.createAccountStatesFromTransactionData(
				transactionData,
				this::normalizeAmount);
		return selectHarvestingEligibleAccounts(accountStateMap, new BlockHeight(endHeight), minHarvesterBalance);
	}

	private long normalizeAmount(final long amt) {
		// normalize in a way that min outlink weight represents the same amount in $
		// then the factor should be: (BTC market cap) / (NEM market cap) * (NEM supply / BTC supply).
		// More concrete: 1000 NEM are about $0.44 for 4M market cap and 9000M supply of NEM.
		// With 1 BTC = $250 (14M supply) we have $0.44 = 0.00177 BTC.
		// So the ratio is about 1000 / 0.00177 = 565000.
		final double nemMarketCap = 4_000_000.0;
		final double nemSupply = 9_000_000_000.0;
		return (long)(amt * (this.blockChainAdapter.getMarketCap() / nemMarketCap) * (nemSupply / this.blockChainAdapter.getSupplyUnits()));
	}

	private static Collection<AccountState> selectHarvestingEligibleAccounts(
			final Map<Address, AccountState> accountStateMap,
			final BlockHeight height,
			final Amount minHarvesterBalance) {
		final CanHarvestPredicate canHarvestPredicate = new CanHarvestPredicate(minHarvesterBalance);
		return accountStateMap.values().stream()
				.filter(accountState -> canHarvestPredicate.canHarvest(accountState, height))
				.collect(Collectors.toList());
	}

	//endregion

	//region graph (non-tests)

	@Test
	/**
	 * This is a useful function for viewing what the graphs look like. With many transactions, it becomes unmeaningful, however,
	 * so use with caution.
	 */
	public void graphViewTest() throws SQLException, IOException {
		final long startHeight = 0;
		final long stopHeight = this.defaultEndHeight;

		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setEpsilonClusteringValue(0.40);
		builder.setMinOutlinkWeight(Amount.fromNem(100));
		builder.setMinHarvesterBalance(Amount.fromNem(100));
		final Collection<AccountState> eligibleAccountStates = this.loadEligibleHarvestingAccountStates(startHeight, stopHeight, builder);
		final PoiContext poiContext = new PoiContext(eligibleAccountStates, new BlockHeight(stopHeight), builder.create());
		final SparseMatrix outlinkMatrix = poiContext.getOutlinkMatrix();
		final ClusteringResult result = poiContext.getClusteringResult();
		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params, result);
		viewer.saveGraph();
	}

	@Test
	public void graphClusteringTest() throws SQLException {
		final int startHeight = 0;
		final int stopHeight = this.defaultEndHeight;

		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		final SparseMatrix outlinkMatrix = this.createNetOutlinkMatrix(startHeight, stopHeight, builder);
		final ClusteringResult result = calculateClusteringResult(new FastScanClusteringStrategy(), outlinkMatrix);
		LOGGER.info(String.format("The clusterer found %d regular clusters, %d hubs, and %d outliers",
				result.getClusters().size(),
				result.getHubs().size(),
				result.getOutliers().size()));
	}

	private SparseMatrix createNetOutlinkMatrix(final long startHeight, final long endHeight, final PoiOptionsBuilder builder) {
		final Collection<AccountState> eligibleAccountStates = this.loadEligibleHarvestingAccountStates(startHeight, endHeight, builder);
		final PoiContext poiContext = new PoiContext(eligibleAccountStates, new BlockHeight(endHeight), builder.create());
		return poiContext.getOutlinkMatrix();
	}

	private static ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final SparseMatrix outlinkMatrix) {
		final long start = System.currentTimeMillis();
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final long stop = System.currentTimeMillis();
		LOGGER.info("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighborMap, strategy);
		return clusterer.cluster(neighborhood);
	}

	//endregion

	protected Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		return ExceptionUtils.propagate(() -> {
			// Act:
			return this.repository.loadTransactionData(startHeight, stopHeight);
		});
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<AccountState> acctStates,
			final PoiOptionsBuilder poiOptionsBuilder,
			final ImportanceScorer scorer) {
		final PoiOptions options = poiOptionsBuilder.create();
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(scorer, height -> options);
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

	/**
	 * Simple class to encapsulate mu and epsilon values.
	 */
	private class ClusteringOptions {
		final int mu;
		final double epsilon;

		ClusteringOptions(final int mu, final double epsilon) {
			this.mu = mu;
			this.epsilon = epsilon;
		}
	}

	/**
	 * Simple class to encapsulate teleportation and interlevel teleportation probabilities.
	 */
	private class TeleportationProbabilities {
		final double teleporationProb;
		final double interLevelTeleporationProb;

		TeleportationProbabilities(final double teleporationProb, final double interLevelTeleporationProb) {
			this.teleporationProb = teleporationProb;
			this.interLevelTeleporationProb = interLevelTeleporationProb;
		}
	}
}
