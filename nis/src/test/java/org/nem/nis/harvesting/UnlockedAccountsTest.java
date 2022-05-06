package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;

import java.util.*;
import java.util.stream.*;

public class UnlockedAccountsTest {
	private static final int MAX_UNLOCKED_ACCOUNTS = 3;
	private static final BlockHeight DEFAULT_HEIGHT = new BlockHeight(17);

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
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_UNKNOWN_ACCOUNT));
		context.assertAccountIsLocked(account);
		context.assertIsKnownAddressDelegation(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void cannotUnlockForagingIneligibleAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanHarvestAtHeight(account, DEFAULT_HEIGHT, false);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_HARVESTING_INELIGIBLE));
		context.assertAccountIsLocked(account);
		context.assertIsKnownAddressDelegation(account);
		context.assertCanForageDelegation(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canUnlockForagingEligibleAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanHarvestAtHeight(account, DEFAULT_HEIGHT, true);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
		context.assertAccountIsUnlocked(account);
		context.assertIsKnownAddressDelegation(account);
		context.assertCanForageDelegation(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(1));
	}

	@Test
	public void cannotUnlockBlockedHarvestingAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = new Account(
				new KeyPair(PublicKey.fromHexString("b74e3914b13cb742dfbceef110d85bad14bd3bb77051a08be93c0f8a0651fde2")));
		context.setKnownAddress(account, true);
		context.setCanHarvestAtHeight(account, DEFAULT_HEIGHT, true);

		// Act:
		final UnlockResult result = context.unlockedAccounts.addUnlockedAccount(account);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_HARVESTING_BLOCKED));
		context.assertAccountIsLocked(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region removeUnlockedAccount

	@Test
	public void canLockUnlockedAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanHarvestAtHeight(account, DEFAULT_HEIGHT, true);

		// Act:
		context.unlockedAccounts.addUnlockedAccount(account);

		// Act:
		context.unlockedAccounts.removeUnlockedAccount(account);

		// Assert:
		context.assertAccountIsLocked(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	@Test
	public void lockingLockedAccountHasNoEffect() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.setKnownAddress(account, true);
		context.setCanHarvestAtHeight(account, DEFAULT_HEIGHT, true);

		// Act:
		context.unlockedAccounts.removeUnlockedAccount(account);

		// Assert:
		context.assertAccountIsLocked(account);
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region iterator

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
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(StreamSupport.stream(context.unlockedAccounts.spliterator(), false).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(Arrays.asList(accounts.get(0), accounts.get(2))));
	}

	// endregion

	// region unlockedLimit

	@Test
	public void canUnlockExactlyMaxUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createServerLimitPlusOneAccounts();

		// Assert: MAX_UNLOCKED_ACCOUNTS accounts can be unlocked
		for (int i = 0; i < MAX_UNLOCKED_ACCOUNTS; ++i) {
			MatcherAssert.assertThat(context.unlockedAccounts.addUnlockedAccount(accounts.get(i)), IsEqual.equalTo(UnlockResult.SUCCESS));
		}

		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(MAX_UNLOCKED_ACCOUNTS));
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
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(MAX_UNLOCKED_ACCOUNTS));
		MatcherAssert.assertThat(context.unlockedAccounts.isAccountUnlocked(accounts.get(3)), IsEqual.equalTo(false));
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.FAILURE_SERVER_LIMIT));
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
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(context.unlockedAccounts.isAccountUnlocked(accounts.get(3)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result, IsEqual.equalTo(UnlockResult.SUCCESS));
	}

	// endregion

	// region maxSize

	@Test
	public void maxSizeIsEqualToMaxUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final int maxSize = context.unlockedAccounts.maxSize();

		// Assert:
		MatcherAssert.assertThat(maxSize, IsEqual.equalTo(MAX_UNLOCKED_ACCOUNTS));
	}

	// endregion

	// region prune

	@Test
	public void pruneRemovesNoAccountIfAllAccountsAreEligibleToHarvest() {
		// Arrange:
		final TestContext context = new TestContext();
		final Collection<Account> accounts = context.createHarvestingEligibleAccountsAtHeight(DEFAULT_HEIGHT, MAX_UNLOCKED_ACCOUNTS);
		accounts.forEach(context.unlockedAccounts::addUnlockedAccount);

		// Sanity:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size()));

		// Act:
		context.unlockedAccounts.prune(DEFAULT_HEIGHT);

		// Assert:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size()));
		accounts.forEach(context::assertAccountIsUnlocked);
	}

	@Test
	public void pruneRemovesAccountIfHarvestingPredicateReturnsFalse() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createHarvestingEligibleAccountsAtHeight(DEFAULT_HEIGHT, MAX_UNLOCKED_ACCOUNTS);
		accounts.forEach(context.unlockedAccounts::addUnlockedAccount);
		final Account accountToRemove = accounts.get(1);
		context.setCanHarvestPredicateAtHeight(accountToRemove, DEFAULT_HEIGHT, false);

		// Sanity:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size()));

		// Act:
		context.unlockedAccounts.prune(DEFAULT_HEIGHT);

		// Assert:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size() - 1));
		context.assertAccountIsLocked(accountToRemove);
	}

	private static void assertPruning(final int pruneCount) {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Account> accounts = context.createHarvestingEligibleAccountsAtHeight(DEFAULT_HEIGHT, MAX_UNLOCKED_ACCOUNTS);
		accounts.forEach(context.unlockedAccounts::addUnlockedAccount);
		final Account accountToRemove = accounts.get(1);
		context.setKnownAddress(accountToRemove, false);

		// Sanity:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size()));

		// Act:
		for (int i = 0; i < pruneCount; ++i) {
			context.unlockedAccounts.prune(DEFAULT_HEIGHT);
		}

		// Assert:
		MatcherAssert.assertThat(context.unlockedAccounts.size(), IsEqual.equalTo(accounts.size() - 1));
		context.assertAccountIsLocked(accountToRemove);
	}

	@Test
	public void pruneRemovesAccountIfAccountIsUnknown() {
		// Assert:
		assertPruning(1);
	}

	@Test
	public void pruneIsIdempotent() {
		// Assert:
		assertPruning(10);
	}

	// endregion

	private static class TestContext {
		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final CanHarvestPredicate canHarvestPredicate = Mockito.mock(CanHarvestPredicate.class);
		private final UnlockedAccounts unlockedAccounts = new UnlockedAccounts(this.accountLookup, this.accountStateCache,
				this.lastBlockLayer, this.canHarvestPredicate, MAX_UNLOCKED_ACCOUNTS);

		private void setKnownAddress(final Account account, final boolean isKnown) {
			Mockito.when(this.accountLookup.isKnownAddress(account.getAddress())).thenReturn(isKnown);
		}

		private void setCanHarvestAtHeight(final Account account, final BlockHeight height, final boolean canHarvest) {
			Mockito.when(this.lastBlockLayer.getLastBlockHeight()).thenReturn(height);

			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getWeightedBalances().addFullyVested(height, Amount.fromNem(canHarvest ? 10000 : 1));
			Mockito.when(this.accountStateCache.findLatestForwardedStateByAddress(account.getAddress())).thenReturn(accountState);

			Mockito.when(this.canHarvestPredicate.canHarvest(accountState, height)).thenReturn(canHarvest);
		}

		private void setCanHarvestPredicateAtHeight(final Account account, final BlockHeight height, final boolean canHarvest) {
			final AccountState accountState = this.accountStateCache.findLatestForwardedStateByAddress(account.getAddress());
			Mockito.when(this.canHarvestPredicate.canHarvest(accountState, height)).thenReturn(canHarvest);
		}

		private List<Account> createHarvestingEligibleAccountsAtHeight(final BlockHeight height, final int count) {
			final List<Account> accounts = new ArrayList<>();
			for (int i = 0; i < count; ++i) {
				final Account account = Utils.generateRandomAccount();
				this.setKnownAddress(account, true);
				this.setCanHarvestAtHeight(account, height, true);
				accounts.add(account);
			}

			return accounts;
		}

		private List<Account> createServerLimitPlusOneAccounts() {
			return createHarvestingEligibleAccountsAtHeight(DEFAULT_HEIGHT, MAX_UNLOCKED_ACCOUNTS + 1);
		}

		private void assertIsKnownAddressDelegation(final Account account) {
			Mockito.verify(this.accountLookup, Mockito.times(1)).isKnownAddress(account.getAddress());
		}

		private void assertCanForageDelegation(final Account account) {
			Mockito.verify(this.lastBlockLayer, Mockito.times(1)).getLastBlockHeight();
			Mockito.verify(this.accountStateCache, Mockito.times(1)).findLatestForwardedStateByAddress(account.getAddress());
		}

		private void assertAccountIsLocked(final Account account) {
			MatcherAssert.assertThat(this.unlockedAccounts.isAccountUnlocked(account), IsEqual.equalTo(false));
			MatcherAssert.assertThat(this.unlockedAccounts.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(false));
		}

		private void assertAccountIsUnlocked(final Account account) {
			MatcherAssert.assertThat(this.unlockedAccounts.isAccountUnlocked(account), IsEqual.equalTo(true));
			MatcherAssert.assertThat(this.unlockedAccounts.isAccountUnlocked(account.getAddress()), IsEqual.equalTo(true));
		}
	}
}
