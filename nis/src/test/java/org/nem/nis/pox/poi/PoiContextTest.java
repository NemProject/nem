package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.pox.poi.graph.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class PoiContextTest {
	private static final PoiOptions DEFAULT_OPTIONS = new PoiOptionsBuilder().create();
	private static final long MIN_HARVESTING_BALANCE = DEFAULT_OPTIONS.getMinHarvesterBalance().getNumMicroNem();
	private static final long MIN_OUTLINK_WEIGHT = DEFAULT_OPTIONS.getMinOutlinkWeight().getNumMicroNem();

	// region construction (failures)

	@Test
	public void cannotCreateContextAroundZeroAccounts() {
		// Arrange:
		final List<TestAccountInfo> accountInfos = Collections.emptyList();

		final BlockHeight height = new BlockHeight(21);
		final List<AccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		ExceptionAssert.assertThrows(v -> createPoiContext(accountStates, height), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateContextAroundZeroForagingEligibleAccounts() {
		// Arrange:
		final long multiplier = 1000 * Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(new TestAccountInfo(multiplier - 1, null), // non-harvesting account
				new TestAccountInfo(multiplier - 1, null)); // non-harvesting account

		final BlockHeight height = new BlockHeight(21);
		final List<AccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		ExceptionAssert.assertThrows(v -> createPoiContext(accountStates, height), IllegalArgumentException.class);
	}

	// endregion

	// region vectors

	@Test
	public void vestedBalanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) only harvesting-eligible accounts (0, 2, 3, 4) are represented
		// (2) vested balances are not normalized
		MatcherAssert.assertThat(context.getVestedBalanceVector(),
				IsEqual.equalTo(new ColumnVector(29999999999L, 50000000000L, 10000000000L, 10000000000L)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) only harvesting-eligible accounts (0, 2, 3, 4) are represented
		// (2) calculation delegates to PoiAccountInfo
		MatcherAssert.assertThat(context.getOutlinkScoreVector().roundTo(5), IsEqual.equalTo(new ColumnVector(0, 3e06, 0, 10e06)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectlyWhenThereAreBidirectionalFlows() {
		// Act:
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setNegativeOutlinkWeight(0.6);
		final PoiContext context = createTestPoiContextWithAccountLinks(poiOptionsBuilder.create());

		// Assert:
		// (1) only harvesting-eligible accounts (0, 1, 2, 3) are represented
		// (2) calculation delegates to PoiAccountInfo
		// (3) negative outflows are scaled
		// (4) net outflows are used instead of total outflows
		MatcherAssert.assertThat(context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(9 * MIN_OUTLINK_WEIGHT, 0, 0.6 * -15 * MIN_OUTLINK_WEIGHT, 8 * MIN_OUTLINK_WEIGHT)));
	}

	@Test
	public void poiStartVectorIsInitializedToNormalizedUniformVector() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) start vector is uniform
		// (2) start vector is normalized
		MatcherAssert.assertThat(context.getPoiStartVector(), IsEqual.equalTo(new ColumnVector(0.25, 0.25, 0.25, 0.25)));
	}

	@Test
	public void outlierVectorIsSetCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithRealGraph();

		// Assert:
		// (1) values corresponding to outliers are 1
		// (2) values corresponding to non-outliers are 0
		final ColumnVector expectedOutlierVector = new ColumnVector(20);
		expectedOutlierVector.setAt(13, 1);
		expectedOutlierVector.setAt(17, 1);
		expectedOutlierVector.setAt(19, 1);
		MatcherAssert.assertThat(context.getOutlierVector(), IsEqual.equalTo(expectedOutlierVector));
	}

	@Test
	public void graphWeightVectorIsSetCorrectly() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setOutlierWeight(0.75);
		final PoiContext context = createTestPoiContextWithRealGraph(builder);

		// Assert:
		// (1) values corresponding to outliers are 0.75
		// (2) values corresponding to non-outliers are 1
		final ColumnVector expectedGraphWeightVector = new ColumnVector(20);
		expectedGraphWeightVector.setAll(1);
		expectedGraphWeightVector.setAt(13, 0.75);
		expectedGraphWeightVector.setAt(17, 0.75);
		expectedGraphWeightVector.setAt(19, 0.75);
		MatcherAssert.assertThat(context.getGraphWeightVector(), IsEqual.equalTo(expectedGraphWeightVector));
	}

	// endregion

	// region dangle indexes

	@Test
	public void dangleIndexesAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) accounts without outlinks are dangling (2 has inlinks but no outlinks)
		MatcherAssert.assertThat(context.getDangleIndexes(), IsEquivalent.equivalentTo(2));
	}

	// endregion

	// region matrices

	@Test
	public void outlinkMatrixIsInitializedCorrectly() {
		// Act:
		// (0, 1, 8), (0, 2, 4)
		// (1, 0, 2), (1, 2, 6)
		// (3, 0, 3), (3, 2, 5)
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) account link weights are normalized
		// (2) net outlinks are used ((0, 1, 8) + (1, 0, 2) => (0, 1, 6))
		final Matrix expectedAccountLinks = new DenseMatrix(4, 4);
		expectedAccountLinks.setAt(0, 3, 0.375);
		expectedAccountLinks.setAt(1, 0, 0.6);
		expectedAccountLinks.setAt(2, 0, 0.4);
		expectedAccountLinks.setAt(2, 1, 1.0);
		expectedAccountLinks.setAt(2, 3, 0.625);

		MatcherAssert.assertThat(context.getOutlinkMatrix().roundTo(5), IsEqual.equalTo(expectedAccountLinks));
	}

	@Test
	public void outlinkMatrixIsInitializedCorrectlyWhenNonZeroOutlinkWeightIsConfigured() {
		// Act:
		// (0, 1, 8), (0, 2, 4), (1, 0, 2), (1, 2, 6), (3, 0, 3), (3, 2, 5)
		// ==> (0, 1, 6), (0, 2, 4), (1, 2, 6), (3, 2, 5)
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setMinOutlinkWeight(Amount.fromMicroNem(4 * MIN_OUTLINK_WEIGHT));
		final PoiOptions options = poiOptionsBuilder.create();
		final PoiContext context = createTestPoiContextWithAccountLinks(options);

		// Assert:
		// (1) account link weights are normalized
		// (2) net outlinks are used ((0, 1, 8) + (1, 0, 2) => (0, 1, 6))
		// (3) net outlinks less than 5 are ignored
		final Matrix expectedAccountLinks = new DenseMatrix(4, 4);
		expectedAccountLinks.setAt(1, 0, 0.6);
		expectedAccountLinks.setAt(2, 0, 0.4);
		expectedAccountLinks.setAt(2, 1, 1.0);
		expectedAccountLinks.setAt(2, 3, 1.0);

		MatcherAssert.assertThat(context.getOutlinkMatrix().roundTo(5), IsEqual.equalTo(expectedAccountLinks));
	}

	@Test
	public void interLevelProximityMatrixIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithRealGraph();

		// Assert:
		final InterLevelProximityMatrix interLevel = context.getInterLevelMatrix();
		InterLevelProximityMatrixTest.assertInterLevelMatrixForGraphWithThreeClustersTwoHubsAndThreeOutliers(interLevel);
	}

	// endregion

	// region clustering result

	@Test
	public void clusteringResultIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithRealGraph();
		final ClusteringResult result = context.getClusteringResult();

		// Assert:
		final ClusteringResult expectedResult = IdealizedClusterFactory
				.create(GraphTypeEpsilon040.GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS);
		MatcherAssert.assertThat(result.getClusters(), IsEquivalent.equivalentTo(expectedResult.getClusters()));
		MatcherAssert.assertThat(result.getHubs(), IsEquivalent.equivalentTo(expectedResult.getHubs()));
		MatcherAssert.assertThat(result.getOutliers(), IsEqual.equalTo(expectedResult.getOutliers()));
	}

	// endregion

	// region updateImportances

	@Test
	public void canUpdateFilteredAccountsWithCompatiblePageRankVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(5, 2, 7, 3), new ColumnVector(4));

		// Assert:
		// - accounts without harvesting power are given 0 page rank
		final List<Double> importances = accountStates.stream().map(a -> a.getImportanceInfo().getLastPageRank())
				.collect(Collectors.toList());
		MatcherAssert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
	}

	@Test
	public void canUpdateFilteredAccountsWithCompatibleImportanceVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 7, 3));

		// Assert:
		// - accounts without harvesting power are given 0 importance
		final List<Double> importances = accountStates.stream().map(a -> {
			final ReadOnlyAccountImportance ai = a.getImportanceInfo();
			return ai.isSet() ? ai.getImportance(height) : 0.0;
		}).collect(Collectors.toList());
		MatcherAssert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
	}

	@Test
	public void cannotUpdateFilteredAccountsWithIncompatiblePageRankVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Assert:
		ExceptionAssert.assertThrows(v -> context.updateImportances(new ColumnVector(5, 2, 7, 3, 4, 8), new ColumnVector(4)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> context.updateImportances(new ColumnVector(5, 2, 3), new ColumnVector(4)),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotUpdateFilteredAccountsWithIncompatibleImportanceVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Assert:
		ExceptionAssert.assertThrows(v -> context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 7, 3, 4, 8)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 3)),
				IllegalArgumentException.class);
	}

	@Test
	public void updateImportancesAddsHistoricalImportanceForFilteredAccounts() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(3, 7, 2, 5), new ColumnVector(5, 2, 7, 3));

		// Assert:
		// - accounts without harvesting power have 0 historical importance and page rank
		final List<Double> importances = accountStates.stream().map(a -> a.getHistoricalImportances().getHistoricalImportance(height))
				.collect(Collectors.toList());
		final List<Double> lastPageRanks = accountStates.stream().map(a -> a.getHistoricalImportances().getHistoricalPageRank(height))
				.collect(Collectors.toList());
		MatcherAssert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
		MatcherAssert.assertThat(lastPageRanks, IsEqual.equalTo(Arrays.asList(3.0, 0.0, 7.0, 2.0, 5.0, 0.0)));
	}

	@Test
	public void updateImportancesAddsNoHistoricalImportanceForOtherHeights() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(3, 7, 2, 5), new ColumnVector(5, 2, 7, 3));

		// Assert:
		final int numHistoricalEntries = accountStates.stream().map(a -> a.getHistoricalImportances().size()).reduce(0, Integer::sum);
		MatcherAssert.assertThat(numHistoricalEntries, IsEqual.equalTo(4));
	}

	// endregion

	// region utilities

	private static void addAccountLink(final BlockHeight height, final AccountState sender, final AccountState recipient,
			final int weight) {
		final Amount amount = Amount.fromMicroNem(weight * MIN_OUTLINK_WEIGHT);
		final AccountLink link = new AccountLink(height, amount, recipient.getAddress());
		sender.getImportanceInfo().addOutlink(link);
	}

	private static List<AccountState> createTestPoiAccountStates(final List<TestAccountInfo> accountInfos, final BlockHeight height) {
		final List<AccountState> accountStates = new ArrayList<>();
		for (final TestAccountInfo info : accountInfos) {
			final AccountState state = new AccountState(Utils.generateRandomAddress());
			state.getWeightedBalances().addFullyVested(height, Amount.fromMicroNem(info.vestedBalance));

			for (final int amount : info.amounts) {
				final AccountLink link = new AccountLink(height, Amount.fromNem(amount), Utils.generateRandomAddress());
				state.getImportanceInfo().addOutlink(link);
			}

			accountStates.add(state);
		}

		return accountStates;
	}

	private static List<AccountState> createDefaultTestAccountStates(final BlockHeight height) {
		final List<TestAccountInfo> accountInfos = Arrays.asList(new TestAccountInfo(3 * MIN_HARVESTING_BALANCE - 1, null),
				new TestAccountInfo(MIN_HARVESTING_BALANCE - 1, new int[]{
						1
				}), // 1 (insufficient balance)
				new TestAccountInfo(5 * MIN_HARVESTING_BALANCE, new int[]{
						1, 2
				}), // 3
				new TestAccountInfo(MIN_HARVESTING_BALANCE, null), new TestAccountInfo(MIN_HARVESTING_BALANCE, new int[]{
						1, 1, 4, 3, 1
				}), // 10
				new TestAccountInfo(MIN_HARVESTING_BALANCE - 1, new int[]{
						7
				})); // 7 (insufficient vested balance)

		// outlinks
		// - 0: none
		// - 2: 1, 2
		// - 3: none
		// - 4: 1, 1, 4, 5, 1
		return createTestPoiAccountStates(accountInfos, height);
	}

	private static class TestAccountInfo {
		public final long vestedBalance;
		public final int[] amounts;

		public TestAccountInfo(final long vestedBalance) {
			this(vestedBalance, null);
		}

		public TestAccountInfo(final long vestedBalance, final int[] amounts) {
			this.vestedBalance = vestedBalance;
			this.amounts = null == amounts ? new int[]{} : amounts;
		}
	}

	// endregion

	// region PoiContext factories

	private static PoiContext createPoiContextWithDefaultTestAccountStates() {
		final BlockHeight height = new BlockHeight(21);
		final List<AccountState> accountStates = createDefaultTestAccountStates(height);
		return createPoiContext(accountStates, height);
	}

	private static PoiContext createPoiContext(final List<AccountState> accountStates, final BlockHeight height) {
		return new PoiContext(accountStates, height, DEFAULT_OPTIONS);
	}

	private static PoiContext createTestPoiContextWithAccountLinks() {
		return createTestPoiContextWithAccountLinks(DEFAULT_OPTIONS);
	}

	private static PoiContext createTestPoiContextWithAccountLinks(final PoiOptions poiOptions) {
		// Arrange: create 4/5 harvesting accounts
		final List<TestAccountInfo> accountInfos = Arrays.asList(new TestAccountInfo(MIN_HARVESTING_BALANCE),
				new TestAccountInfo(MIN_HARVESTING_BALANCE), new TestAccountInfo(MIN_HARVESTING_BALANCE),
				new TestAccountInfo(MIN_HARVESTING_BALANCE), new TestAccountInfo(MIN_HARVESTING_BALANCE - 1, null));

		final BlockHeight height = new BlockHeight(21);
		final List<AccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// set up account links
		addAccountLink(height, accountStates.get(0), accountStates.get(1), 8);
		addAccountLink(height, accountStates.get(0), accountStates.get(2), 4);
		addAccountLink(height, accountStates.get(1), accountStates.get(0), 2);
		addAccountLink(height, accountStates.get(1), accountStates.get(2), 6);
		addAccountLink(height, accountStates.get(3), accountStates.get(0), 3);
		addAccountLink(height, accountStates.get(3), accountStates.get(2), 5);
		addAccountLink(height, accountStates.get(4), accountStates.get(2), 5); // from non-harvesting account (ignored)
		addAccountLink(height, accountStates.get(0), accountStates.get(4), 2); // to non-harvesting account (included in scores)

		// outlinks
		// - 0: (8, 4, 2) - (2, 3) -- 9
		// - 1: (2, 6) - (8) -------- 0
		// - 2: (none) - (4, 6, 5) -- -15
		// - 3: (3, 5) - (none) ----- 8
		return new PoiContext(accountStates, height, poiOptions);
	}

	private static PoiContext createTestPoiContextWithRealGraph() {
		return createTestPoiContextWithRealGraph(new PoiOptionsBuilder());
	}

	private static PoiContext createTestPoiContextWithRealGraph(final PoiOptionsBuilder poiOptionsBuilder) {
		// Arrange:
		// - the test matrix assumes an epsilon value of 0.4
		poiOptionsBuilder.setEpsilonClusteringValue(0.4);
		final Matrix matrix = OutlinkMatrixFactory.create(GraphTypeEpsilon040.GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS);

		// - create accounts
		final List<TestAccountInfo> accountInfos = new ArrayList<>();
		for (int i = 0; i < matrix.getRowCount(); ++i) {
			accountInfos.add(new TestAccountInfo(MIN_HARVESTING_BALANCE));
		}

		final BlockHeight height = new BlockHeight(21);
		final List<AccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// - set up account links
		for (int i = 0; i < matrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				addAccountLink(height, accountStates.get(element.getColumn()), accountStates.get(element.getRow()), 1);
			}
		}

		// Act:
		return new PoiContext(accountStates, height, poiOptionsBuilder.create());
	}

	// endregion
}
