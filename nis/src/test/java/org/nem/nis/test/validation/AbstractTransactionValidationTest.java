package org.nem.nis.test.validation;

import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractTransactionValidationTest {

	//region ported from DefaultNewBlockTransactionsProviderTest

	//region transfers

	@Test
	public void getBlockTransactionsAllowsEarlierBlockTransfersToBeSpentLater() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// - T(O) - S: 20 | R: 10
		// - T(1) - S -15-> R | S: 03 | R: 25
		// - T(2) - R -12-> S | S: 15 | R: 10 # this transfer is allowed even though it wouldn't be allowed in reverse order
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t1.setFee(Amount.fromNem(2));
		t1.sign();
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t2.setFee(Amount.fromNem(3));
		t2.sign();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1, t2),
				ValidationResult.SUCCESS);
	}

	@Test
	public void getBlockTransactionsExcludesTransactionFromNextBlockIfConfirmedBalanceIsInsufficient() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// - T(O) - S: 20 | R: 10
		// - T(1) - R -12-> S | XXX
		// - T(2) - S -15-> R | S: 03 | R: 25
		final Transaction t1 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		final Transaction t2 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t2.setFee(Amount.fromNem(2));
		t2.sign();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t2),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	public static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = createTransferTransaction(TimeInstant.ZERO, sender, recipient, amount);
		transferTransaction.sign();
		return transferTransaction;
	}

	//endregion

	//region importance transfer

	@Test
	public void getBlockTransactionsDoesNotAllowConflictingImportanceTransfersToBeInSingleBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(50000));
		final Account remote = context.addAccount(Amount.ZERO);
		final Account remote2 = context.addAccount(Amount.ZERO);

		final Transaction t1 = createActivateImportanceTransfer(sender, remote);
		final Transaction t2 = createActivateImportanceTransfer(sender, remote2);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	private static Transaction createActivateImportanceTransfer(final Account sender, final Account remote) {
		final Transaction transaction = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		transaction.setDeadline(transaction.getTimeStamp().addMinutes(1));
		transaction.sign();
		return transaction;
	}

	//endregion

	//region multisig modification

	@Test
	public void getBlockTransactionsDoesNotAllowMultipleMultisigModificationsForSameAccountToBeInSingleBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);

		final Transaction t1 = createMultisigModification(multisig, cosigner1, cosigner2);
		final Transaction t2 = createMultisigModification(multisig, cosigner1, cosigner3);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner, final List<MultisigModification> modifications) {
		final Transaction transaction = new MultisigAggregateModificationTransaction(TimeInstant.ZERO, multisig, modifications);
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		transaction.setSignature(null);
		return createMultisig(cosigner, transaction);
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner, final Account newCosigner) {
		return createMultisigModification(
				multisig,
				cosigner,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosigner)));
	}

	private static MultisigTransaction createMultisigModification(final Account multisig, final Account cosigner) {
		return createMultisigModification(multisig, cosigner, Utils.generateRandomAccount());
	}

	//endregion

	//region multisig

	@Test
	public void getBlockTransactionsDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final Transaction t1 = createTransferTransaction(TimeInstant.ZERO, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(mt1),
				Arrays.asList(),
				ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS);
	}

	@Test
	public void getBlockTransactionsReturnsMultisigTransactionIfMultisigSignaturesArePresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final Transaction t1 = createTransferTransaction(TimeInstant.ZERO, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		mt1.addSignature(createSignature(cosigner2, multisig, t1));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(mt1),
				Arrays.asList(mt1),
				ValidationResult.SUCCESS);
	}

	private static MultisigTransaction createMultisig(final Account cosigner, final Transaction innerTransaction) {
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, cosigner, innerTransaction);
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		transaction.sign();
		return transaction;
	}

	private static MultisigSignatureTransaction createSignature(final Account cosigner, final Account multisig, final Transaction innerTransaction) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner, multisig, innerTransaction);
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		transaction.sign();
		return transaction;
	}

	//endregion

	//endregion

	//region ported from BlockChainValidatorIntegrationTest

	//region general

	@Test
	public void allTransactionsInChainMustNotBePastDeadline() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime().addHours(2);
			final Transaction t = context.prepareMockTransaction(new MockTransaction(0, currentTime));
			context.prepareAccount(t.getSigner(), Amount.fromNem(100));
			t.setDeadline(currentTime.addHours(-1));
			return t;
		}, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void allTransactionsInChainMustHaveValidTimestamp() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final TimeInstant futureTime = NisMain.TIME_PROVIDER.getCurrentTime().addHours(2);
			final Transaction t = context.prepareMockTransaction(new MockTransaction(0, futureTime));
			context.prepareAccount(t.getSigner(), Amount.fromNem(100));
			t.setDeadline(futureTime.addSeconds(10));
			return t;
		}, ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE);
	}

	@Test
	public void chainWithTransactionWithInsufficientFeeIsInvalid() {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			final Transaction t = context.createValidSignedTransaction();
			t.setFee(Amount.fromNem(1));
			return t;
		}, ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	private void assertSingleBadTransactionIsFilteredOut(
			final Function<TestContext, Transaction> getBadTransaction,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = getBadTransaction.apply(context);
		t2.sign();
		final Transaction t3 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t3),
				Arrays.asList(t1, t3),
				expectedResult);
	}

	// TODO 20150203 J-B: seems like the new block filter is not filtering these next two :/

	@Test
	public void chainIsInvalidIfTransactionHashAlreadyExistInHashCache() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();
		final Transaction t3 = context.createValidSignedTransaction();

		final NisCache copyCache = context.nisCache.copy();
		copyCache.getTransactionHashCache()
				.put(new HashMetaDataPair(HashUtils.calculateHash(t2), new HashMetaData(new BlockHeight(10), new TimeInstant(20))));
		copyCache.commit();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t3),
				Arrays.asList(t1, t3),
				ValidationResult.NEUTRAL);
	}

	@Test
	public void chainIsInvalidIfDuplicateTransactionExistsInChain() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t1),
				Arrays.asList(t1),
				ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN);
	}

	@Test
	public void chainWithAllValidTransactionsIsValid() {
		// Assert:
		final TestContext context = new TestContext();
		final Transaction t1 = context.createValidSignedTransaction();
		final Transaction t2 = context.createValidSignedTransaction();
		final Transaction t3 = context.createValidSignedTransaction();

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t3),
				Arrays.asList(t1, t2, t3),
				ValidationResult.SUCCESS);
	}

	//endregion

	//region importance transfer

	@Test
	public void chainWithImportanceTransferToNonZeroBalanceAccountIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100000));
		final Account account2 = context.addAccount(Amount.fromNem(100000));

		final Transaction t1 = createActivateImportanceTransfer(account1, account2);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER);
	}

	@Test
	public void chainWithTransferToRemoteFollowedByImportanceTransferIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100000));
		final Account account2 = context.addAccount(Amount.ZERO);

		final Transaction t1 = createTransferTransaction(account1, account2, new Amount(7));
		final Transaction t2 = createActivateImportanceTransfer(account1, account2);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER);
	}

	@Test
	public void chainWithConflictingImportanceTransfersIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(20000));
		final Account account2 = context.addAccount(Amount.fromNem(20000));
		final Account accountX = Utils.generateRandomAccount();

		final Transaction t1 = createActivateImportanceTransfer(account1, accountX);
		final Transaction t2 = createActivateImportanceTransfer(account2, accountX);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void cannotSendTransferToRemoteAccount() {
		// Assert:
		this.assertTransferIsInvalidForRemoteAccount(TransferTransaction::getRecipient);
	}

	@Test
	public void cannotSendTransferFromRemoteAccount() {
		// Assert:
		this.assertTransferIsInvalidForRemoteAccount(VerifiableEntity::getSigner);
	}

	private void assertTransferIsInvalidForRemoteAccount(final Function<TransferTransaction, Account> getRemoteAccount) {
		// Assert:
		this.assertSingleBadTransactionIsFilteredOut(context -> {
			// Arrange:
			final Account signer = context.addAccount(Amount.fromNem(100));
			final TransferTransaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(10));

			// - make the transaction signer a remote account
			context.makeRemote(getRemoteAccount.apply(t1));
			return t1;
		}, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
	}

	//endregion

	//region transfer
	
	@Test
	public void chainIsValidIfAccountSpendsAmountReceivedEarlier() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));

		final Transaction t1 = createTransferTransaction(account1, account2, Amount.fromNem(500));
		final Transaction t2 = createTransferTransaction(account2, account1, Amount.fromNem(1250));
		final Transaction t3 = createTransferTransaction(account1, account2, Amount.fromNem(1700));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t3),
				Arrays.asList(t1, t2, t3),
				ValidationResult.SUCCESS);
	}

	@Test
	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(17));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(20));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	@Test
	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(22));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(20));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(t1),
				ValidationResult.SUCCESS);
	}

	@Test
	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientBalanceForAll() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signer = context.addAccount(Amount.fromNem(36));
		final Transaction t1 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(9));
		final Transaction t2 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(8));
		final Transaction t3 = createTransferTransaction(signer, Utils.generateRandomAccount(), Amount.fromNem(14));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2, t3),
				Arrays.asList(t1, t2),
				ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	//endregion

	//region multisig modification tests

	@Test
	public void blockCanContainMultipleMultisigModificationsForDifferentAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);

		final Account multisig2 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig2, cosigner2);

		final Transaction t1 = createMultisigModification(multisig1, cosigner1);
		final Transaction t2 = createMultisigModification(multisig2, cosigner2);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1, t2),
				ValidationResult.SUCCESS);
	}

	@Test
	public void blockCannotContainMultipleMultisigModificationsForSameAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig1 = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig1, cosigner1);
		context.setCosigner(multisig1, cosigner2);

		final MultisigTransaction t1 = createMultisigModification(multisig1, cosigner1);
		t1.addSignature(createSignature(cosigner2, multisig1, t1.getOtherTransaction()));

		final MultisigTransaction t2 = createMultisigModification(multisig1, cosigner2);
		t2.addSignature(createSignature(cosigner1, multisig1, t2.getOtherTransaction()));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	@Test
	public void blockCannotContainModificationWithMultipleDeletes() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);
		context.setCosigner(multisig, cosigner3);

		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.Del, cosigner2),
				new MultisigModification(MultisigModificationType.Del, cosigner3));

		final Transaction t1 = createMultisigModification(multisig, cosigner1, modifications);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(),
				ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES);
	}

	@Test
	public void blockCanContainModificationWithSingleDelete() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		context.setCosigner(multisig, cosigner1);
		context.setCosigner(multisig, cosigner2);

		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(MultisigModificationType.Del, cosigner2));

		final Transaction t1 = createMultisigModification(multisig, cosigner1, modifications);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(t1),
				ValidationResult.SUCCESS);
	}

	//endregion

	//region multisig tests

	@Test
	public void canValidateMultisigTransferFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, recipient);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(t1),
				ValidationResult.SUCCESS);
	}

	/*
	TODO need to move this to block chain validator tests

	@Test
	public void multisigTransferHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.fromNem(200));
		final Account recipient = context.addAccount(Amount.ZERO);

		// Act:
		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, recipient);

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) = 690
		// - C 200 - 0 (No Change) = 200
		// - R 0 + 100 (Inner T amount) = 100
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(690)));
		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(200)));
		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}
	*/

	@Test
	public void canValidateMultisigTransferWithMultipleSignaturesFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1),
				Arrays.asList(t1),
				ValidationResult.SUCCESS);
	}

	/*
		TODO need to move this to block chain validator tests
	@Test
	public void multisigTransferWithMultipleSignaturesHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.fromNem(201));
		final Account cosigner2 = context.addAccount(Amount.fromNem(202));
		final Account cosigner3 = context.addAccount(Amount.fromNem(203));
		final Account recipient = context.addAccount(Amount.ZERO);
		// Act:
		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) - 2 * 6 (Signature fee) = 678
		// - C1 201 - 0 (No Change) = 201
		// - C2 202 - 0 (No Change) = 202
		// - C3 203 - 0 (No Change) = 203
		// - R 0 + 100 (Inner T amount) = 100
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(678)));
		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(201)));
		Assert.assertThat(factory.getAccountInfo(cosigner2).getBalance(), IsEqual.equalTo(Amount.fromNem(202)));
		Assert.assertThat(factory.getAccountInfo(cosigner3).getBalance(), IsEqual.equalTo(Amount.fromNem(203)));
		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}
	 */

	protected static MultisigTransaction createMultisigWithSignatures(
			final TestContext context,
			final Account multisig,
			final Account cosigner,
			final Account recipient) {
		return createMultisigWithSignatures(
				context,
				multisig,
				cosigner,
				Arrays.asList(),
				recipient);
	}

	protected static MultisigTransaction createMultisigWithSignatures(
			final TestContext context,
			final Account multisig,
			final Account cosigner,
			final List<Account> otherCosigners,
			final Account recipient) {
		// Arrange:
		context.setCosigner(multisig, cosigner);
		for (final Account otherCosigner : otherCosigners) {
			context.setCosigner(multisig, otherCosigner);
		}

		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transfer = createTransferTransaction(multisig, recipient, Amount.fromNem(100));
		transfer.setFee(Amount.fromNem(10));
		transfer.setSignature(null);

		final MultisigTransaction msTransaction = new MultisigTransaction(currentTime, cosigner, transfer);
		msTransaction.setFee(Amount.fromNem(200));

		for (final Account otherCosigner : otherCosigners) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, otherCosigner, multisig, transfer);
			signatureTransaction.setFee(Amount.fromNem(6));
			msTransaction.addSignature(prepareTransaction(signatureTransaction));
		}

		return prepareTransaction(msTransaction);
	}

	@Test
	public void blockConvertingAccountToMultisigCannotAlsoMakeOtherTransactionsFromThatAccountInSameBlock() {
		// Arrange:
		final TestContext context = new TestContext();

		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Account multisig = context.addAccount(Amount.fromNem(10000));
		final Account cosigner = context.addAccount(Amount.fromNem(10000));

		// - make the account multisig
		Transaction t1 = new MultisigAggregateModificationTransaction(
				currentTime,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, cosigner)));
		t1 = prepareTransaction(t1);

		// - create a transfer transaction from the multisig account
		final Transaction t2 = createTransferTransaction(
				multisig,
				Utils.generateRandomAccount(),
				Amount.fromNem(100));

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG);
	}

	//endregion

	protected abstract void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult);

	private static <T extends Transaction> T prepareTransaction(final T transaction) {
		// set the deadline appropriately and sign
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		transaction.setDeadline(currentTime.addSeconds(10));
		transaction.sign();
		return transaction;
	}

	public static class TestContext {
		public final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();

		public TestContext() {
			// add one large account that is harvesting-eligible
			this.addAccount(Amount.fromNem(100000));
		}

		public Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountState accountState = copyCache.getAccountStateCache().findStateByAddress(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			accountState.setHeight(BlockHeight.ONE);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
			copyCache.commit();
			return account;
		}

		public void makeRemote(final Account account) {
			final NisCache copyCache = this.nisCache.copy();
			final int mode = ImportanceTransferTransaction.Mode.Activate.value();
			copyCache.getAccountStateCache().findStateByAddress(account.getAddress())
					.getRemoteLinks()
					.addLink(new RemoteLink(Utils.generateRandomAddress(), BlockHeight.ONE, mode, RemoteLink.Owner.RemoteHarvester));
			copyCache.commit();
		}

		public void setCosigner(final Account account) {
			final NisCache copyCache = this.nisCache.copy();
			final int mode = ImportanceTransferTransaction.Mode.Activate.value();
			copyCache.getAccountStateCache().findStateByAddress(account.getAddress())
					.getRemoteLinks()
					.addLink(new RemoteLink(Utils.generateRandomAddress(), BlockHeight.ONE, mode, RemoteLink.Owner.RemoteHarvester));
			copyCache.commit();
		}

		private void setCosigner(final Account multisig, final Account cosigner) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountStateCache accountStateCache = copyCache.getAccountStateCache();
			accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
			accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			copyCache.commit();
		}

		public MockTransaction createValidSignedTransaction() {
			return this.createValidSignedTransaction(Utils.generateRandomAccount());
		}

		public MockTransaction createValidSignedTransaction(final Account signer) {
			final TimeInstant timeInstant = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME / 2);
			final MockTransaction transaction = new MockTransaction(signer, 12, timeInstant);
			transaction.sign();
			return this.prepareMockTransaction(transaction);
		}

		private MockTransaction prepareMockTransaction(final MockTransaction transaction) {
			this.prepareAccount(transaction.getSigner(), Amount.fromNem(100));
			return transaction;
		}
	}
}
