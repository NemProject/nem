package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.DebitPredicate;

@SuppressWarnings("rawtypes")
public class DefaultXemDebitPredicateTest {

	@Test
	public void canDebitEvaluatesAmountAgainstBalancesInAccountState() {
		// Arrange:
		final ExtendedAccountStateCache accountStateCache = new DefaultAccountStateCache().copy();
		final Account account1 = addAccountWithBalance(accountStateCache, Amount.fromNem(10));
		final Account account2 = addAccountWithBalance(accountStateCache, Amount.fromNem(77));
		accountStateCache.commit();

		// Act:
		final DebitPredicate<Amount> debitPredicate = new DefaultXemDebitPredicate(accountStateCache);

		// Assert:
		MatcherAssert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(9)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(10)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(11)), IsEqual.equalTo(false));

		MatcherAssert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(76)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(77)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(78)), IsEqual.equalTo(false));
	}

	@Test
	public void canDebitReturnsCorrectResultWhenAccountBalanceIsZero() {
		// Arrange:
		final ExtendedAccountStateCache accountStateCache = new DefaultAccountStateCache().copy();
		final Account account1 = addAccountWithBalance(accountStateCache, Amount.ZERO);
		accountStateCache.commit();

		// Act:
		final DebitPredicate<Amount> debitPredicate = new DefaultXemDebitPredicate(accountStateCache);

		// Assert:
		MatcherAssert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(0)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(1)), IsEqual.equalTo(false));
	}

	private static Account addAccountWithBalance(final AccountStateCache accountStateCache, final Amount amount) {
		final Account account = Utils.generateRandomAccount();
		final AccountState accountState = accountStateCache.findStateByAddress(account.getAddress());
		accountState.getAccountInfo().incrementBalance(amount);
		return account;
	}
}
