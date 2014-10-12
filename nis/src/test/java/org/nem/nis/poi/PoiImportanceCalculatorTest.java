package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.test.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PoiImportanceCalculatorTest {
	private static final Logger LOGGER = Logger.getLogger(PoiImportanceCalculatorTest.class.getName());
	private static final PoiOptions DEFAULT_OPTIONS = new PoiOptionsBuilder().create();

	@Test
	public void fastScanClusteringResultsInSameImportancesAsScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new ScanClusteringStrategy());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance < 0.00001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanOutlierScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new OutlierScan());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanSingleClusterScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new SingleClusterScan());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanNoClustering() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(null);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringCorrectlyIdentifiesMostImportantAccount() {
		// Act:
		final ColumnVector importances = calculateImportances(new FastScanClusteringStrategy());
		int maxIndex = 0;
		int nextMaxIndex = 0;
		for (int i = 0; i < importances.size(); ++i) {
			if (importances.getAt(i) > importances.getAt(maxIndex)) {
				nextMaxIndex = maxIndex;
				maxIndex = i;
			} else if (importances.getAt(i) > importances.getAt(nextMaxIndex)) {
				nextMaxIndex = i;
			}
		}

		// Assert: max index and next max index are 4 and 5
		Assert.assertThat(maxIndex + nextMaxIndex, IsEqual.equalTo(9));
		Assert.assertThat(Math.abs(maxIndex - nextMaxIndex), IsEqual.equalTo(1));
	}

	private static double calculateDistanceBetweenFastScanAndOtherImportances(final GraphClusteringStrategy clusteringStrategy) {
		// Act:
		final ColumnVector fastScanImportances = calculateImportances(new FastScanClusteringStrategy());
		final ColumnVector otherImportances = calculateImportances(clusteringStrategy);

		// Assert:
		final double distance = fastScanImportances.l2Distance(otherImportances);
		LOGGER.info(String.format("fastScanImportances - %s", fastScanImportances));
		LOGGER.info(String.format("otherImportances ---- %s", otherImportances));
		LOGGER.info(String.format("distance - %f", distance));
		return distance;
	}

	private static ColumnVector calculateImportances(final GraphClusteringStrategy clusteringStrategy) {
		final Collection<PoiAccountState> accountStates = createAccountStatesFromGraph(GraphType.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setClusteringStrategy(clusteringStrategy);
		if (null == clusteringStrategy) {
			final PoiOptions defaultOptions = poiOptionsBuilder.create();
			final double totalTeleportationProbability = defaultOptions.getTeleportationProbability() + defaultOptions.getInterLevelTeleportationProbability();
			poiOptionsBuilder.setTeleportationProbability(totalTeleportationProbability);
			poiOptionsBuilder.setInterLevelTeleportationProbability(0.00);
		}

		return calculateImportances(poiOptionsBuilder.create(), new BlockHeight(2), accountStates);
	}

	//endregion

	//region prototypical graphs

	/**
	 * <pre>
	 *     1 \       / 6
	 *     2 -\     /- 7
	 *     3 --o 0 o-- 8
	 *     4 -/     \- 9
	 *     5 /       \ 10
	 * </pre>
	 */
	@Test
	public void hubSinkSpokeGraphGivesHigherImportanceToHubThanSpokes() {
		// Arrange:
		// - account 0 starts with 2000 NEM
		// - accounts 1-10 start with 2100 NEM
		// - accounts 1-10 send 100 NEM to 0
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		for (int i = 1; i <= 10; ++i) {
			outlinkMatrix.setAt(0, i, 100);

			// initialize all hubs with 2100 NEM so that after the transfers all accounts will have the same balance
			accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100)));
		}

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are 2000 (the sent NEM have not vested)
		assertEqualBalances(height, accountStates, 2000);

		// - all spokes have the same importance
		final double hubImportance = importances.getAt(0);
		final double spokeImportance = importances.getAt(1);
		for (int i = 2; i < importances.size(); ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(spokeImportance));
		}

		// - the hub importance is greater than the spoke importance
		final double ratio = hubImportance / spokeImportance;
		LOGGER.info(String.format("hub: %f; spoke %f; ratio: %f", hubImportance, spokeImportance, ratio));
		Assert.assertThat(ratio > 1.20, IsEqual.equalTo(true));
		Assert.assertThat(ratio < 1.60, IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 *     1 o\       /o 6
	 *     2 o-\     /-o 7
	 *     3 o--- 0 ---o 8
	 *     4 o-/     \-o 9
	 *     5 o/       \o 10
	 * </pre>
	 */
	@Test
	public void hubSpokeSinkGraphGivesHigherImportanceHubThanToSpokes() {
		// Arrange:
		// - account 0 starts with 3000 NEM
		// - accounts 1-10 start with 2000 NEM
		// - account 0 sends 100 NEM to accounts 0-10
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(3000)));

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		for (int i = 1; i <= 10; ++i) {
			outlinkMatrix.setAt(i, 0, 100);

			// initialize all hubs with 2000 NEM so that after the transfers all accounts will have the same balance
			accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));
		}

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are 2000 (the sent NEM have not vested)
		assertEqualBalances(height, accountStates, 2000);

		// - all spokes have the same importance
		final double hubImportance = importances.getAt(0);
		final double spokeImportance = importances.getAt(1);
		for (int i = 2; i < importances.size(); ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(spokeImportance));
		}

		// - the hub importance is greater than the spoke importance
		final double ratio = hubImportance / spokeImportance;
		LOGGER.info(String.format("hub: %f; spoke %f; ratio: %f", hubImportance, spokeImportance, ratio));
		Assert.assertThat(ratio > 1.20, IsEqual.equalTo(true));
		Assert.assertThat(ratio < 1.60, IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 *     4----o5----o1 --o0o-- 6----o7----o8
	 *     o           |         o           |
	 *     |           o         |           o
	 *     3o----------2        10o----------9
	 * </pre>
	 */
	@Test
	public void fiveMemberRingConnectedThroughSpoke() {
		// Arrange:
		// - account 0 starts with 2000 NEM
		// - accounts 2-5 and 7-10 start with 2100 NEM
		// - accounts 1 and 6 start with 2200 NEM
		// - account 1-5 and 2-10 send around NEM in a loop
		// - account 1-6 send NEM to 0
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2200)));
		for (int i = 2; i <= 5; ++i) { accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100))); }
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2200)));
		for (int i = 7; i <= 10; ++i) { accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100))); }

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		outlinkMatrix.setAt(0, 1, 100);
		for (int i = 1; i <= 4; ++i) { outlinkMatrix.setAt(i + 1, i, 100); }
		outlinkMatrix.setAt(1, 5, 100);
		outlinkMatrix.setAt(0, 6, 100);
		for (int i = 6; i <= 9; ++i) { outlinkMatrix.setAt(i + 1, i, 100); }
		outlinkMatrix.setAt(6, 10, 100);

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);
		final ColumnVector balances = getBalances(height, accountStates);

		// Assert:
		// - all balances should be within a small range
		for (int i = 0; i < balances.size(); ++i) {
			final double balance = balances.getAt(i);
			Assert.assertThat(balance >= 2000 && balance < 2005, IsEqual.equalTo(true));
		}

		// - all importances should be within a small range
		for (int i = 0; i < importances.size(); ++i) {
			final double importance = importances.getAt(i);
			Assert.assertThat(importance > 0.085 && importance < 0.10, IsEqual.equalTo(true));
		}

		// - the ring importances should be the same
		for (int i = 1; i <= 5; ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(importances.getAt(i + 5)));
		}
	}

	/**
	 * a --o b   c
	 */
	@Test
	public void outlinkHasPositiveImpactOnImportance() {
		// Arrange:
		final List<PoiAccountState> accountStates = Arrays.asList(
				createAccountStateWithBalance(Amount.fromNem(101000)),
				createAccountStateWithBalance(Amount.fromNem(100000)),
				createAccountStateWithBalance(Amount.fromNem(100000)));

		// 0 sends part of balance to 1
		final BlockHeight height = new BlockHeight(2);
		addOutlink(accountStates.get(0), accountStates.get(1), height, Amount.fromNem(1000));

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are equal
		assertEqualBalances(height, accountStates, 100000);

		// - outlink > inlink > outlier
		Assert.assertThat(importances.getAt(1) > importances.getAt(0), IsEqual.equalTo(true));
		Assert.assertThat(importances.getAt(0) > importances.getAt(2), IsEqual.equalTo(true));
	}

	//endregion

	//region test helpers

	private static void addOutlink(
			final PoiAccountState senderAccountState,
			final PoiAccountState recipientAccountState,
			final BlockHeight blockHeight,
			final Amount amount) {
		senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
		senderAccountState.getImportanceInfo().addOutlink(
				new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

		recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
	}

	private static PoiAccountState createAccountStateWithBalance(final Amount balance) {
		final PoiAccountState accountState = new PoiAccountState(Utils.generateRandomAddress());
		accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
		return accountState;
	}

	private static Collection<PoiAccountState> createAccountStatesFromGraph(final GraphType graphType) {
		final Matrix outlinkMatrix = OutlinkMatrixFactory.create(graphType);
		return createAccountStatesFromGraph(outlinkMatrix);
	}

	private static Collection<PoiAccountState> createAccountStatesFromGraph(final Matrix outlinkMatrix) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final PoiAccountState accountState = new PoiAccountState(Utils.generateRandomAddress());
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, Amount.fromNem(1000000000));
			accountStates.add(accountState);
		}

		addOutlinksFromGraph(accountStates, new BlockHeight(2), outlinkMatrix);
		return accountStates;
	}

	private static void addOutlinksFromGraph(
			final List<PoiAccountState> accountStates,
			final BlockHeight blockHeight,
			final Matrix outlinkMatrix) {
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = outlinkMatrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				final Amount amount = Amount.fromNem(element.getValue().longValue());
				if (amount.compareTo(Amount.ZERO) > 0) {
					final PoiAccountState senderAccountState = accountStates.get(element.getColumn());
					final PoiAccountState recipientAccountState = accountStates.get(element.getRow());
					addOutlink(senderAccountState, recipientAccountState, blockHeight, amount);
				}
			}
		}
	}

	private static ColumnVector calculateImportances(
			final PoiOptions options,
			final BlockHeight importanceBlockHeight,
			final Collection<PoiAccountState> accountStates) {
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(
				new PoiScorer(),
				options);
		importanceCalculator.recalculate(importanceBlockHeight, accountStates);
		return getImportances(importanceBlockHeight, accountStates);
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

	private static void assertEqualBalances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates,
			final double amount) {
		// Act:
		final ColumnVector balances = getBalances(blockHeight, accountStates);

		// Assert:
		for (int i = 0; i < balances.size(); ++i) {
			Assert.assertThat(balances.getAt(i), IsEqual.equalTo(amount));
		}
	}

	private static ColumnVector getImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		final List<Double> importances = accountStates.stream()
			.map(a -> a.getImportanceInfo().getImportance(blockHeight))
			.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		LOGGER.info(String.format("importances: %s", importancesVector));
		return importancesVector;

	}

	//endregion
}