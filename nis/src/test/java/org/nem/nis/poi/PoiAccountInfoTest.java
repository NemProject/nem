package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
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
	public void hasOutLinksIsOnlyTrueWhenAnAccountHasAtLeastOneOutLink() {
		// Assert:
		Assert.assertThat(createAccountInfoWithOutLinks(null).hasOutLinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutLinks(0).hasOutLinks(), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfoWithOutLinks(1).hasOutLinks(), IsEqual.equalTo(true));
		Assert.assertThat(createAccountInfoWithOutLinks(2).hasOutLinks(), IsEqual.equalTo(true));
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final List<AccountLink> outLinks) {
		final Account account = Utils.generateRandomAccount();
		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account);
	}

	private static PoiAccountInfo createAccountInfoWithOutLinks(final int numOutLinks) {
		final Account account = Utils.generateRandomAccount();

		final List<AccountLink> outLinks = new ArrayList<>();
		for (int i = 0; i < numOutLinks; ++i)
			outLinks.add(new AccountLink());

		account.setOutlinks(outLinks);
		return new PoiAccountInfo(11, account);
	}
}