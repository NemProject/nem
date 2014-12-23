package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.DebitPredicate;

public class DefaultDebitPredicateTest {

	@Test
	public void getDebitPredicateEvaluatesAmountAgainstBalancesInAccountState() {
		// Arrange:
		final AccountStateCache accountStateCache = new DefaultAccountStateCache().asAutoCache();
		final Account account1 = addAccountWithBalance(accountStateCache, Amount.fromNem(10));
		final Account account2 = addAccountWithBalance(accountStateCache, Amount.fromNem(77));

		// Act:
		final DebitPredicate debitPredicate = new DefaultDebitPredicate(accountStateCache);

		// Assert:
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(9)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(10)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(11)), IsEqual.equalTo(false));

		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(76)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(77)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(78)), IsEqual.equalTo(false));
	}

	private static Account addAccountWithBalance(final AccountStateCache accountStateCache, final Amount amount) {
		final Account account = Utils.generateRandomAccount();
		final AccountState accountState = accountStateCache.findStateByAddress(account.getAddress());
		accountState.getAccountInfo().incrementBalance(amount);
		return account;
	}
}