package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.time.TimeProvider;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.cache.DefaultHashCache;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisCacheFactory;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BatchTransactionValidator;
import org.nem.nis.validators.SingleTransactionValidator;
import org.nem.nis.validators.TransactionValidatorFactory;

public class UnconfirmedTransactionsMultisigTest {
	@Test
	public void getTransactionsForNewBlockDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);
		multisigTransaction.setDeadline(multisigTransaction.getTimeStamp().addHours(2));
		multisigTransaction.sign();

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));
		context.makeCosignatory(context.cosigner1, context.multisig);
		context.makeCosignatory(context.cosigner2, context.multisig);

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.transactions.getTransactionsForNewBlock(Utils.generateRandomAddress(),
				currentTime.addMinutes(10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(0));
	}

	// note: this test actually tests few things, which means it's most likely ugly
	// * adding multisig signature adds it to PRESENT MultisigTransaction
	// * multisig signatures are NOT returned by getTransactionsForNewBlock
	// * multisig transaction is returned only if (number of cosignatories-1) == number of sigs
	@Test
	public void addingMultisigSignatureAddsItToMultisigTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);
		multisigTransaction.setDeadline(multisigTransaction.getTimeStamp().addHours(2));
		multisigTransaction.sign();
		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, context.cosigner2, HashUtils.calculateHash(t1));
		signatureTransaction.setDeadline(signatureTransaction.getTimeStamp().addHours(2));
		signatureTransaction.sign();

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));
		context.setBalance(context.cosigner2, Amount.fromNem(101));
		context.makeCosignatory(context.cosigner1, context.multisig);
		context.makeCosignatory(context.cosigner2, context.multisig);

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction);
		final ValidationResult result2 = context.transactions.addExisting(signatureTransaction);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.transactions.getTransactionsForNewBlock(Utils.generateRandomAddress(),
				currentTime.addMinutes(10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		final MultisigTransaction transaction = (MultisigTransaction)blockTransactions.getAll().get(0);
		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getCosignerSignatures().first(), IsSame.sameInstance(signatureTransaction));
	}

	@Test
	public void multisigTransactionIssuedNotByCosignatoryIsRejected() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);
		multisigTransaction.setDeadline(multisigTransaction.getTimeStamp().addHours(2));
		multisigTransaction.sign();

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));

		// Act:
		final ValidationResult result = context.transactions.addExisting(multisigTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	private static class TestContext {
		private final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory();
		private final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;
		private final ReadOnlyAccountStateCache accountStateCache;
		private final TimeProvider timeProvider;

		private final Account multisig = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();

		private TestContext() {
			this.singleValidator = this.factory.createSingle(this.stateCache, false);
			this.batchValidator = this.factory.createBatch(Mockito.mock(DefaultHashCache.class));
			this.accountStateCache = this.stateCache;
			this.timeProvider = Mockito.mock(TimeProvider.class);
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);
			Mockito.when(validatorFactory.createSingle(Mockito.any(), Mockito.anyBoolean())).thenReturn(this.singleValidator);
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(TimeInstant.ZERO);

			this.addState(this.multisig);
			this.addState(this.recipient);
			this.addState(this.cosigner1);
			this.addState(this.cosigner2);

			this.transactions = new UnconfirmedTransactions(
					validatorFactory,
					NisCacheFactory.createReadOnly(this.accountStateCache, transactionHashCache),
					this.timeProvider);
		}


		public MultisigTransaction createMultisigTransaction(final TimeInstant currentTime, final Transaction t1) {
			return new MultisigTransaction(currentTime, this.cosigner1, t1);
		}

		public TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Amount amount) {
			final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, this.multisig, this.recipient, amount, null);
			transferTransaction.setDeadline(timeStamp.addSeconds(1));
			return transferTransaction;
		}

		public void makeCosignatory(final Account signer, final Account multisig) {
			final BlockHeight blockHeight = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);
			this.stateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
			this.stateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), blockHeight);
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
