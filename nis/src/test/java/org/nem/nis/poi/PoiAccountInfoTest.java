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
	public void foragingRequiresPositiveBalanceAndPositiveVestedBalance() {
		// Assert:
		Assert.assertThat(canForage(0, 0), IsEqual.equalTo(false));
		Assert.assertThat(canForage(0, 1), IsEqual.equalTo(false));
		Assert.assertThat(canForage(1, 0), IsEqual.equalTo(false));
		Assert.assertThat(canForage(1, 1), IsEqual.equalTo(true));
	}

	private static boolean canForage(int balance, int coinDays) {
		// Arrange:
		final BlockHeight height = new BlockHeight(33);
		final MockAccount account = new MockAccount();
		account.incrementBalance(Amount.fromNem(balance));
		account.setVestedBalanceAt(Amount.fromNem(coinDays), height);

		// Act:
		return new PoiAccountInfo(11, account, height).canForage(height);
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
		final PoiAccountInfo info2 = new PoiAccountInfo(info.getIndex(), info.getAccount(), new BlockHeight(2881));

		// Assert: (median * num out-links)
		Assert.assertThat(info.getOutLinkScore(), IsEqual.equalTo(15.0e06));
		Assert.assertThat(info2.getOutLinkScore(), IsEqual.equalTo(15.0e06 * PoiAccountInfo.DECAY_BASE * PoiAccountInfo.DECAY_BASE));
	}

	private static PoiAccountInfo createAccountInfoWithNullOutLinks() {
		return createAccountInfoWithOutLinks((List<AccountLink>)null);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final List<AccountLink> outLinks) {
		final BlockHeight height = BlockHeight.ONE;
		final Account account = Utils.generateRandomAccount();
		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account, height);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final int... amounts) {
		final Account account = Utils.generateRandomAccount();

		final List<AccountLink> outLinks = new ArrayList<>();
		final BlockHeight height = BlockHeight.ONE;
		for (int amount : amounts) {
			final AccountLink link = new AccountLink(height, Amount.fromNem(amount), account);
			outLinks.add(link);
		}

		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account, height);
	}
}