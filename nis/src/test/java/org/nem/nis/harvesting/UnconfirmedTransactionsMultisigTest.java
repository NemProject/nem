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
import org.nem.nis.validators.AggregateSingleTransactionValidatorBuilder;
import org.nem.nis.validators.BatchTransactionValidator;
import org.nem.nis.validators.SingleTransactionValidator;
import org.nem.nis.validators.TransactionValidatorFactory;

import java.util.Arrays;

public class UnconfirmedTransactionsMultisigTest {

	// TODO 20150103 J-G: tests seem fine, but also seems like you're mostly testing the same stuff in blockchainservices; which are getting the validators
	// > from the same place

	@Test
	public void multisigTransactionIssuedNotByCosignatoryIsRejected() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));

		// Act:
		final ValidationResult result = context.transactions.addExisting(multisigTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void transferTransactionIssuedByMultisigIsRejected() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction transaction = new TransferTransaction(currentTime, context.multisig, context.recipient, Amount.fromNem(7), null);
		transaction.setDeadline(currentTime.addSeconds(1));
		transaction.sign();

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.makeCosignatory(context.cosigner1, context.multisig);

		// Act:
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void getTransactionsForNewBlockDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));
		context.makeCosignatory(context.cosigner1, context.multisig);
		context.makeCosignatory(context.cosigner2, context.multisig);

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.getTransactionsForNewBlock(currentTime);

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
		final UnconfirmedTransactions blockTransactions = context.getTransactionsForNewBlock(currentTime);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		final MultisigTransaction transaction = (MultisigTransaction)blockTransactions.getAll().get(0);
		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getCosignerSignatures().first(), IsSame.sameInstance(signatureTransaction));
	}

	@Test
	public void filterRemovesMultisigTransactionThatHasSameInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);
		context.makeCosignatory(context.cosigner1, context.multisig);

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction1 = context.createMultisigTransaction(currentTime, t1);
		final MultisigTransaction multisigTransaction2 = context.createMultisigTransaction(currentTime.addSeconds(1), t1);

		context.setBalance(context.multisig, Amount.fromNem(16));
		context.setBalance(context.cosigner1, Amount.fromNem(200));

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction1);
		final ValidationResult result2 = context.transactions.addExisting(multisigTransaction2);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.getTransactionsForNewBlock(currentTime);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		// TODO 2050102 G-* : should this actually succeed?
		// TODO 20150103 J-G : probably not
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));

		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
	}

	// TODO 20150103 J-G : how is this different from the previous?
	@Test
	public void filterRemovesMultisigModificationTransactionThatHasSameInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(11);
		context.makeCosignatory(context.cosigner1, context.multisig);

		final Transaction t1 = context.createMultisigAddTransaction(currentTime, context.cosigner2);
		final MultisigTransaction multisigTransaction1 = context.createMultisigTransaction(currentTime, t1);
		final Transaction t2 = context.createMultisigAddTransaction(currentTime, context.recipient);
		final MultisigTransaction multisigTransaction2 = context.createMultisigTransaction(currentTime, t2);

		context.setBalance(context.multisig, Amount.fromNem(2000));
		context.setBalance(context.cosigner1, Amount.fromNem(200));

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction1);
		final ValidationResult result2 = context.transactions.addExisting(multisigTransaction2);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.getTransactionsForNewBlock(currentTime);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));

		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
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
			this.singleValidator = this.factory.createSingle(this.stateCache);
			this.batchValidator = this.factory.createBatch(Mockito.mock(DefaultHashCache.class));
			this.accountStateCache = this.stateCache;
			this.timeProvider = Mockito.mock(TimeProvider.class);
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);

			final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
			builder.add(this.singleValidator);
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any())).thenReturn(builder);

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

		public Transaction createMultisigAddTransaction(final TimeInstant timeStamp, final Account account) {
			final Transaction transaction = new MultisigSignerModificationTransaction(timeStamp, this.multisig,
					Arrays.asList(new MultisigModification(MultisigModificationType.Add, account)));
			transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
			return transaction;
		}

		private UnconfirmedTransactions getTransactionsForNewBlock(TimeInstant currentTime) {
			return this.transactions.getTransactionsForNewBlock(
					Utils.generateRandomAddress(),
					currentTime.addMinutes(10));
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
