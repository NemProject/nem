package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.ExceptionAssert;

public class TimeBasedVestingWeightedBalancesTest {

	// region addReceive

	@Test
	public void canAddToEmptyBalances() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		final WeightedBalance referenceBalance = WeightedBalance.createUnvested(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addWithinSameBucketProducesCorrectResult() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		final WeightedBalance referenceBalance = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(100));
		weightedBalances.addReceive(new BlockHeight(1440), Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(100));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addSpanningAcrossGroupsProducesCorrectResults() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		WeightedBalance referenceBalance = WeightedBalance.createUnvested(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next(); // UV[1] = 123 * .9
		referenceBalance = referenceBalance.next(); // UV[2] = UV[1] *.9
		// UV[3] = UV[2] + 345
		referenceBalance = referenceBalance.createReceive(referenceBalance.getBlockHeight(), Amount.fromNem(345));

		// Assert:
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 2881, referenceBalance.getUnvestedBalance());
		// UV[4] = UV[3] * .9
		assertUnvested(weightedBalances, 2881 + 1440, referenceBalance.next().getUnvestedBalance());
		assertUnvested(weightedBalances, 2881 + 1440, Amount.fromMicroNem(400_167_000L));
	}

	// endregion

	// region receiveUndo

	@Test
	public void undoRestoresProperBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		WeightedBalance referenceBalance = WeightedBalance.createUnvested(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));
		final Amount afterNext = weightedBalances.getUnvested(new BlockHeight(2881 + 1440));
		weightedBalances.undoReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();

		// Assert:
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		// use previous test as a reference to obtain proper value here
		MatcherAssert.assertThat(afterNext, IsEqual.equalTo(Amount.fromMicroNem(400_167_000L)));
		assertUnvested(weightedBalances, 2881 + 1440, referenceBalance.getUnvestedBalance());
	}

	// endregion

	// region addSend

	@Test(expected = IllegalArgumentException.class)
	public void cannotSendFromEmptyBalances() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
	}

	@Test
	public void canSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(100));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(100));
	}

	@Test
	public void canSendWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.ZERO);
		assertUnvested(weightedBalances, 1440, Amount.ZERO);
	}

	@Test
	public void canUndoSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoSendWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoSendPartiallyMaturedWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(new BlockHeight(1441), Amount.fromNem(123));
		weightedBalances.undoSend(new BlockHeight(1441), Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, Amount.fromMicroNem((long) (123_000_000 * .9)));
	}

	@Test
	public void canUndoSendWholeBalanceCumulative() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(10_000));
		weightedBalances.addReceive(new BlockHeight(1440), Amount.fromNem(2_000));
		weightedBalances.addReceive(new BlockHeight(1441), Amount.fromNem(300));
		weightedBalances.addReceive(new BlockHeight(2880), Amount.fromNem(40));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(5));
		weightedBalances.addSend(new BlockHeight(2882), Amount.fromNem(12_345));
		weightedBalances.undoSend(new BlockHeight(2882), Amount.fromNem(12_345));

		// Assert:
		assertSum(weightedBalances, 2880, Amount.fromNem(12_340));
		assertSum(weightedBalances, 2881, Amount.fromNem(12_345));
		assertSum(weightedBalances, 2882, Amount.fromNem(12_345));
	}

	@Test
	public void canUndoAfterTimeSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		assertUnvested(weightedBalances, 1, Amount.fromNem(100));

		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	// endregion

	// region safety checks

	@Test
	public void addSendThrowsIfPassedHeightIsPriorToLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = setupWeightedBalances();

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.addSend(new BlockHeight(9), Amount.fromNem(321)),
				IllegalArgumentException.class);
	}

	@Test
	public void addReceiveThrowsIfPassedHeightIsPriorToLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = setupWeightedBalances();

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.addReceive(new BlockHeight(9), Amount.fromNem(321)),
				IllegalArgumentException.class);
	}

	@Test
	public void undoSendThrowsIfPassedHeightIsNotLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = setupWeightedBalances();

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(9), Amount.fromNem(123)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(11), Amount.fromNem(123)),
				IllegalArgumentException.class);
	}

	@Test
	public void undoSendThrowsIfPassedAmountIsNotLastAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = setupWeightedBalances();

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(10), Amount.fromNem(122)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(10), Amount.fromNem(124)),
				IllegalArgumentException.class);
	}

	@Test
	public void undoReceiveThrowsIfPassedHeightIsNotLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1234));

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.undoReceive(new BlockHeight(9), Amount.fromNem(1234)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> weightedBalances.undoReceive(new BlockHeight(11), Amount.fromNem(1234)),
				IllegalArgumentException.class);
	}

	@Test
	public void undoReceiveThrowsIfPassedAmountIsNotLastAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1234));

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(10), Amount.fromNem(1233)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> weightedBalances.undoSend(new BlockHeight(10), Amount.fromNem(1235)),
				IllegalArgumentException.class);
	}

	private static WeightedBalances setupWeightedBalances() {
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1234));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(123));
		return weightedBalances;
	}

	// endregion

	// region getVested / getUnvested

	@Test
	public void getVestedReturnsAmountZeroIfBalancesAreEmpty() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Assert:
		assertVested(weightedBalances, 123, Amount.ZERO);
	}

	@Test
	public void getVestedReturnsAmountZeroIfMinimumHeightOfBalancesIsLargerThanSuppliedHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(123), Amount.fromNem(234));

		// Assert:
		assertVested(weightedBalances, 122, Amount.ZERO);
		assertVested(weightedBalances, 56, Amount.ZERO);
		assertVested(weightedBalances, 1, Amount.ZERO);
	}

	@Test
	public void getVestedReturnsAmountWithHighestIndexForSuppliedHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(1000));
		weightedBalances.convertToFullyVested();
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(14), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(14), Amount.fromNem(100));

		// Act:
		assertVested(weightedBalances, 1, Amount.fromNem(1000));
		assertVested(weightedBalances, 10, Amount.fromNem(600));
		assertVested(weightedBalances, 11, Amount.fromNem(400));
		assertVested(weightedBalances, 12, Amount.fromNem(400));
		assertVested(weightedBalances, 14, Amount.fromNem(200));
		assertVested(weightedBalances, 15, Amount.fromNem(200));
	}

	@Test
	public void getUnvestedReturnsAmountZeroIfBalancesAreEmpty() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Assert:
		assertUnvested(weightedBalances, 123, Amount.ZERO);
	}

	@Test
	public void getUnvestedReturnsAmountZeroIfMinimumHeightOfBalancesIsLargerThanSuppliedHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(123), Amount.fromNem(234));

		// Assert:
		assertUnvested(weightedBalances, 122, Amount.ZERO);
		assertUnvested(weightedBalances, 56, Amount.ZERO);
		assertUnvested(weightedBalances, 1, Amount.ZERO);
	}

	@Test
	public void getUnvestedReturnsAmountWithHighestIndexForSuppliedHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1000));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1000));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(10), Amount.fromNem(100));
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(1000));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(14), Amount.fromNem(100));
		weightedBalances.addSend(new BlockHeight(14), Amount.fromNem(100));

		// Act:
		assertUnvested(weightedBalances, 1, Amount.ZERO);
		assertUnvested(weightedBalances, 10, Amount.fromNem(2600));
		assertUnvested(weightedBalances, 11, Amount.fromNem(2400));
		assertUnvested(weightedBalances, 12, Amount.fromNem(2400));
		assertUnvested(weightedBalances, 14, Amount.fromNem(2200));
		assertUnvested(weightedBalances, 15, Amount.fromNem(2200));
	}

	// endregion

	// region convertToFullyVested

	@Test
	public void convertToFullyVestedFailsIfBalancesSizeIsNotOne() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();

		// Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.convertToFullyVested(), IllegalArgumentException.class);

		// Arrange:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.convertToFullyVested(), IllegalArgumentException.class);
	}

	@Test
	public void convertToFullyVestedFailsIfWeightedBalanceHasHeightOtherThanOne() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(2), Amount.fromNem(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> weightedBalances.convertToFullyVested(), IllegalArgumentException.class);
	}

	@Test
	public void convertToFullyVestedConvertsUnvestedBalanceToVestedBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234)));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));

		// Act:
		weightedBalances.convertToFullyVested();

		// Assert:
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234)));
	}

	// endregion

	// region copy

	@Test
	public void canCopyWeightedBalances() {
		// Arrange:
		final BlockHeight height1 = new BlockHeight(1441);
		final BlockHeight height2 = new BlockHeight(1500);
		final WeightedBalances originalBalances = new TimeBasedVestingWeightedBalances();
		originalBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		originalBalances.addReceive(height2, Amount.fromNem(345));

		// Act
		final ReadOnlyWeightedBalances copiedBalances = originalBalances.copy();

		// Assert:
		assertEqualAtHeight(copiedBalances, originalBalances, BlockHeight.ONE);
		assertEqualAtHeight(copiedBalances, originalBalances, height1);
		assertEqualAtHeight(copiedBalances, originalBalances, height2);
	}

	// endregion

	// region prune

	@Test
	public void pruneRemovesAllOlderBalancesAndPreservesCorrectBalancesAtPruneHeight() {
		// Arrange:
		final long[] heights = new long[]{
				1, 1 + 1440, 1 + 1440 * 2, 1 + 1440 * 3, 1 + 1440 * 4
		};
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(heights[0]), Amount.fromNem(10_000));
		weightedBalances.addSend(new BlockHeight(heights[1]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[2]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[3]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[4]), Amount.fromNem(23));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(9));

		final WeightedBalance referenceBalance = WeightedBalance.createUnvested(new BlockHeight(heights[0]), Amount.fromNem(10_000)).next()
				.createSend(new BlockHeight(heights[1]), Amount.fromNem(23)).next()
				.createSend(new BlockHeight(heights[2]), Amount.fromNem(23));

		// Act:
		weightedBalances.prune(new BlockHeight(heights[2]));

		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(5));
		assertVested(weightedBalances, heights[2], referenceBalance.getVestedBalance());
		assertUnvested(weightedBalances, heights[2], referenceBalance.getUnvestedBalance());
	}

	@Test
	public void pruneRemovesAllOlderBalancesAndPreservesCorrectBalancesAfterPruneHeight() {
		// Arrange:
		final long[] heights = new long[]{
				1, 1 + 1440, 1 + 1440 * 2, 1 + 1440 * 3, 1 + 1440 * 4
		};
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(heights[0]), Amount.fromNem(10_000));
		weightedBalances.addSend(new BlockHeight(heights[1]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[2]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[3]), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(heights[4]), Amount.fromNem(23));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(9));

		final WeightedBalance referenceBalance = WeightedBalance.createUnvested(new BlockHeight(heights[0]), Amount.fromNem(10_000)).next()
				.createSend(new BlockHeight(heights[1]), Amount.fromNem(23)).next()
				.createSend(new BlockHeight(heights[2]), Amount.fromNem(23)).next()
				.createSend(new BlockHeight(heights[3]), Amount.fromNem(23));

		// Act:
		weightedBalances.prune(new BlockHeight(heights[2]));

		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(5));
		assertVested(weightedBalances, heights[3], referenceBalance.getVestedBalance());
		assertUnvested(weightedBalances, heights[3], referenceBalance.getUnvestedBalance());
	}

	// endregion

	// region undoChain

	@Test
	public void undoChainDoesNothingIfPassedHeightIsLargerThanLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(10_000));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(23));

		// Act:
		weightedBalances.undoChain(new BlockHeight(12));

		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(2));
	}

	@Test
	public void undoChainDoesNothingIfPassedHeightIsEqualToLastHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(10_000));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(23));

		// Act:
		weightedBalances.undoChain(new BlockHeight(11));

		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(2));
	}

	@Test
	public void undoChainRemovesEntriesPosteriorToPassedHeight() {
		// Arrange:
		final WeightedBalances weightedBalances = new TimeBasedVestingWeightedBalances();
		weightedBalances.addReceive(new BlockHeight(10), Amount.fromNem(10_000));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(23));
		weightedBalances.addSend(new BlockHeight(11), Amount.fromNem(23));
		weightedBalances.addReceive(new BlockHeight(12), Amount.fromNem(100));
		weightedBalances.addReceive(new BlockHeight(12), Amount.fromNem(200));
		weightedBalances.addReceive(new BlockHeight(12), Amount.fromNem(3100));
		weightedBalances.addSend(new BlockHeight(2100), Amount.fromNem(34));

		// Act:
		weightedBalances.undoChain(new BlockHeight(11));

		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(weightedBalances.getUnvested(new BlockHeight(11)), IsEqual.equalTo(Amount.fromNem(10_000 - 3 * 23)));
		MatcherAssert.assertThat(weightedBalances.getVested(new BlockHeight(11)), IsEqual.equalTo(Amount.ZERO));
	}

	// endregion

	private static void assertUnvested(final ReadOnlyWeightedBalances weightedBalances, final long height, final Amount amount) {
		MatcherAssert.assertThat(weightedBalances.getUnvested(new BlockHeight(height)), IsEqual.equalTo(amount));
	}

	private static void assertVested(final ReadOnlyWeightedBalances weightedBalances, final long height, final Amount amount) {
		MatcherAssert.assertThat(weightedBalances.getVested(new BlockHeight(height)), IsEqual.equalTo(amount));
	}

	private static void assertEqualAtHeight(final ReadOnlyWeightedBalances actualBalances, final ReadOnlyWeightedBalances expectedBalances,
			final BlockHeight height) {
		assertUnvested(actualBalances, height.getRaw(), expectedBalances.getUnvested(height));
		assertVested(actualBalances, height.getRaw(), expectedBalances.getVested(height));
	}

	private static void assertSum(final ReadOnlyWeightedBalances weightedBalances, final long height, final Amount amount) {
		final BlockHeight blockHeight = new BlockHeight(height);
		final Amount actualSum = weightedBalances.getUnvested(blockHeight).add(weightedBalances.getVested(blockHeight));
		MatcherAssert.assertThat(actualSum, IsEqual.equalTo(amount));
	}
}
