package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.*;

import java.util.*;
import java.util.stream.Collectors;

public class PoiContextTest {
	private static final PoiOptions DEFAULT_OPTIONS = new PoiOptionsBuilder().create();
	private static final Amount MIN_HARVESTING_BALANCE = DEFAULT_OPTIONS.getMinHarvesterBalance();

	//region construction (failures)

	@Test
	public void cannotCreateContextAroundZeroAccounts() {
		// Arrange:
		final List<TestAccountInfo> accountInfos = Arrays.asList();

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		ExceptionAssert.assertThrows(v -> createPoiContext(accountStates, height), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateContextAroundZeroForagingEligibleAccounts() {
		// Arrange:
		final long multiplier = 1000 * Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(multiplier - 1, null), // non-foraging account
				new TestAccountInfo(multiplier - 1, null)); // non-foraging account

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccountStates(accountInfos, height);

		// Act:
		ExceptionAssert.assertThrows(v -> createPoiContext(accountStates, height), IllegalArgumentException.class);
	}

	//endregion

	//region process

	//region vectors

	@Test
	public void vestedBalanceVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) only harvesting-eligible accounts (0, 2, 3, 4) are represented
		// (2) vested balances are not normalized
		Assert.assertThat(
				context.getVestedBalanceVector(),
				IsEqual.equalTo(new ColumnVector(2999999999L, 5000000000L, 1000000000L, 1000000000L)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectly() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) only harvesting-eligible accounts (0, 2, 3, 4) are represented
		// (2) calculation delegates to PoiAccountInfo
		Assert.assertThat(
				context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(0, 3e06, 0, 10e06)));
	}

	@Test
	public void outlinkScoreVectorIsInitializedCorrectlyWhenThereAreBidirectionalFlows() {
		// Act:
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setNegativeOutlinkWeight(0.4);
		final PoiContext context = createTestPoiContextWithAccountLinks(poiOptionsBuilder.create());

		// Assert:
		// (1) only harvesting-eligible accounts (0, 1, 2, 3) are represented
		// (2) calculation delegates to PoiAccountInfo
		// (3) negative outflows are scaled
		// (4) net outflows are used instead of total outflows
		Assert.assertThat(
				context.getOutlinkScoreVector().roundTo(5),
				IsEqual.equalTo(new ColumnVector(9e06, 0, 0.4 * -15e06, 8e06)));
	}

	@Test
	public void poiStartVectorIsInitializedToNormalizedUniformVector() {
		// Act:
		final PoiContext context = createPoiContextWithDefaultTestAccountStates();

		// Assert:
		// (1) start vector is uniform
		// (2) start vector is normalized
		Assert.assertThat(
				context.getPoiStartVector(),
				IsEqual.equalTo(new ColumnVector(0.25, 0.25, 0.25, 0.25)));
	}

	//endregion

	//region dangle indexes

	@Test
	public void dangleIndexesAreInitializedCorrectly() {
		// Act:
		final PoiContext context = createTestPoiContextWithAccountLinks();

		// Assert:
		// (1) accounts without outlinks are dangling (2 has inlinks but no outlinks)
		Assert.assertThat(
				context.getDangleIndexes(),
				IsEquivalent.equivalentTo(new Integer[] { 2 }));
	}

	//endregion

	//region matrices

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

		Assert.assertThat(
				context.getOutlinkMatrix().roundTo(5),
				IsEqual.equalTo(expectedAccountLinks));
	}

	@Test
	public void outlinkMatrixIsInitializedCorrectlyWhenNonZeroOutlinkWeightIsConfigured() {
		// Act:
		// (0, 1, 8), (0, 2, 4), (1, 0, 2), (1, 2, 6), (3, 0, 3), (3, 2, 5)
		// ==> (0, 1, 6), (0, 2, 4), (1, 2, 6), (3, 2, 5)
		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setMinOutlinkWeight(Amount.fromNem(4));
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

		Assert.assertThat(
				context.getOutlinkMatrix().roundTo(5),
				IsEqual.equalTo(expectedAccountLinks));
	}

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
		r.setAt(0, 0, 1.0 / 3.0); // N(0): 1; |A(0)|: 3
		r.setAt(0, 1, 1.0 / 3.0); // N(1): 1; |A(0)|: 3
		r.setAt(0, 2, 1.0 / 9.0); // N(2): 3; |A(0)|: 3
		r.setAt(1, 3, 1.0 / 6.0); // N(3): 2; |A(1)|: 3
		r.setAt(1, 4, 1.0 / 3.0); // N(4): 1; |A(1)|: 3
		r.setAt(1, 5, 1.0 / 3.0); // N(5): 1; |A(1)|: 3
		r.setAt(1, 6, 1.0 / 3.0); // N(6): 1; |A(1)|: 3
		r.setAt(2, 2, 1.0 / 3.0); // N(2): 3; |A(2)|: 1
		r.setAt(2, 3, 1.0 / 2.0); // N(3): 2; |A(2)|: 1
		r.setAt(3, 2, 1.0 / 3.0); // N(2): 3; |A(3)|: 1
		r.setAt(3, 7, 1.0 / 1.0); // N(7): 1; |A(3)|: 1

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	//endregion

	//endregion

	//region updateImportances

	@Test
	public void canUpdateFilteredAccountsWithCompatiblePageRankVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(5, 2, 7, 3), new ColumnVector(4));

		// Assert:
		// - accounts without harvesting power are given 0 page rank
		final List<Double> importances = accountStates.stream()
				.map(a -> a.getImportanceInfo().getLastPageRank())
				.collect(Collectors.toList());
		Assert.assertThat(importances, IsEqual.equalTo(Arrays.asList(5.0, 0.0, 2.0, 7.0, 3.0, 0.0)));
	}

	@Test
	public void canUpdateFilteredAccountsWithCompatibleImportanceVector() {
		// Arrange:
		final BlockHeight height = new BlockHeight(17);
		final List<PoiAccountState> accountStates = createDefaultTestAccountStates(height);
		final PoiContext context = createPoiContext(accountStates, height);

		// Act:
		context.updateImportances(new ColumnVector(4), new ColumnVector(5, 2, 7, 3));

		// Assert:
		// - accounts without harvesting power are given 0 importance
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
		final List<PoiAccountState> accountStates = createDefaultTestAccountStates(height);
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
		final List<PoiAccountState> accountStates = createDefaultTestAccountStates(height);
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

	//region test helpers

	//region utilities

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

	private static List<PoiAccountState> createDefaultTestAccountStates(final BlockHeight height) {
		final long multiplier = MIN_HARVESTING_BALANCE.getNumMicroNem();
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(3 * multiplier - 1, null),
				new TestAccountInfo(multiplier - 1, new int[] { 1 }), // 1 (insufficient balance)
				new TestAccountInfo(5 * multiplier, new int[] { 1, 2 }), // 3
				new TestAccountInfo(multiplier, null),
				new TestAccountInfo(multiplier, new int[] { 1, 1, 4, 3, 1 }), // 10
				new TestAccountInfo(multiplier - 1, new int[] { 7 })); // 7 (insufficient vested balance)

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

		public TestAccountInfo(final long vestedBalance, final int[] amounts) {
			this.vestedBalance = vestedBalance;
			this.amounts = null == amounts ? new int[] { } : amounts;
		}
	}

	//endregion

	//region PoiContext factories

	private static PoiContext createPoiContextWithDefaultTestAccountStates() {
		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createDefaultTestAccountStates(height);
		return createPoiContext(accountStates, height);
	}

	private static PoiContext createPoiContext(final List<PoiAccountState> accountStates, final BlockHeight height) {
		return new PoiContext(accountStates, height, DEFAULT_OPTIONS);
	}

	private static PoiContext createTestPoiContextWithAccountLinks() {
		return createTestPoiContextWithAccountLinks(DEFAULT_OPTIONS);
	}

	private static PoiContext createTestPoiContextWithAccountLinks(final PoiOptions poiOptions) {
		// Arrange: create 4 accounts
		final long multiplier = MIN_HARVESTING_BALANCE.getNumMicroNem();// 1000 is min harvesting balance
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

		// outlinks
		// - 0: (8, 4, 2) - (2, 3) -- 9
		// - 1: (2, 6) - (8) -------- 0
		// - 2: (none) - (4, 6, 5) -- -15
		// - 3: (3, 5) - (none) ----- 9
		return new PoiContext(accountStates, height, poiOptions);
	}

	/**
	 * <pre>
	 * Graph:         0
	 *               / \
	 *              o   o
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
	 * </pre>
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
		addAccountLink(height, accountStates.get(5), accountStates.get(6), 1);

		// Act:
		return createPoiContext(accountStates, height);
	}

	//endregion
}