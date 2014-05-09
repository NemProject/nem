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
		final Account account = Utils.generateRandomAccount();
		final PoiAccountInfo info = new PoiAccountInfo(17, account);

		// Assert:
		Assert.assertThat(info.getIndex(), IsEqual.equalTo(17));
		Assert.assertThat(info.getAccount(), IsSame.sameInstance(account));
	}

	@Test
	public void foragingRequiresPositiveBalanceAndPositiveCoinDays() {
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
		account.setCoinDaysAt(Amount.fromNem(coinDays), height);

		// Act:
		return new PoiAccountInfo(11, account).canForage(height);
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
	public void outLinkWeightsAreNormalizedInOrderWhenAccountHasOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutLinks(2, 3, 1, 5, 9);

		// Assert:
		Assert.assertThat(
				info.getOutLinkWeights(),
				IsEqual.equalTo(new ColumnVector(0.10, 0.15, 0.05, 0.25, 0.45)));
	}

	@Test
	public void outLinkScoreIsComputedCorrectlyWhenAccountHasOutLinks() {
		// Arrange:
		final PoiAccountInfo info = createAccountInfoWithOutLinks(2, 3, 1, 5, 9);

		// Assert: (normalized median * num out-links)
		Assert.assertThat(info.getOutLinkScore(), IsEqual.equalTo(15.0));
	}

	private static PoiAccountInfo createAccountInfoWithNullOutLinks() {
		return createAccountInfoWithOutLinks((List<AccountLink>)null);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final List<AccountLink> outLinks) {
		final Account account = Utils.generateRandomAccount();
		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final int... strengths) {
		final Account account = Utils.generateRandomAccount();

		final List<AccountLink> outLinks = new ArrayList<>();
		for (int strength : strengths) {
			final AccountLink link = new AccountLink();
			link.setStrength(strength);
			outLinks.add(link);
		}

		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account);
	}
}