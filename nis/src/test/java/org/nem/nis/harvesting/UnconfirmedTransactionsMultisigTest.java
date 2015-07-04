package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

/**
 * Those test are similar to those that can be found in BlockChainServices.
 * The point is to have end-to-end tests, to make sure that:
 * a) BlockChainServices rejects invalid blocks (that could be generated with altered UT)
 * b) UT does not let generate invalid blocks (would be bad for harvesters)
 */
public class UnconfirmedTransactionsMultisigTest {
	final static TimeInstant CURRENT_TIME = new TimeInstant(10_000);
	final static TimeInstant EXPIRY_TIME = CURRENT_TIME.addSeconds(-BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME - 1);

	//region multisig signature

	@Test
	public void properSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = new MultisigSignatureTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransaction();
		final ValidationResult signatureResult = context.addSignatureTransaction(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void expiredSignatureIsNotAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = new MultisigSignatureTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(EXPIRY_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransaction();
		final ValidationResult signatureResult = context.addSignatureTransaction(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(signatureResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void multisigTransactionWithSignatureIsAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = new MultisigSignatureTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionWithExpiredSignatureIsNotAccepted() {
		// Arrange:
		final MultisigSignatureTestContext context = new MultisigSignatureTestContext();
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(EXPIRY_TIME);

		// Act:
		final ValidationResult multisigResult = context.addMultisigTransactionWithSignature(signature);

		// Assert:
		Assert.assertThat(multisigResult, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	//region dropExpiredTransactions

	@Test
	public void dropExpiredTransactionsDropsMultisigTransactionWithExpiredSignature() {
		this.assertTransactionIsDropped(CURRENT_TIME.addHours(1), CURRENT_TIME.addMinutes(1));
	}

	@Test
	public void dropExpiredTransactionsDropsMultisigTransactionWithExpiredInnerTransaction() {
		this.assertTransactionIsDropped(CURRENT_TIME.addMinutes(1), CURRENT_TIME.addHours(1));
	}

	private void assertTransactionIsDropped(final TimeInstant innerTransactionDeadline, final TimeInstant signatureDeadline) {
		// Arrange:
		final MultisigSignatureTestContext context = new MultisigSignatureTestContext();
		context.t1.setDeadline(innerTransactionDeadline);
		final MultisigTransaction multisigTransaction = context.context.createMultisigTransaction(CURRENT_TIME, context.t1);
		final MultisigSignatureTransaction signature = context.createSignatureTransaction(CURRENT_TIME);
		signature.setDeadline(signatureDeadline);
		signature.sign();
		multisigTransaction.addSignature(signature);
		Assert.assertThat(context.context.transactions.addExisting(multisigTransaction), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		context.context.transactions.dropExpiredTransactions(CURRENT_TIME.addMinutes(5));

		// Assert:
		Assert.assertThat(context.context.transactions.size(), IsEqual.equalTo(0));
	}

	//endregion

	private static class MultisigSignatureTestContext {
		private final TestContext context = new TestContext();
		private final Transaction t1;
		private final MultisigTransaction multisigTransaction;

		public MultisigSignatureTestContext() {
			this.context.makeCosignatory(this.context.cosigner1, this.context.multisig);
			this.context.makeCosignatory(this.context.cosigner2, this.context.multisig);

			this.context.setBalance(this.context.multisig, Amount.fromNem(121));
			this.context.setBalance(this.context.cosigner1, Amount.ZERO);
			this.context.setBalance(this.context.cosigner2, Amount.ZERO);

			this.t1 = this.context.createTransferTransaction(CURRENT_TIME, Amount.fromNem(7));
			this.multisigTransaction = this.context.createMultisigTransaction(CURRENT_TIME, this.t1);
		}

		public MultisigSignatureTransaction createSignatureTransaction(final TimeInstant signatureTime) {
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
					signatureTime,
					this.context.cosigner2,
					this.context.multisig,
					this.t1);
			signature.setDeadline(signature.getTimeStamp().addSeconds(1));
			signature.sign();
			return signature;
		}

		public ValidationResult addMultisigTransaction() {
			return this.context.transactions.addExisting(this.multisigTransaction);
		}

		public ValidationResult addMultisigTransactionWithSignature(final MultisigSignatureTransaction signatureTransaction) {
			this.multisigTransaction.addSignature(signatureTransaction);
			return this.context.transactions.addExisting(this.multisigTransaction);
		}

		public ValidationResult addSignatureTransaction(final MultisigSignatureTransaction signatureTransaction) {
			return this.context.transactions.addExisting(signatureTransaction);
		}
	}

	//endregion

	private static class TestContext {
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory(this.timeProvider);
		private final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
		private final ReadOnlyNamespaceCache namespaceCache = Mockito.mock(ReadOnlyNamespaceCache.class);
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;
		private final ReadOnlyAccountStateCache accountStateCache;

		private final Account multisig = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();

		private TestContext() {
			this.batchValidator = this.factory.createBatch(Mockito.mock(DefaultHashCache.class));
			this.accountStateCache = this.stateCache;
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);

			// need to actually create for every invocation and not create once
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
			Mockito.when(this.nisCache.getNamespaceCache()).thenReturn(this.namespaceCache);
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any()))
							.then((invocationOnMock) -> this.factory.createSingleBuilder(this.nisCache));
			Mockito.when(validatorFactory.createIncompleteSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> this.factory.createIncompleteSingleBuilder(this.nisCache));

			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(CURRENT_TIME);

			this.addState(this.multisig);
			this.addState(this.recipient);
			this.addState(this.cosigner1);
			this.addState(this.cosigner2);

			this.transactions = new UnconfirmedTransactions(
					validatorFactory,
					NisCacheFactory.createReadOnly(this.accountStateCache, transactionHashCache),
					this.timeProvider,
					() -> BlockHeight.MAX.prev());
		}

		public MultisigTransaction createMultisigTransaction(final TimeInstant currentTime, final Transaction t1) {
			final MultisigTransaction transaction = new MultisigTransaction(currentTime, this.cosigner1, t1);
			transaction.setDeadline(transaction.getTimeStamp().addHours(2));
			transaction.sign();
			return transaction;
		}

		public TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Amount amount) {
			final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, this.multisig, this.recipient, amount, null);
			transferTransaction.setDeadline(timeStamp.addSeconds(1));
			return transferTransaction;
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			this.stateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			this.stateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress());
		}

		private AccountState addState(final Account account) {
			final Address address = account.getAddress();
			final AccountState state = new AccountState(address);
			Mockito.when(this.stateCache.findStateByAddress(address)).thenReturn(state);
			return state;
		}

		public void setBalance(final Account multisig, final Amount amount) {
			this.stateCache.findStateByAddress(multisig.getAddress()).getAccountInfo().incrementBalance(amount);
		}
	}
}
