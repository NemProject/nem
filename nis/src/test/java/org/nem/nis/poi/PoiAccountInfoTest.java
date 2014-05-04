package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

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
		// Arrange
		final BlockHeight height = new BlockHeight(21);

		// Assert:
		Assert.assertThat(createAccountInfo(0, 0).canForage(height), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfo(0, 1).canForage(height), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfo(1, 0).canForage(height), IsEqual.equalTo(false));
		Assert.assertThat(createAccountInfo(1, 1).canForage(height), IsEqual.equalTo(true));
	}

	private static PoiAccountInfo createAccountInfo(int balance, int coinDays) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));

		// TODO: how can I set coin days?
		final AccountLink accountLink = new AccountLink();
//		account.setOutlinks();
//		account.getCoinDays().addCoinDay(new CoinDay(new BlockHeight(1200), Amount.fromNem(coinDays)));
//	 account.getCoinDays(.
		return new PoiAccountInfo(11, account);
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