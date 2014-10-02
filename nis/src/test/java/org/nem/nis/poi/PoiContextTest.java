package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PoiContextTest {

	//region construction

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateContextAroundZeroAccounts() {
		// Arrange:
		final List<TestAccountInfo> accountInfos = Arrays.asList();

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		createPoiContext(accountStates, height);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateContextAroundZeroForagingEligibleAccounts() {
		// Arrange:
		final long multiplier = 1000 * Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(multiplier - 1, null), // non-foraging account
				new TestAccountInfo(multiplier - 1, null)); // non-foraging account

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		createPoiContext(accountStates, height);
	}

	//endregion

	//region process

	@Test
	public void vestedBalanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) vested balances are not normalized
		Assert.assertThat(
				context.getVestedBalanceVector(),
				IsEqual.equalTo(new ColumnVector(2999999999L, 5000000000L, 1000000000L, 1000000000L)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) calculation delegates to PoiAccountInfo
		Assert.assertThat(
				context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(0, 3e06, 0, 10e06)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectlyWhenThereAreBidirectionalFlows() {
		// Act:
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) both foraging-eligible and non-foraging-eligible accounts are represented
		// (2) calculation delegates to PoiAccountInfo
		// (3) net outflows are used instead of total outflows
		Assert.assertThat(
				context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(9e06, 0, -3e06, 8e06)));
	}

	@Test
	public void poiStartVectorIsInitializedToNormalizedUniformVectorForFirstIteration() {
		// Act:
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) start vector is uniform
		// (2) start vector is normalized
		Assert.assertThat(
				context.getPoiStartVector(),
				IsEqual.equalTo(new ColumnVector(0.25, 0.25, 0.25, 0.25)));
	}

	@Test
	public void poiStartVectorIsDerivedFromPreviousPageRankForSubsequentIterations() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		accountStates.get(0).getImportanceInfo().setLastPageRank(3);
		accountStates.get(1).getImportanceInfo().setLastPageRank(7);
		accountStates.get(2).getImportanceInfo().setLastPageRank(4);
		accountStates.get(4).getImportanceInfo().setLastPageRank(2);

		// Act:
		final PoiContext context = createPoiContext(accountStates, height);

		// Assert:
		// (1) start vector is derived from previous page rank
		// (2) start vector is normalized
		//TODO: this test should currently fail because we took out this feature for now
		//		Assert.assertThat(
		//				context.getPoiStartVector(),
		//				IsEqual.equalTo(new ColumnVector(3.0 / 9, 4.0 / 9, 0.0, 2.0 / 9)));
		// TODO-CR [08062014][J-M]: should we remove this test
		// TODO-CR [20140806][M-J][M-BR]: not sure. can we find a way to add this back?
	}

	//region teleportation probabilities

	@Test
	public void teleportationProbabilityIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) a value of 0.750
		Assert.assertThat(context.getTeleportationProbability(), IsEqual.equalTo(0.750));
	}

	@Test
	public void interLevelTeleportationProbabilityIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) a value of 0.100
		Assert.assertThat(context.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.100));
	}

	@Test
	public void inverseTeleportationVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) (1 - 0.75 - 0.10 = 0.15) / N (4.0)
		final double expectedProb = 0.15 / 4.0;
		Assert.assertThat(roundTo(context.getInverseTeleportationProbability(), 5), IsEqual.equalTo(expectedProb));
	}

	//endregion

	@Test
	public void dangleIndexesAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) accounts without out-links are dangling
		Assert.assertThat(
				context.getDangleIndexes(),
				IsEquivalent.equivalentTo(new Integer[] { 0, 2 }));
	}

	@Test
	public void dangleVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContext();

		// Assert:
		// (1) dangle vector is the 1-vector
		Assert.assertThat(
				context.getDangleVector(),
				IsEqual.equalTo(new ColumnVector(1, 1, 1, 1)));
	}

	@Test
	public void outlinkMatrixIsInitializedCorrectly() {
		// Act:
		// (0, 1, 8), (0, 2, 4)
		// (1, 0, 2), (1, 2, 6)
		// (3, 0, 3), (3, 2, 5)
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) account link weights are normalized
		final Matrix expectedAccountLinks = new DenseMatrix(4, 4);
		expectedAccountLinks.setAt(0, 3, 0.375);
		expectedAccountLinks.setAt(1, 0, 0.6);
		expectedAccountLinks.setAt(2, 0, 0.4);
		expectedAccountLinks.setAt(2, 1, 1.0);
		expectedAccountLinks.setAt(2, 3, 0.625);

		Assert.assertThat(
				context.getOutlinkMatrix().roundTo(5),
				IsEqual.equalTo(expectedAccountLinks));
	}

	/**
	 * @formatter: off
	 * Graph:         0
	 *               / \
	 *              /   \
	 *             o     o
	 *             1----o2----o7
	 *                   |
	 *                   o
	 *                   3
	 *                   |
	 *                   o
	 *                   4
	 *                  / \
	 *                 o   o
	 *                5----o6
	 * @formatter: on
	 *
	 * Expected: clusters {0,1,2} and {4,5,6}, one hub {3}, one outlier {7}
	 */
	@Test
	public void clustersAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithTwoClustersOneHubAndOneOutlier();

		// Assert:
		final List<Cluster> expectedClusters = Arrays.asList(
				new Cluster(new ClusterId(0), Arrays.asList(NisUtils.toNodeIdArray(0, 1, 2))),
				new Cluster(new ClusterId(4), Arrays.asList(NisUtils.toNodeIdArray(4, 5, 6))));
		final List<Cluster> expectedHubs = Arrays.asList(
				new Cluster(new ClusterId(3), Arrays.asList(NisUtils.toNodeIdArray(3))));
		final List<Cluster> expectedOutliers = Arrays.asList(
				new Cluster(new ClusterId(7), Arrays.asList(NisUtils.toNodeIdArray(7))));

		Assert.assertThat(context.getClusteringResult().getClusters(), IsEquivalent.equivalentTo(expectedClusters));
		Assert.assertThat(context.getClusteringResult().getHubs(), IsEquivalent.equivalentTo(expectedHubs));
		Assert.assertThat(context.getClusteringResult().getOutliers(), IsEquivalent.equivalentTo(expectedOutliers));
	}

	// TODO-CR [08062014][J-M]: nice tests!
	// TODO-CR [08062014][J-M]: can you explain what the getInterLevelMatrix is?
	// TODO-CR [20140806][M-J]: Interlevel matrix is the inter-level proximity matrix M (decomposed into R and A matrices). This is the new term added to PageRank to create NCDawareRank, because this represents connections between blocks in the graph.

	@Test
	public void interLevelProximityMatrixIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithTwoClustersOneHubAndOneOutlier();

		// Assert:
		final InterLevelProximityMatrix interLevel = context.getInterLevelMatrix();
		final SparseMatrix a = new SparseMatrix(8, 4, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 2, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 3, 1.0);
		final SparseMatrix r = new SparseMatrix(4, 8, 8);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 9.0);
		r.setAt(1, 3, 1.0 / 6.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);
		r.setAt(1, 6, 1.0 / 3.0);
		r.setAt(2, 2, 1.0 / 3.0);
		r.setAt(2, 3, 1.0 / 2.0);
		r.setAt(3, 2, 1.0 / 3.0);
		r.setAt(3, 7, 1.0 / 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	//endregion

	//region updateImportances

	@Test
	public void canUpdateFilteredAccountsWithCompatiblePageRankVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(5, 2, 7, 3), new ColumnVector(4));

		// Assert:
		final List<Double> importances = accountStates.stream()
				.map(a -> a.getImportanceInfo().getLastPageRank())
				.collect(Collectors.toList());
		Assert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
	}

	@Test
	public void canUpdateFilteredAccountsWithCompatibleImportanceVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 7, 3));

		// Assert:
		final List<Double> importances = accountStates.stream()
				.map(a -> {
					final AccountImportance ai = a.getImportanceInfo();
					return ai.isSet() ? ai.getImportance(height) : 0.0;
				})
				.collect(Collectors.toList());
		Assert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
	}

	@Test
	public void cannotUpdateFilteredAccountsWithIncompatiblePageRankVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> context.updateImportances(new ColumnVector(5, 2, 7, 3, 4, 8), new ColumnVector(4)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> context.updateImportances(new ColumnVector(5, 2, 3), new ColumnVector(4)),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotUpdateFilteredAccountsWithIncompatibleImportanceVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 7, 3, 4, 8)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 3)),
				IllegalArgumentException.class);
	}

	//endregion

	private static void addAccountLink(
			final BlockHeight height,
			final PoiAccountState sender,
			final PoiAccountState recipient,
			final int amount) {

		final AccountLink link = new AccountLink(height, Amount.fromNem(amount), recipient.getAddress());
		sender.getImportanceInfo().addOutlink(link);
	}

	private static List<PoiAccountState> createTestPoiAccountStates(
			final List<TestAccountInfo> accountInfos,
			final BlockHeight height) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (final TestAccountInfo info : accountInfos) {
			final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
			state.getWeightedBalances().addFullyVested(height, Amount.fromMicroNem(info.vestedBalance));

			for (final int amount : info.amounts) {
				final AccountLink link = new AccountLink(height, Amount.fromNem(amount), Utils.generateRandomAddress());
				state.getImportanceInfo().addOutlink(link);
			}

			accountStates.add(state);
		}

		return accountStates;
	}

	private static List<PoiAccountState> createTestPoiAccountStates(final BlockHeight height) {
		final long multiplier = PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumMicroNem();
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * multiplier - 1, null),
				new TestAccountInfo(multiplier - 1, new int[] { 1 }), // 1 (insufficient balance)
				new TestAccountInfo(5 * multiplier, new int[] { 1, 2 }), // 3
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, new int[] { 1, 1, 4, 3, 1 }), // 10
				new TestAccountInfo(multiplier - 1, new int[] { 7 })); // 7 (insufficient vested balance)

		return createTestPoiAccountStates(accountInfos, height);
	}

	private static PoiContext createPoiContext() {
		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(height);
		return createPoiContext(accountStates, height);
	}

	private static PoiContext createPoiContext(final List<PoiAccountState> accountStates, final BlockHeight height) {
		return new PoiContext(accountStates, height, new FastScanClusteringStrategy());
	}

	private static PoiContext createTestPoiContextWithAccountLinks() {
		// Arrange: create 4 accounts
		final long multiplier = PoiAccountInfo.MIN_HARVESTING_BALANCE.getNumMicroNem();// 1000 is min harvesting balance
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier - 1, null)); // non-foraging account

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// set up account links
		addAccountLink(height, accountStates.get(0), accountStates.get(1), 8);
		addAccountLink(height, accountStates.get(0), accountStates.get(2), 4);
		addAccountLink(height, accountStates.get(1), accountStates.get(0), 2);
		addAccountLink(height, accountStates.get(1), accountStates.get(2), 6);
		addAccountLink(height, accountStates.get(3), accountStates.get(0), 3);
		addAccountLink(height, accountStates.get(3), accountStates.get(2), 5);
		addAccountLink(height, accountStates.get(4), accountStates.get(2), 5); // from non-foraging account (ignored)
		addAccountLink(height, accountStates.get(0), accountStates.get(4), 2); // to non-foraging account (included in scores)

		// Act:
		return createPoiContext(accountStates, height);
	}

	/**
	 * @formatter: off
	 * Graph:         0
	 *               / \
	 *              /   \
	 *             1-----2-----7
	 *                   |
	 *                   |
	 *                   3
	 *                   |
	 *                   |
	 *                   4
	 *                  / \
	 *                 /   \
	 *                5-----6
	 * @formatter: on
	 *
	 * Expected: clusters {0,1,2} and {4,5,6}, one hub {3}, one outlier {7}
	 */
	private static PoiContext createTestPoiContextWithTwoClustersOneHubAndOneOutlier() {
		// Arrange: create 8 accounts
		final long multiplier = 1000 * Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, null));

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// set up account links
		addAccountLink(height, accountStates.get(0), accountStates.get(1), 1);
		addAccountLink(height, accountStates.get(0), accountStates.get(2), 1);
		addAccountLink(height, accountStates.get(1), accountStates.get(2), 1);
		addAccountLink(height, accountStates.get(2), accountStates.get(7), 1);
		addAccountLink(height, accountStates.get(2), accountStates.get(3), 1);
		addAccountLink(height, accountStates.get(3), accountStates.get(4), 1);
		addAccountLink(height, accountStates.get(4), accountStates.get(5), 1);
		addAccountLink(height, accountStates.get(4), accountStates.get(6), 1);

		// Act:
		return createPoiContext(accountStates, height);
	}

	public double roundTo(final double value, final int numPlaces) {
		final double multipler = Math.pow(10, numPlaces);
		return Math.round(value * multipler) / multipler;
	}


	private static PoiContext createPoiContext(
			final Iterable<PoiAccountState> accountStates,
			final BlockHeight height) {
		return new PoiContext(accountStates, height, new FastScanClusteringStrategy());
	}

	private static class TestAccountInfo {
		public final long vestedBalance;
		public final int[] amounts;

		public TestAccountInfo(final long vestedBalance, final int[] amounts) {
			this.vestedBalance = vestedBalance;
			this.amounts = null == amounts ? new int[] { } : amounts;
		}
	}
}