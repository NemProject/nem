package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
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
	public void hasOutLinksIsOnlyTrueWhenAccountHasAtLeastOneOutLink() {
		// Assert:
		Assert.assertThat(createAccountInfoWithNullOutLinks().hasOutLinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutLinks().hasOutLinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutLinks(1).hasOutLinks(), IsEqual.equalTo(true));
		Assert.assertThat(createAccountInfoWithOutLinks(2, 4).hasOutLinks(), IsEqual.equalTo(true));
	}

	@Test
	public void outLinkWeightsAreNullWhenAccountHasNoOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutLinks();

		// Assert:
		Assert.assertThat(info.getOutLinkWeights(), IsNull.nullValue());
	}

	@Test
	public void outLinkScoreIsZeroWhenAccountHasNoOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutLinks();

		// Assert:
		Assert.assertThat(info.getOutLinkScore(), IsEqual.equalTo(0.0));
	}

	@Test
	public void outLinkWeightsAreOrderedWhenAccountHasOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutLinks(2, 3, 1, 5, 9);

		// Assert:
		Assert.assertThat(
				info.getOutLinkWeights(),
				IsEqual.equalTo(new ColumnVector(2.0e06, 3.0e06, 1.0e06, 5.0e06, 9.0e06)));
	}

	@Test
	public void outLinkScoreIsComputedCorrectlyWhenAccountHasOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutLinks(2, 3, 1, 5, 9);

		// Assert: (median * num out-links)
		Assert.assertThat(info.getOutLinkScore(), IsEqual.equalTo(15.0e06));
	}

	@Test
	public void outLinkWeightsAreOrderedWhenAccountHasOutLinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = PoiAccountInfo.DECAY_BASE;
		final double twoDayDecay = PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE;
		final double threeDayDecay = PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutLinks(
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
		Assert.assertThat(info.getOutLinkWeights(), IsEqual.equalTo(expectedWeights));
	}

	@Test
	public void outLinkScoreIsComputedCorrectlyWhenAccountHasOutLinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = PoiAccountInfo.DECAY_BASE;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutLinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });

		// Assert: (median * num out-links)
		Assert.assertThat(info.getOutLinkScore(), IsEqual.equalTo(5.0e06 * oneDayDecay * 7));
	}

	private static PoiAccountInfo createAccountInfoWithNullOutLinks() {
		return createAccountInfoWithOutLinks((List<AccountLink>)null);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final List<AccountLink> outLinks) {
		final BlockHeight height = BlockHeight.ONE;
		final Account account = Utils.generateRandomAccount();
		addAllOutLinks(account, outLinks);
		return new PoiAccountInfo(11, account, height);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final int... amounts) {
		final int[] heights = new int[amounts.length];
		for (int i = 0; i < amounts.length; ++i)
			heights[i] = 1;

		return createAccountInfoWithOutLinks(1, amounts, heights);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(
			final int referenceHeight,
			final int[] amounts,
			final int[] heights) {
		if (amounts.length != heights.length)
			throw new IllegalArgumentException("amounts and heights must have same length");

		final Account account = Utils.generateRandomAccount();

		final List<AccountLink> outLinks = new ArrayList<>();
		for (int i = 0; i < amounts.length; ++i) {
			final AccountLink link = new AccountLink(
					new BlockHeight(heights[i]),
					Amount.fromNem(amounts[i]),
					account.getAddress());
			outLinks.add(link);
		}

		addAllOutLinks(account, outLinks);
		return new PoiAccountInfo(11, account, new BlockHeight(referenceHeight));
	}

	private static void addAllOutLinks(final Account account, final List<AccountLink> outLinks) {
		if (null == outLinks)
			return;

		for (final AccountLink link : outLinks)
			account.getImportanceInfo().addOutLink(link);
	}
}