package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.*;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;

import java.util.*;
import java.util.stream.*;

public class UnlockedAccountsTest {

	// region addUnlockedAccount

	@Test
	public void cannotUnlockUnknownAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, false);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_UNKNOWN_ACCOUNT));
		context.assertAccountIsLocked(account);
		context.assertIsKnownAddressDelegation(account);
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void cannotUnlockForagingIneligibleAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanForageAtHeight(account, 17, false);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_FORAGING_INELIGIBLE));
		context.assertAccountIsLocked(account);
		context.assertIsKnownAddressDelegation(account);
		context.assertCanForageDelegation(account);
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void cannotUnlockForagingEligibleAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanForageAtHeight(account, 17, true);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
		context.assertAccountIsUnlocked(account);
		context.assertIsKnownAddressDelegation(account);
		context.assertCanForageDelegation(account);
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(1));
	}

	//endregion

	//region removeUnlockedAccount

	@Test
	public void canLockUnlockedAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanForageAtHeight(account, 17, true);

		// Act:
		context.unlockedAccounts.addUnlockedAccount(account);

		// Act:
		context.unlockedAccounts.removeUnlockedAccount(account);

		// Assert:
		context.assertAccountIsLocked(account);
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void lockingLockedAccountHasNoEffect() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanForageAtHeight(account, 17, true);

		// Act:
		context.unlockedAccounts.removeUnlockedAccount(account);

		// Assert:
		context.assertAccountIsLocked(account);
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	//endregion

	//region iterator
	@Test
	public void canIterateOverAllUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			final Account account = Utils.generateRandomAccount();
			context.setKnownAddress(account, true);
			context.setCanForageAtHeight(account, 17, true);
			accounts.add(account);
		}

		// Act: unlock three accounts and then lock the second one
		context.unlockedAccounts.addUnlockedAccount(accounts.get(0));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(1));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(2));
		context.unlockedAccounts.removeUnlockedAccount(accounts.get(1));

		// Assert:
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(2));
		Assert.assertThat(
				StreamSupport.stream(context.unlockedAccounts.spliterator(), false).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(Arrays.asList(accounts.get(0), accounts.get(2))));
	}

	//endregion

	//region unlockedLimit
	@Test
	public void cannotUnlockMoreThanLimitAccounts() {
		final TestContext context = new TestContext();
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			final Account account = Utils.generateRandomAccount();
			context.setKnownAddress(account, true);
			context.setCanForageAtHeight(account, 17, true);
			accounts.add(account);
		}

		// TODO 20141005 J-G comment is wrong :)
		// > while i am usually against combining Act and Assert
		// > there might be some benefit to validating the result of every addUnlockedAccount
		// Act: unlock three accounts and then lock the second one
		context.unlockedAccounts.addUnlockedAccount(accounts.get(0));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(1));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(2));
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(accounts.get(3));

		// Assert:
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(3));
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_SERVER_LIMIT));
	}

	@Test
	public void canUnlockIfBelowServerLimit() {
		final TestContext context = new TestContext();
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			final Account account = Utils.generateRandomAccount();
			context.setKnownAddress(account, true);
			context.setCanForageAtHeight(account, 17, true);
			accounts.add(account);
		}

		// Act: unlock three accounts and then lock the second one
		context.unlockedAccounts.addUnlockedAccount(accounts.get(0));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(1));
		context.unlockedAccounts.addUnlockedAccount(accounts.get(2));

		// this should fail
		context.unlockedAccounts.addUnlockedAccount(accounts.get(3));
		context.unlockedAccounts.removeUnlockedAccount(accounts.get(2));
		// this should succeed
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(accounts.get(3));

		// Assert:
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(3));
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
	}
	//endregion

	private static class TestContext {
		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);
		private final UnlockedAccounts unlockedAccounts = new UnlockedAccounts(this.accountLookup, this.poiFacade, this.lastBlockLayer, this.nisConfiguration);

		public TestContext() {
			// TODO 20141005 J-G can you use a class constant for 3 e.g. TEST_SERVER_LIMIT ... i think it will make the tests a little clearer
			Mockito.when(this.nisConfiguration.getUnlockedLimit()).thenReturn(3);
		}

		private void setKnownAddress(final Account account, final boolean isKnown) {
			Mockito.when(this.accountLookup.isKnownAddress(account.getAddress())).thenReturn(isKnown);
		}

		private void setCanForageAtHeight(final Account account, final long lastBlockHeight, final boolean canForage) {
			Mockito.when(this.lastBlockLayer.getLastBlockHeight()).thenReturn(lastBlockHeight);

			final PoiAccountState accountState = new PoiAccountState(account.getAddress());
			accountState.getWeightedBalances().addFullyVested(new BlockHeight(lastBlockHeight), Amount.fromNem(canForage ? 10000 : 1));
			Mockito.when(this.poiFacade.findLatestForwardedStateByAddress(account.getAddress())).thenReturn(accountState);
		}

		private void assertIsKnownAddressDelegation(final Account account) {
			Mockito.verify(this.accountLookup, Mockito.times(1)).isKnownAddress(account.getAddress());
		}

		private void assertCanForageDelegation(final Account account) {
			Mockito.verify(this.lastBlockLayer, Mockito.times(1)).getLastBlockHeight();
			Mockito.verify(this.poiFacade, Mockito.times(1)).findLatestForwardedStateByAddress(account.getAddress());
		}

		private void assertAccountIsLocked(final Account account) {
			Assert.assertThat(this.unlockedAccounts.isAccountUnlocked(account), IsEqual.equalTo(false));
			Assert.assertThat(this.unlockedAccounts.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(false));
		}

		private void assertAccountIsUnlocked(final Account account) {
			Assert.assertThat(this.unlockedAccounts.isAccountUnlocked(account), IsEqual.equalTo(true));
			Assert.assertThat(this.unlockedAccounts.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(true));
		}
	}
}