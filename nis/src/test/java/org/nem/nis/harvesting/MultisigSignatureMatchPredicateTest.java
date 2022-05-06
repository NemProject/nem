package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.AccountState;

import java.util.Arrays;

public class MultisigSignatureMatchPredicateTest {

	@Test
	public void isNotMatchIfSignatureAndMultisigTransactionsHaveSameSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner1);

		// Act:
		final boolean isMatch = context.predicate.isMatch(signatureTransaction, multisigTransaction);

		// Assert:
		MatcherAssert.assertThat(isMatch, IsEqual.equalTo(false));
	}

	@Test
	public void isNotMatchIfSignatureAndMultisigTransactionsHaveDifferentOtherTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1,
				Utils.generateRandomHash());
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);

		// Act:
		final boolean isMatch = context.predicate.isMatch(signatureTransaction, multisigTransaction);

		// Assert:
		MatcherAssert.assertThat(isMatch, IsEqual.equalTo(false));
	}

	@Test
	public void isNotMatchIfSignatureAndMultisigTransactionsHaveDifferentMultisigAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransactionWithMultisig(context.cosigner1,
				Utils.generateRandomAccount());
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);

		// Act:
		final boolean isMatch = context.predicate.isMatch(signatureTransaction, multisigTransaction);

		// Assert:
		MatcherAssert.assertThat(isMatch, IsEqual.equalTo(false));
	}

	@Test
	public void isNotMatchIfSignatureIsSignedByNonCosigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(Utils.generateRandomAccount());
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);

		// Act:
		final boolean isMatch = context.predicate.isMatch(signatureTransaction, multisigTransaction);

		// Assert:
		MatcherAssert.assertThat(isMatch, IsEqual.equalTo(false));
	}

	@Test
	public void isMatchIfSignatureAndMultisigTransactionsReferenceSameOtherTransactionAndAreSignedByDifferentCosigners() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);

		// Act:
		final boolean isMatch = context.predicate.isMatch(signatureTransaction, multisigTransaction);

		// Assert:
		MatcherAssert.assertThat(isMatch, IsEqual.equalTo(true));
	}

	private static class TestContext {
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final MultisigSignatureMatchPredicate predicate = new MultisigSignatureMatchPredicate(this.accountStateCache);

		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();
		private final Account multisig = Utils.generateRandomAccount();
		private final Transaction otherTransaction = new MockTransaction(this.multisig);
		private final Hash otherTransactionHash = HashUtils.calculateHash(this.otherTransaction);

		public TestContext() {
			Mockito.when(this.accountStateCache.findStateByAddress(Mockito.any()))
					.thenAnswer(invocationOnMock -> new AccountState((Address) invocationOnMock.getArguments()[0]));
			for (final Account cosigner : Arrays.asList(this.cosigner1, this.cosigner2)) {
				final AccountState cosignerAccountState = new AccountState(cosigner.getAddress());
				cosignerAccountState.getMultisigLinks().addCosignatoryOf(this.multisig.getAddress());
				Mockito.when(this.accountStateCache.findStateByAddress(cosigner.getAddress())).thenReturn(cosignerAccountState);
			}
		}

		public MultisigSignatureTransaction createSignatureTransaction(final Account signer) {
			return this.createSignatureTransaction(signer, this.otherTransactionHash);
		}

		public MultisigSignatureTransaction createSignatureTransactionWithMultisig(final Account signer, final Account multisig) {
			return new MultisigSignatureTransaction(TimeInstant.ZERO, signer, multisig, this.otherTransactionHash);
		}

		public MultisigSignatureTransaction createSignatureTransaction(final Account signer, final Hash hash) {
			return new MultisigSignatureTransaction(TimeInstant.ZERO, signer, this.multisig, hash);
		}

		public MultisigTransaction createMultisigTransaction(final Account signer) {
			return new MultisigTransaction(TimeInstant.ZERO, signer, this.otherTransaction);
		}
	}
}
