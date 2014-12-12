package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;

import java.util.*;
import java.util.stream.*;

public class UnlockedAccountsTest {
	private static final int MAX_UNLOCKED_ACCOUNTS = 3;

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
	public void canUnlockForagingEligibleAccount() {
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
		final List<Account> accounts = context.createServerLimitPlusOneAccounts();

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
	public void canUnlockExactlyMaxUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createServerLimitPlusOneAccounts();

		// Assert: MAX_UNLOCKED_ACCOUNTS accounts can be unlocked
		for (int i = 0; i < MAX_UNLOCKED_ACCOUNTS; ++i) {
			Assert.assertThat(context.unlockedAccounts.addUnlockedAccount(accounts.get(i)), IsEqual.equalTo(UnlockResult.SUCCESS));
		}

		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(MAX_UNLOCKED_ACCOUNTS));
	}

	@Test
	public void cannotUnlockMoreThanMaxUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createServerLimitPlusOneAccounts();

		// - unlock MAX_UNLOCKED_ACCOUNTS accounts
		for (int i = 0; i < MAX_UNLOCKED_ACCOUNTS; ++i) {
			context.unlockedAccounts.addUnlockedAccount(accounts.get(i));
		}

		// Act: unlock account MAX_UNLOCKED_ACCOUNTS + 1
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(accounts.get(3));

		// Assert: the account was not unlocked
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(MAX_UNLOCKED_ACCOUNTS));
		Assert.assertThat(context.unlockedAccounts.isAccountUnlocked(accounts.get(3)), IsEqual.equalTo(false));
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_SERVER_LIMIT));
	}

	@Test
	public void canUnlockIfBelowServerLimit() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createServerLimitPlusOneAccounts();

		// - unlock MAX_UNLOCKED_ACCOUNTS accounts
		for (int i = 0; i < MAX_UNLOCKED_ACCOUNTS; ++i) {
			context.unlockedAccounts.addUnlockedAccount(accounts.get(i));
		}

		// unlock account MAX_UNLOCKED_ACCOUNTS + 1 (fails)
		context.unlockedAccounts.addUnlockedAccount(accounts.get(3));

		// Act: lock the second account and unlock MAX_UNLOCKED_ACCOUNTS + 1 account
		context.unlockedAccounts.removeUnlockedAccount(accounts.get(2));
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(accounts.get(3));

		// Assert: the account was unlocked
		Assert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(3));
		Assert.assertThat(context.unlockedAccounts.isAccountUnlocked(accounts.get(3)), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
	}

	//endregion

	private static class TestContext {
		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final AccountStateRepository accountStateRepository = Mockito.mock(PoiFacade.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final CanHarvestPredicate canHarvestPredicate = Mockito.mock(CanHarvestPredicate.class);
		private final UnlockedAccounts unlockedAccounts = new UnlockedAccounts(
				this.accountLookup,
				this.accountStateRepository,
				this.lastBlockLayer,
				this.canHarvestPredicate,
				MAX_UNLOCKED_ACCOUNTS);

		private void setKnownAddress(final Account account, final boolean isKnown) {
			Mockito.when(this.accountLookup.isKnownAddress(account.getAddress())).thenReturn(isKnown);
		}

		private void setCanForageAtHeight(final Account account, final long lastBlockHeight, final boolean canForage) {
			Mockito.when(this.lastBlockLayer.getLastBlockHeight()).thenReturn(lastBlockHeight);

			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getWeightedBalances().addFullyVested(new BlockHeight(lastBlockHeight), Amount.fromNem(canForage ? 10000 : 1));
			Mockito.when(this.accountStateRepository.findLatestForwardedStateByAddress(account.getAddress())).thenReturn(accountState);

			Mockito.when(this.canHarvestPredicate.canHarvest(accountState, new BlockHeight(lastBlockHeight))).thenReturn(canForage);
		}

		private List<Account> createServerLimitPlusOneAccounts() {
			final List<Account> accounts = new ArrayList<>();
			for (int i = 0; i < MAX_UNLOCKED_ACCOUNTS + 1; ++i) {
				final Account account = Utils.generateRandomAccount();
				this.setKnownAddress(account, true);
				this.setCanForageAtHeight(account, 17, true);
				accounts.add(account);
			}

			return accounts;
		}

		private void assertIsKnownAddressDelegation(final Account account) {
			Mockito.verify(this.accountLookup, Mockito.times(1)).isKnownAddress(account.getAddress());
		}

		private void assertCanForageDelegation(final Account account) {
			Mockito.verify(this.lastBlockLayer, Mockito.times(1)).getLastBlockHeight();
			Mockito.verify(this.accountStateRepository, Mockito.times(1)).findLatestForwardedStateByAddress(account.getAddress());
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