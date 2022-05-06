package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.AccountState;

import java.util.Collections;
import java.util.function.*;

public class ImpactfulTransactionPredicateTest {

	// region non-multisig

	@Test
	public void transactionSignerIsImpacted() {
		// Assert:
		assertPredicateResult(context -> context.sender, true);
	}

	@Test
	public void transactionOtherAccountIsImpacted() {
		// Assert:
		assertPredicateResult(context -> context.otherAccount, true);
	}

	@Test
	public void nonTransactionAccountIsNotImpacted() {
		// Assert:
		assertPredicateResult(context -> Utils.generateRandomAccount(), false);
	}

	private static void assertPredicateResult(final Function<TestContext, Account> getAccount, final boolean expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.test(getAccount.apply(context));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region multisig

	@Test
	public void cosignerOfMultisigIsImpacted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final AccountState cosignerAccountState = new AccountState(cosigner.getAddress());
		cosignerAccountState.getMultisigLinks().addCosignatory(multisig.getAddress());
		Mockito.when(context.accountStateCache.findStateByAddress(cosigner.getAddress())).thenReturn(cosignerAccountState);

		final MultisigTransaction transaction = RandomTransactionFactory.createMultisigTransfer(multisig, cosigner);

		// Act:
		final boolean result = context.predicate.test(cosigner.getAddress(), transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void nonCosignerOfMultisigIsNotImpacted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final AccountState cosignerAccountState = new AccountState(cosigner.getAddress());
		cosignerAccountState.getMultisigLinks().addCosignatory(multisig.getAddress());
		Mockito.when(context.accountStateCache.findStateByAddress(cosigner.getAddress())).thenReturn(cosignerAccountState);

		final Account nonCosigner = Utils.generateRandomAccount();
		Mockito.when(context.accountStateCache.findStateByAddress(nonCosigner.getAddress()))
				.thenReturn(new AccountState(nonCosigner.getAddress()));

		final MultisigTransaction transaction = RandomTransactionFactory.createMultisigTransfer(multisig, cosigner);

		// Act:
		final boolean result = context.predicate.test(nonCosigner.getAddress(), transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
	}

	// endregion

	private static class TestContext {
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final BiPredicate<Address, Transaction> predicate = new ImpactfulTransactionPredicate(this.accountStateCache);

		private final Account sender = Utils.generateRandomAccount();
		private final Account otherAccount = Utils.generateRandomAccount();
		private final MockTransaction transaction = new MockTransaction(this.sender, 7);

		public TestContext() {
			this.transaction.setOtherAccounts(Collections.singletonList(this.otherAccount));
		}

		public boolean test(final Account account) {
			return this.predicate.test(account.getAddress(), this.transaction);
		}
	}
}
