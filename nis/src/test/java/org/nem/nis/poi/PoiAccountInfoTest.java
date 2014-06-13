package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.test.MockAccount;

import java.util.*;

public class PoiAccountInfoTest {

	@Test
	public void accountInfoExposesConstructorParameters() {
		// Arrange:
		final BlockHeight height = BlockHeight.ONE;
		final Account account = Utils.generateRandomAccount();
		final PoiAccountInfo info = new PoiAccountInfo(17, account, height);

		// Assert:
		Assert.assertThat(info.getIndex(), IsEqual.equalTo(17));
		Assert.assertThat(info.getAccount(), IsSame.sameInstance(account));
	}

	@Test
	public void foragingRequiresMinimumBalanceAndMinimumVestedBalance() {
		// Assert: balance must be at least one nem
		Assert.assertThat(canForage(Amount.ZERO, Amount.ZERO), IsEqual.equalTo(false));
		Assert.assertThat(canForage(Amount.ZERO, Amount.fromNem(1)), IsEqual.equalTo(false));
		Assert.assertThat(canForage(Amount.fromMicroNem(999999), Amount.fromNem(1)), IsEqual.equalTo(false));

		// Assert: vested balance must be at least one nem
		Assert.assertThat(canForage(Amount.fromNem(1), Amount.ZERO), IsEqual.equalTo(false));
		Assert.assertThat(canForage(Amount.fromNem(1), Amount.fromMicroNem(999999)), IsEqual.equalTo(false));

		// Assert: balance and vested balance must be at least one nem
		Assert.assertThat(canForage(Amount.fromNem(1), Amount.fromNem(1)), IsEqual.equalTo(true));
		Assert.assertThat(canForage(Amount.fromNem(1), Amount.fromNem(2)), IsEqual.equalTo(true));
		Assert.assertThat(canForage(Amount.fromNem(2), Amount.fromNem(1)), IsEqual.equalTo(true));
	}

	private static boolean canForage(final Amount balance, final Amount vestedBalance) {
		// Arrange:
		final BlockHeight height = new BlockHeight(33);
		final MockAccount account = new MockAccount();
		account.incrementBalance(balance);
		account.setVestedBalanceAt(vestedBalance, height);

		// Act:
		return new PoiAccountInfo(11, account, height).canForage();
	}

	@Test
	public void hasOutlinksIsOnlyTrueWhenAccountHasAtLeastOneOutlink() {
		// Assert:
		Assert.assertThat(createAccountInfoWithNullOutlinks().hasOutlinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutlinks().hasOutlinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutlinks(1).hasOutlinks(), IsEqual.equalTo(true));
		Assert.assertThat(createAccountInfoWithOutlinks(2, 4).hasOutlinks(), IsEqual.equalTo(true));
	}

	@Test
	public void outlinkWeightsAreNullWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Assert:
		Assert.assertThat(info.getOutlinkWeights(), IsNull.nullValue());
	}

	@Test
	public void outlinkScoreIsZeroWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Assert:
		Assert.assertThat(info.getOutlinkScore(), IsEqual.equalTo(0.0));
	}

	@Test
	public void outlinkWeightsAreOrderedWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);

		// Assert:
		Assert.assertThat(
				info.getOutlinkWeights(),
				IsEqual.equalTo(new ColumnVector(2.0e06, 3.0e06, 1.0e06, 5.0e06, 9.0e06)));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);

		// Assert: (median * num out-links)
		Assert.assertThat(info.getOutlinkScore(), IsEqual.equalTo(15.0e06));
	}

	@Test
	public void outlinkWeightsAreOrderedWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = PoiAccountInfo.DECAY_BASE;
		final double twoDayDecay = PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE;
		final double threeDayDecay = PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });

		// Assert:
		final ColumnVector expectedWeights = new ColumnVector(
				2.0e06 * threeDayDecay,
				6.0e06 * twoDayDecay,
				3.0e06 * twoDayDecay,
				1.0e06 * oneDayDecay,
				5.0e06 * oneDayDecay,
				8.0e06,
				9.0e06);
		Assert.assertThat(info.getOutlinkWeights(), IsEqual.equalTo(expectedWeights));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = PoiAccountInfo.DECAY_BASE;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });

		// Assert: (median * num out-links)
		Assert.assertThat(info.getOutlinkScore(), IsEqual.equalTo(5.0e06 * oneDayDecay * 7));
	}

	private static PoiAccountInfo createAccountInfoWithNullOutlinks() {
		return createAccountInfoWithOutlinks((List<AccountLink>)null);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final List<AccountLink> outlinks) {
		final BlockHeight height = BlockHeight.ONE;
		final Account account = Utils.generateRandomAccount();
		addAllOutlinks(account, outlinks);
		return new PoiAccountInfo(11, account, height);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final int... amounts) {
		final int[] heights = new int[amounts.length];
		for (int i = 0; i < amounts.length; ++i)
			heights[i] = 1;

		return createAccountInfoWithOutlinks(1, amounts, heights);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(
			final int referenceHeight,
			final int[] amounts,
			final int[] heights) {
		if (amounts.length != heights.length)
			throw new IllegalArgumentException("amounts and heights must have same length");

		final Account account = Utils.generateRandomAccount();

		final List<AccountLink> outlinks = new ArrayList<>();
		for (int i = 0; i < amounts.length; ++i) {
			final AccountLink link = new AccountLink(
					new BlockHeight(heights[i]),
					Amount.fromNem(amounts[i]),
					account.getAddress());
			outlinks.add(link);
		}

		addAllOutlinks(account, outlinks);
		return new PoiAccountInfo(11, account, new BlockHeight(referenceHeight));
	}

	private static void addAllOutlinks(final Account account, final List<AccountLink> outlinks) {
		if (null == outlinks)
			return;

		for (final AccountLink link : outlinks)
			account.getImportanceInfo().addOutlink(link);
	}
}