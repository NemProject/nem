package org.nem.nis.harvesting;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.util.Arrays;

/**
 * Those test are similar to those that can be found in BlockChainServices.
 * The point is to have end-to-end tests, to make sure that:
 * a) BlockChainServices rejects invalid blocks (that could be generated with altered UT)
 * b) UT does not let generate invalid blocks (would be bad for harvesters)
 */
public class UnconfirmedTransactionsMultisigTest {
	final static TimeInstant CURRENT_SYSTEM_TIME = new TimeInstant(10_000);
	// TODO 20150107 J-G: should rename this to something else
	final static TimeInstant currentTime = CURRENT_SYSTEM_TIME.addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME - 1);

	// TODO 20150105 J-G: should we look into merging those tests somehow?
	@Test
	public void multisigTransactionIssuedNotByCosignatoryIsRejected() {
		// Arrange:
		final TestContext context = new TestContext();

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
	public void properSignatureIsAccepted() {
		assertSignatureAcceptance(true, false, ValidationResult.SUCCESS);
	}

	@Test
	public void expiredSignatureIsNotAccepted() {
		assertSignatureAcceptance(false, false, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void multisigTransactionWithSignatureIsAccepted() {
		assertSignatureAcceptance(true, true, ValidationResult.SUCCESS);
	}

	@Test
	public void multisigTransactionWithExpiredSignatureIsNotAccepted() {
		assertSignatureAcceptance(false, true, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	// TODO 20150107 J-G: should split up into two functions
	private static void assertSignatureAcceptance(boolean nonExpiredSignature, boolean signatureIsPartOfMultisig, ValidationResult validationResult) {
		// Arrange:
		final TestContext context = new TestContext();
		context.makeCosignatory(context.cosigner1, context.multisig);
		context.makeCosignatory(context.cosigner2, context.multisig);

		context.setBalance(context.multisig, Amount.fromNem(10));
		context.setBalance(context.cosigner1, Amount.fromNem(101));
		context.setBalance(context.cosigner2, Amount.fromNem(10));

		final Transaction t1 = context.createTransferTransaction(currentTime, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(currentTime, t1);

		final ValidationResult result1 = signatureIsPartOfMultisig ? null : context.transactions.addExisting(multisigTransaction);

		final TimeInstant signatureTime = nonExpiredSignature ? currentTime : CURRENT_SYSTEM_TIME.addSeconds(-2);
		final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(signatureTime, context.cosigner2, HashUtils.calculateHash(t1));
		signature.setDeadline(signature.getTimeStamp().addSeconds(1));
		signature.sign();

		if (signatureIsPartOfMultisig) {
			multisigTransaction.addSignature(signature);
		}

		// Act:
		final ValidationResult result2 = signatureIsPartOfMultisig ?
				context.transactions.addExisting(multisigTransaction) :
				context.transactions.addExisting(signature);

		// Assert:
		if (! signatureIsPartOfMultisig) {
			Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		}
		Assert.assertThat(result2, IsEqual.equalTo(validationResult));
	}
	
	@Test
	public void filterRemovesMultisigTransactionThatHasSameInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
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
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.NEUTRAL));

		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void filterRemovesMultisigModificationTransactionThatHasMultipleMultisigAggregateModificationTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
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
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));

		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
	}

	private static class TestContext {
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory(this.timeProvider);
		private final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
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
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any())).then((invocationOnMock) -> this.factory.createSingleBuilder(this.stateCache));

			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(CURRENT_SYSTEM_TIME);

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
			final Transaction transaction = new MultisigAggregateModificationTransaction(timeStamp, this.multisig,
					Arrays.asList(new MultisigModification(MultisigModificationType.Add, account)));
			transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
			return transaction;
		}

		private UnconfirmedTransactions getTransactionsForNewBlock(final TimeInstant currentTime) {
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
