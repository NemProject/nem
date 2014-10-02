package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.secret.*;

import java.util.*;

public class PoiAccountInfoTest {

	@Test
	public void accountInfoExposesConstructorParameters() {
		// Arrange:
		final BlockHeight height = BlockHeight.ONE;
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		final PoiAccountInfo info = new PoiAccountInfo(17, state, height);

		// Assert:
		Assert.assertThat(info.getIndex(), IsEqual.equalTo(17));
		Assert.assertThat(info.getState(), IsSame.sameInstance(state));
	}

	@Test
	public void cannotForageWhenVestedBalanceIsLessThanMinimumBalance() {
		// Arrange:
		final Amount minBalanceMinusOne = Amount.fromMicroNem(999999999);

		// Assert:
		Assert.assertThat(canForage(Amount.ZERO), IsEqual.equalTo(false));
		Assert.assertThat(canForage(minBalanceMinusOne), IsEqual.equalTo(false));
	}

	@Test
	public void canForageWhenVestedBalanceIsAtLeastMinimumBalance() {
		// Arrange:
		final Amount minBalance = Amount.fromNem(1000);
		final Amount twiceMinBalance = Amount.fromNem(2000);

		// Assert:
		Assert.assertThat(canForage(minBalance), IsEqual.equalTo(true));
		Assert.assertThat(canForage(twiceMinBalance), IsEqual.equalTo(true));
	}

	private static boolean canForage(final Amount vestedBalance) {
		// Arrange:
		final BlockHeight height = new BlockHeight(33);
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(height, vestedBalance);

		// Act:
		return new PoiAccountInfo(11, state, height).canHarvest();
	}

	//region getOutlinks

	@Test
	public void outlinksAreEmptyWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		Assert.assertThat(actualLinks, IsEqual.equalTo(new ArrayList<>()));
	}

	@Test
	public void outlinksAreCorrectWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(
				new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06),
				new WeightedLink(Address.fromEncoded("acc 1"), 3.0e06),
				new WeightedLink(Address.fromEncoded("acc 2"), 1.0e06),
				new WeightedLink(Address.fromEncoded("acc 3"), 5.0e06),
				new WeightedLink(Address.fromEncoded("acc 4"), 9.0e06));
		Assert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	@Test
	public void outlinksAreCorrectWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = WeightedBalanceDecayConstants.DECAY_BASE;
		final double twoDayDecay = oneDayDecay * oneDayDecay;
		final double threeDayDecay = twoDayDecay * oneDayDecay;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(
				new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * threeDayDecay),
				new WeightedLink(Address.fromEncoded("acc 1"), 6.0e06 * twoDayDecay),
				new WeightedLink(Address.fromEncoded("acc 2"), 3.0e06 * twoDayDecay),
				new WeightedLink(Address.fromEncoded("acc 3"), 1.0e06 * oneDayDecay),
				new WeightedLink(Address.fromEncoded("acc 4"), 5.0e06 * oneDayDecay),
				new WeightedLink(Address.fromEncoded("acc 5"), 8.0e06),
				new WeightedLink(Address.fromEncoded("acc 6"), 9.0e06));
		Assert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	//endregion

	//region getNetOutlinks

	@Test
	public void netOutlinksAreEmptyWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Act:
		final List<WeightedLink> actualLinks = info.getNetOutlinks();

		// Assert:
		Assert.assertThat(actualLinks, IsEqual.equalTo(new ArrayList<>()));
	}

	@Test
	public void netOutlinksAreCorrectWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 4"), 2.0e06));

		// Act:
		final List<WeightedLink> actualLinks = info.getNetOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(
				new WeightedLink(Address.fromEncoded("acc 1"), 3.0e06),
				new WeightedLink(Address.fromEncoded("acc 3"), 5.0e06),
				new WeightedLink(Address.fromEncoded("acc 4"), 7.0e06));
		Assert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	@Test
	public void netOutlinksAreCorrectWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = WeightedBalanceDecayConstants.DECAY_BASE;
		final double twoDayDecay = oneDayDecay * oneDayDecay;
		final double threeDayDecay = twoDayDecay * oneDayDecay;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * threeDayDecay));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 8.0e06 * twoDayDecay));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 5"), 2.5e06));

		// Act:
		final List<WeightedLink> actualLinks = info.getNetOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(
				new WeightedLink(Address.fromEncoded("acc 1"), 6.0e06 * twoDayDecay),
				new WeightedLink(Address.fromEncoded("acc 3"), 1.0e06 * oneDayDecay),
				new WeightedLink(Address.fromEncoded("acc 4"), 5.0e06 * oneDayDecay),
				new WeightedLink(Address.fromEncoded("acc 5"), 5.5e06),
				new WeightedLink(Address.fromEncoded("acc 6"), 9.0e06));
		Assert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	//endregion

	//region getNetOutlinkScore

	@Test
	public void outlinkScoreIsZeroWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Assert:
		Assert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(0.0));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 4"), 2.0e06));

		// Assert: sum(net out-links)
		Assert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(1.5e07));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		final double oneDayDecay = WeightedBalanceDecayConstants.DECAY_BASE;
		final double twoDayDecay = oneDayDecay * oneDayDecay;
		final double threeDayDecay = twoDayDecay * oneDayDecay;

		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(
				4322,
				new int[] { 2, 6, 3, 1, 5, 8, 9, 11, 7 },
				new int[] { 2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000 });
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * threeDayDecay));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 8.0e06 * twoDayDecay));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 5"), 2.5e06));

		// Assert: sum(net out-links)
		final double expectedScore = 6.0e06 * twoDayDecay + (1.0e06 + 5.0e06) * oneDayDecay + 5.50e06 + 9.0e06;
		Assert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(expectedScore));
	}

	//endregion

	private static PoiAccountInfo createAccountInfoWithNullOutlinks() {
		return createAccountInfoWithOutlinks((List<AccountLink>)null);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final List<AccountLink> outlinks) {
		final BlockHeight height = BlockHeight.ONE;
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		addAllOutlinks(state, outlinks);
		return new PoiAccountInfo(11, state, height);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final int... amounts) {
		final int[] heights = new int[amounts.length];
		for (int i = 0; i < amounts.length; ++i) {
			heights[i] = 1;
		}

		return createAccountInfoWithOutlinks(1, amounts, heights);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(
			final int referenceHeight,
			final int[] amounts,
			final int[] heights) {
		if (amounts.length != heights.length) {
			throw new IllegalArgumentException("amounts and heights must have same length");
		}

		final List<AccountLink> outlinks = new ArrayList<>();
		for (int i = 0; i < amounts.length; ++i) {
			final Account otherAccount = Mockito.mock(Account.class);
			Mockito.when(otherAccount.getAddress()).thenReturn(Address.fromEncoded(String.format("acc %d", i)));

			final AccountLink link = new AccountLink(
					new BlockHeight(heights[i]),
					Amount.fromNem(amounts[i]),
					otherAccount.getAddress());
			outlinks.add(link);
		}

		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		addAllOutlinks(state, outlinks);
		return new PoiAccountInfo(11, state, new BlockHeight(referenceHeight));
	}

	private static void addAllOutlinks(final PoiAccountState state, final List<AccountLink> outlinks) {
		if (null == outlinks) {
			return;
		}

		for (final AccountLink link : outlinks) {
			state.getImportanceInfo().addOutlink(link);
		}
	}
}