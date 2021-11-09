package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisTestConstants;

import java.util.*;

public class PoiAccountInfoTest {
	private static final long OUTLINK_HISTORY = 30 * NisTestConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final double ONE_DAY_DECAY = WeightedBalanceDecayConstants.DECAY_BASE;
	private static final double TWO_DAY_DECAY = ONE_DAY_DECAY * ONE_DAY_DECAY;
	private static final double THREE_DAY_DECAY = TWO_DAY_DECAY * ONE_DAY_DECAY;

	@Test
	public void accountInfoExposesConstructorParameters() {
		// Arrange:
		final BlockHeight height = BlockHeight.ONE;
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final PoiAccountInfo info = new PoiAccountInfo(17, state, height);

		// Assert:
		MatcherAssert.assertThat(info.getIndex(), IsEqual.equalTo(17));
		MatcherAssert.assertThat(info.getState(), IsSame.sameInstance(state));
	}

	// region getOutlinks

	@Test
	public void outlinksAreEmptyWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		MatcherAssert.assertThat(actualLinks, IsEqual.equalTo(new ArrayList<>()));
	}

	@Test
	public void outlinksAreCorrectWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06),
				new WeightedLink(Address.fromEncoded("acc 1"), 3.0e06), new WeightedLink(Address.fromEncoded("acc 2"), 1.0e06),
				new WeightedLink(Address.fromEncoded("acc 3"), 5.0e06), new WeightedLink(Address.fromEncoded("acc 4"), 9.0e06));
		MatcherAssert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	@Test
	public void outlinksAreCorrectWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(4322, new int[]{
				2, 6, 3, 1, 5, 8, 9, 11, 7
		}, new int[]{
				2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000
		});

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		final List<WeightedLink> expectedLinks = Arrays.asList(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * THREE_DAY_DECAY),
				new WeightedLink(Address.fromEncoded("acc 1"), 6.0e06 * TWO_DAY_DECAY),
				new WeightedLink(Address.fromEncoded("acc 2"), 3.0e06 * TWO_DAY_DECAY),
				new WeightedLink(Address.fromEncoded("acc 3"), 1.0e06 * ONE_DAY_DECAY),
				new WeightedLink(Address.fromEncoded("acc 4"), 5.0e06 * ONE_DAY_DECAY),
				new WeightedLink(Address.fromEncoded("acc 5"), 8.0e06), new WeightedLink(Address.fromEncoded("acc 6"), 9.0e06));
		MatcherAssert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	@Test
	public void outlinksOlderThanOutlinkBlockHistoryDoNotGetProcessed() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoForOldOutlinkBlockHistoryTests(50000);

		// Act:
		final List<WeightedLink> actualLinks = info.getOutlinks();

		// Assert:
		MatcherAssert.assertThat(actualLinks.size(), IsEqual.equalTo(5));
	}

	private static PoiAccountInfo createAccountInfoForOldOutlinkBlockHistoryTests(final long accountInfoHeight) {
		// Arrange:
		// block heights must be in order so that account links have increasing block heights
		final int outlinkHistory = (int) OUTLINK_HISTORY;
		final int height = (int) accountInfoHeight;
		return createAccountInfoWithOutlinks(height, new int[]{
				1, 2, 3, 4, 5, 6, 7
		}, new int[]{
				height - outlinkHistory - 100, height - outlinkHistory - 1, height - outlinkHistory, 20000, 30000, 40000, 48998
		});
	}

	// endregion

	// region getNetOutlinks

	@Test
	public void netOutlinksAreEmptyWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Act:
		final List<WeightedLink> actualLinks = info.getNetOutlinks();

		// Assert:
		MatcherAssert.assertThat(actualLinks, IsEqual.equalTo(new ArrayList<>()));
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
		final List<WeightedLink> expectedLinks = Arrays.asList(new WeightedLink(Address.fromEncoded("acc 0"), 0.0), // 2 - 2
				new WeightedLink(Address.fromEncoded("acc 1"), 3.0e06), // 3
				new WeightedLink(Address.fromEncoded("acc 2"), -1.0e06), // 1 - 2
				new WeightedLink(Address.fromEncoded("acc 3"), 5.0e06), // 5
				new WeightedLink(Address.fromEncoded("acc 4"), 7.0e06)); // 9 -2
		MatcherAssert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	@Test
	public void netOutlinksAreCorrectWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(3 * 1440 + 2, new int[]{
				2, 6, 3, 1, 5, 8, 9, 11, 7
		}, new int[]{
				2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000
		});
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * THREE_DAY_DECAY));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 7.0e06 * TWO_DAY_DECAY));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 5"), 2.5e06));

		// Act:
		final List<WeightedLink> actualLinks = info.getNetOutlinks();

		// Assert:
		// - amounts at future block heights are not present (amounts 11 and 7)
		final List<WeightedLink> expectedLinks = Arrays.asList(new WeightedLink(Address.fromEncoded("acc 0"), 0.0), // 2 - 2
				new WeightedLink(Address.fromEncoded("acc 1"), 6.0e06 * TWO_DAY_DECAY), // 6
				new WeightedLink(Address.fromEncoded("acc 2"), -4.0e06 * TWO_DAY_DECAY), // 3 - 7
				new WeightedLink(Address.fromEncoded("acc 3"), 1.0e06 * ONE_DAY_DECAY), // 1
				new WeightedLink(Address.fromEncoded("acc 4"), 5.0e06 * ONE_DAY_DECAY), // 5
				new WeightedLink(Address.fromEncoded("acc 5"), 5.5e06), // 8 - 2.5
				new WeightedLink(Address.fromEncoded("acc 6"), 9.0e06)); // 9
		MatcherAssert.assertThat(actualLinks, IsEquivalent.equivalentTo(expectedLinks));
	}

	// endregion

	// region getNetOutlinkScore

	@Test
	public void outlinkScoreIsZeroWhenAccountHasNoOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithNullOutlinks();

		// Assert:
		MatcherAssert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(0.0));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 4"), 2.0e06));

		// Assert: sum(2, 3, 1, 5, 9) - sum(2, 2, 2)
		MatcherAssert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(1.4e07));
	}

	@Test
	public void negativeOutlinkScoreIsComputedCorrectlyWhenAccountHasOutlinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutlinks(2, 3, 1, 5, 9);
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 20.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 2.0e06));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 4"), 2.0e06));

		// Assert: (sum(2, 3, 1, 5, 9) - sum(20, 2, 2))
		MatcherAssert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(-0.4e07));
	}

	@Test
	public void outlinkScoreIsComputedCorrectlyWhenAccountHasOutlinksWithVariableHeights() {
		// Arrange:
		// block heights must be in order so that account links have increasing block heights
		final PoiAccountInfo info = createAccountInfoWithOutlinks(4322, new int[]{
				2, 6, 3, 1, 5, 8, 9, 11, 7
		}, new int[]{
				2, 1441, 1442, 1443, 2882, 2883, 4322, 4323, 7000
		});
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 0"), 2.0e06 * THREE_DAY_DECAY));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 2"), 8.0e06 * TWO_DAY_DECAY));
		info.addInlink(new WeightedLink(Address.fromEncoded("acc 5"), 2.5e06));

		// Assert: sum(net out-links)
		// - amounts at future block heights are not present (amounts 11 and 7)
		final double expectedScore = 0 * THREE_DAY_DECAY // 2 - 2
				+ 6.0e06 * TWO_DAY_DECAY // 6
				- 5e06 * TWO_DAY_DECAY // 3 - 8
				+ 1.0e06 * ONE_DAY_DECAY // 1
				+ 5.0e06 * ONE_DAY_DECAY // 5
				+ 5.5e06 // 8 - 2.5
				+ 9.0e06; // 9
		MatcherAssert.assertThat(info.getNetOutlinkScore(), IsEqual.equalTo(expectedScore));
	}

	// endregion

	private static PoiAccountInfo createAccountInfoWithNullOutlinks() {
		return createAccountInfoWithOutlinks((List<AccountLink>) null);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final List<AccountLink> outlinks) {
		final BlockHeight height = BlockHeight.ONE;
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		addAllOutlinks(state, outlinks);
		return new PoiAccountInfo(11, state, height);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final int... amounts) {
		final int[] heights = new int[amounts.length];
		Arrays.fill(heights, 1);
		return createAccountInfoWithOutlinks(1, amounts, heights);
	}

	private static PoiAccountInfo createAccountInfoWithOutlinks(final int referenceHeight, final int[] amounts, final int[] heights) {
		if (amounts.length != heights.length) {
			throw new IllegalArgumentException("amounts and heights must have same length");
		}

		final List<AccountLink> outlinks = new ArrayList<>();
		for (int i = 0; i < amounts.length; ++i) {
			final Account otherAccount = Mockito.mock(Account.class);
			Mockito.when(otherAccount.getAddress()).thenReturn(Address.fromEncoded(String.format("acc %d", i)));

			final AccountLink link = new AccountLink(new BlockHeight(heights[i]), Amount.fromNem(amounts[i]), otherAccount.getAddress());
			outlinks.add(link);
		}

		final AccountState state = new AccountState(Utils.generateRandomAddress());
		addAllOutlinks(state, outlinks);
		return new PoiAccountInfo(11, state, new BlockHeight(referenceHeight));
	}

	private static void addAllOutlinks(final AccountState state, final List<AccountLink> outlinks) {
		if (null == outlinks) {
			return;
		}

		for (final AccountLink link : outlinks) {
			state.getImportanceInfo().addOutlink(link);
		}
	}
}
