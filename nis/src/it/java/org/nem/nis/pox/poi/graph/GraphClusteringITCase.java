package org.nem.nis.pox.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.harvesting.CanHarvestPredicate;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.pox.poi.*;
import org.nem.nis.pox.poi.graph.repository.CachedDatabaseRepository;
import org.nem.nis.pox.poi.graph.repository.DatabaseRepository;
import org.nem.nis.pox.poi.graph.repository.GraphClusteringTransaction;
import org.nem.nis.pox.poi.graph.utils.BlockChainAdapter;
import org.nem.nis.state.AccountState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
public abstract class GraphClusteringITCase {
	protected static final Logger LOGGER = Logger.getLogger(GraphClusteringITCase.class.getName());

	private static final PoiOptionsBuilder DEFAULT_POI_OPTIONS_BUILDER = new PoiOptionsBuilder();
	private static final ImportanceScorer DEFAULT_IMPORTANCE_SCORER = new PoiScorer();

	private final int defaultEndHeight;
	private final DatabaseRepository repository;
	private final BlockChainAdapter blockChainAdapter;

	protected GraphClusteringITCase(final DatabaseRepository repository, final BlockChainAdapter blockChainAdapter) {
		this.defaultEndHeight = blockChainAdapter.getDefaultEndHeight();
		this.repository = new CachedDatabaseRepository(repository);
		this.blockChainAdapter = blockChainAdapter;
	}

	//region poiComparisonTest

	@Test
	@Ignore // disabled because it requires the full blockchain
	public void poiComparisonTest() {
		// Arrange:
		final int endHeight = this.defaultEndHeight;

		// a) This is the warm up phase.
		this.getAccountImportances(endHeight, new OutlierScan(), "WARM UP");

		// Act:
		// b) these are actual importances
		final ColumnVector fastScanImportances = this.getAccountImportances(endHeight, new FastScanClusteringStrategy(), "FastScan");
		final ColumnVector outlierScanImportances = this.getAccountImportances(endHeight, new OutlierScan(), "OutlierScan");
		final ColumnVector singleClusterScanImportances = this.getAccountImportances(endHeight, new SingleClusterScan(), "SingleClusterScan");

		// Assert:
		assertDifference("FastScan vs SingleClusterScan", fastScanImportances, singleClusterScanImportances);
		assertDifference("FastScan vs OutlierScan", fastScanImportances, outlierScanImportances);
		// Note: it is not reasonable to expect SingleClusterScan and OutlierScan to be different;
		// it is not interesting if they are similar or different
	}

	private static void assertDifference(final String message, final ColumnVector lhs, final ColumnVector rhs) {
		// Assert:
		LOGGER.info(message);
		final double difference = calculateDifference(lhs, rhs);
		Assert.assertTrue(message, difference > 0.01);
	}

	private ColumnVector getAccountImportances(
			final long endHeight,
			final GraphClusteringStrategy clusteringStrategy,
			final String name) {
		// 0. Load transactions.
		final Collection<AccountState> eligibleAccountStates = this.loadEligibleHarvestingAccountStates(0, endHeight, DEFAULT_POI_OPTIONS_BUILDER);

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

			maxRatio = ratios.getAt(i) > maxRatio ? ratios.getAt(i) : maxRatio;
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

	protected Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		return ExceptionUtils.propagate(() -> {
			// Act:
			return this.repository.loadTransactionData(startHeight, stopHeight);
		});
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<AccountState> acctStates,
			final GraphClusteringStrategy clusteringStrategy) {
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setClusteringStrategy(clusteringStrategy);
		return getAccountImportances(blockHeight, acctStates, poiOptionsBuilder, DEFAULT_IMPORTANCE_SCORER);
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
}
