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
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.makeCosignatory(cosigner1, multisig);

		final Transaction t1 = createModification(cosigner1, multisig, cosigner2);
		final Transaction t2 = createModification(cosigner1, multisig, cosigner3);

		// Act / Assert:
		this.assertTransactions(
				context.nisCache,
				Arrays.asList(t1, t2),
				Arrays.asList(t1),
				ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION);
	}

	private static Transaction createModification(
			final Account cosigner,
			final Account multisig,
			final Account newCosigner) {
		final Transaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosigner)));
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		transaction.sign();
		return createMultisig(cosigner, transaction);
	}

	//endregion

	//region multisig

	@Test
	public void getBlockTransactionsDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.makeCosignatory(cosigner1, multisig);
		context.makeCosignatory(cosigner2, multisig);

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
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.makeCosignatory(cosigner1, multisig);
		context.makeCosignatory(cosigner2, multisig);

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

	private static class MultisigTestContext extends TestContext {

		public void makeCosignatory(final Account cosigner, final Account multisig) {
			final NisCache copyCache = this.nisCache.copy();
			final AccountStateCache accountStateCache = copyCache.getAccountStateCache();
			accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
			copyCache.commit();
		}
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

//	//region importance transfer
//
//	@Test
//	public void chainWithImportanceTransferToNonZeroBalanceAccountIsInvalid() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), BlockMarkerConstants.BETA_REMOTE_VALIDATION_FORK);
//		parentBlock.sign();
//
//		final Account account1 = factory.createAccountWithBalance(Amount.fromNem(100000));
//		final Account account2 = factory.createAccountWithBalance(Amount.fromNem(100000));
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		block.addTransaction(createActivateImportanceTransfer(account1, account2));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER));
//	}
//
//	@Test
//	public void chainWithImportanceTransferAndTransferToRemoteInSameBlockIsInvalid() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), BlockMarkerConstants.BETA_REMOTE_VALIDATION_FORK);
//		parentBlock.sign();
//
//		final Account account1 = factory.createAccountWithBalance(Amount.fromNem(100000));
//		final Account account2 = Utils.generateRandomAccount();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		final Transaction transaction1 = new TransferTransaction(new TimeInstant(100), account1, account2, new Amount(7), null);
//		transaction1.setDeadline(transaction1.getTimeStamp().addHours(1));
//		transaction1.sign();
//		block.addTransaction(transaction1);
//		block.addTransaction(createActivateImportanceTransfer(account1, account2));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER));
//	}
//
//	@Test
//	public void chainWithConflictingImportanceTransfersInSameBlockIsInvalid() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 123);
//		parentBlock.sign();
//
//		final Account account1 = factory.createAccountWithBalance(Amount.fromNem(20000));
//		final Account account2 = factory.createAccountWithBalance(Amount.fromNem(20000));
//		final Account accountX = Utils.generateRandomAccount();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		block.addTransaction(createActivateImportanceTransfer(account1, accountX));
//		block.addTransaction(createActivateImportanceTransfer(account2, accountX));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS));
//	}
//
//	private static Transaction createActivateImportanceTransfer(final Account account1, final Account account2) {
//		final Transaction transaction = new ImportanceTransferTransaction(
//				new TimeInstant(150),
//				account1,
//				ImportanceTransferTransaction.Mode.Activate,
//				account2);
//		transaction.setDeadline(transaction.getTimeStamp().addHours(1));
//		transaction.sign();
//		return transaction;
//	}
//
//	@Test
//	public void cannotSendTransferToRemoteAccount() {
//		// Assert:
//		assertTransferIsInvalidForRemoteAccount(TransferTransaction::getRecipient);
//	}
//
//	@Test
//	public void cannotSendTransferFromRemoteAccount() {
//		// Assert:
//		assertTransferIsInvalidForRemoteAccount(VerifiableEntity::getSigner);
//	}
//
//	private static void assertTransferIsInvalidForRemoteAccount(final Function<TransferTransaction, Account> getRemoteAccount) {
//		// Assert:
//		assertChainWithSingleInvalidTransactionIsInvalid(factory -> {
//			// Arrange:
//			final Account signer = factory.createAccountWithBalance(Amount.fromNem(100));
//			final TransferTransaction transaction = createTransfer(signer, Amount.fromNem(10), Amount.fromNem(2));
//
//			// - make the transaction signer a remote account
//			final int mode = ImportanceTransferTransaction.Mode.Activate.value();
//			factory.accountStateCache.findStateByAddress(getRemoteAccount.apply(transaction).getAddress())
//					.getRemoteLinks()
//					.addLink(new RemoteLink(Utils.generateRandomAddress(), BlockHeight.ONE, mode, RemoteLink.Owner.RemoteHarvester));
//
//			return transaction;
//		}, ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE);
//	}
//
//	//endregion
//
//	@Test
//	public void chainIsInvalidIfTransactionHashAlreadyExistInHashCache() {
//		// Arrange:
//		final long confirmedBlockHeight = 10;
//		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
//		final Transaction transaction = factory.createValidSignedTransaction();
//		Mockito.when(factory.transactionHashCache.anyHashExists(Mockito.any())).thenReturn(true);
//		final BlockChainValidator validator = factory.create();
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), confirmedBlockHeight);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		block.addTransaction(transaction);
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
//	}
//
//	@Test
//	public void chainIsInvalidIfAnyTransactionInABlockIsSignedByBlockHarvester() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		block.addTransaction(factory.createValidSignedTransaction());
//		block.addTransaction(factory.createSignedTransactionWithGivenSender(block.getSigner()));
//		block.addTransaction(factory.createValidSignedTransaction());
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
//	}
//
//	@Test
//	public void chainIsValidIfAccountSpendsAmountReceivedInEarlierBlockInLaterBlock() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Account account1 = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account account2 = factory.createAccountWithBalance(Amount.fromNem(1000));
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
//		blocks.get(0).addTransaction(createTransfer(account1, account2, Amount.fromNem(500)));
//		blocks.get(1).addTransaction(createTransfer(account2, account1, Amount.fromNem(1250)));
//		blocks.get(2).addTransaction(createTransfer(account1, account2, Amount.fromNem(1700)));
//		resignBlocks(blocks);
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	//region balance checks
//
//	@Test
//	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientBalance() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//
//		final Account signer = factory.createAccountWithBalance(Amount.fromNem(17));
//		block.addTransaction(createTransfer(signer, Amount.fromNem(15), Amount.fromNem(14)));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
//	}
//
//	@Test
//	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactBalance() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//
//		final Account signer = factory.createAccountWithBalance(Amount.fromNem(20));
//		block.addTransaction(createTransfer(signer, Amount.fromNem(15), Amount.fromNem(5)));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	@Test
//	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientBalanceForAll() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//
//		final Account signer = factory.createAccountWithBalance(Amount.fromNem(36));
//		block.addTransaction(createTransfer(signer, Amount.fromNem(6), Amount.fromNem(5)));
//		block.addTransaction(createTransfer(signer, Amount.fromNem(8), Amount.fromNem(2)));
//		block.addTransaction(createTransfer(signer, Amount.fromNem(11), Amount.fromNem(5)));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
//	}
//
//	//endregion
//
//	//region multisig modification tests
//
//	@Test
//	public void blockCanContainMultipleMultisigModificationsForDifferentAccounts() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Account multisig1 = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner1 = factory.createAccountWithBalance(Amount.ZERO);
//		factory.setCosigner(multisig1, cosigner1);
//
//		final Account multisig2 = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
//		factory.setCosigner(multisig2, cosigner2);
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
//		blocks.get(0).addTransaction(createMultisigModification(multisig1, cosigner1));
//		blocks.get(0).addTransaction(createMultisigModification(multisig2, cosigner2));
//		resignBlocks(blocks);
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	@Test
//	public void blockCannotContainMultipleMultisigModificationsForSameAccount() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Account multisig1 = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner1 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
//		factory.setCosigner(multisig1, cosigner1);
//		factory.setCosigner(multisig1, cosigner2);
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
//		blocks.get(0).addTransaction(createMultisigModification(multisig1, cosigner1));
//		blocks.get(0).addTransaction(createMultisigModification(multisig1, cosigner2));
//		resignBlocks(blocks);
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION));
//	}
//
//	@Test
//	public void blockCannotContainModificationWithMultipleDeletes() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner1 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner3 = factory.createAccountWithBalance(Amount.ZERO);
//		factory.setCosigner(multisig, cosigner1);
//		factory.setCosigner(multisig, cosigner2);
//		factory.setCosigner(multisig, cosigner3);
//
//		final List<MultisigModification> modifications = Arrays.asList(
//				new MultisigModification(MultisigModificationType.Del, cosigner2),
//				new MultisigModification(MultisigModificationType.Del, cosigner3));
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
//		blocks.get(0).addTransaction(createMultisigModification(multisig, cosigner1, modifications));
//		resignBlocks(blocks);
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES));
//	}
//
//	@Test
//	public void blockCanContainModificationWithSingleDelete() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner1 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
//		factory.setCosigner(multisig, cosigner1);
//		factory.setCosigner(multisig, cosigner2);
//
//		final List<MultisigModification> modifications = Arrays.asList(
//				new MultisigModification(MultisigModificationType.Del, cosigner2));
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
//		blocks.get(0).addTransaction(createMultisigModification(multisig, cosigner1, modifications));
//		resignBlocks(blocks);
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	private static Transaction createMultisigModification(final Account multisig, final Account cosigner) {
//		return createMultisigModification(
//				multisig,
//				cosigner,
//				Arrays.asList(new MultisigModification(MultisigModificationType.Add, Utils.generateRandomAccount())));
//	}
//
//	private static Transaction createMultisigModification(final Account multisig, final Account cosigner, final List<MultisigModification> modifications) {
//		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
//		Transaction transfer = new MultisigAggregateModificationTransaction(currentTime, multisig, modifications);
//		transfer = prepareTransaction(transfer);
//		transfer.setSignature(null);
//
//		final MultisigTransaction msTransaction = new MultisigTransaction(currentTime, cosigner, transfer);
//		msTransaction.setFee(Amount.fromNem(200));
//		return prepareTransaction(msTransaction);
//	}
//
//	//endregion
//
//	//region multisig tests
//
//	@Test
//	public void canValidateMultisigTransferFromCosignerAccountWithZeroBalance() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner = factory.createAccountWithBalance(Amount.ZERO);
//		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
//
//		// Act:
//		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, recipient);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	@Test
//	public void multisigTransferHasFeesAndAmountsDeductedFromMultisigAccount() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner = factory.createAccountWithBalance(Amount.fromNem(200));
//		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
//
//		// Act:
//		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, recipient);
//
//		// Assert:
//		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) = 690
//		// - C 200 - 0 (No Change) = 200
//		// - R 0 + 100 (Inner T amount) = 100
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(690)));
//		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(200)));
//		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
//	}
//
//	@Test
//	public void canValidateMultisigTransferWithMultipleSignaturesFromCosignerAccountWithZeroBalance() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account cosigner3 = factory.createAccountWithBalance(Amount.ZERO);
//		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
//
//		// Act:
//		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//	}
//
//	@Test
//	public void multisigTransferWithMultipleSignaturesHasFeesAndAmountsDeductedFromMultisigAccount() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
//		final Account cosigner = factory.createAccountWithBalance(Amount.fromNem(201));
//		final Account cosigner2 = factory.createAccountWithBalance(Amount.fromNem(202));
//		final Account cosigner3 = factory.createAccountWithBalance(Amount.fromNem(203));
//		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
//		// Act:
//		final ValidationResult result = runMultisigTransferTest(factory, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);
//
//		// Assert:
//		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) - 2 * 6 (Signature fee) = 678
//		// - C1 201 - 0 (No Change) = 201
//		// - C2 202 - 0 (No Change) = 202
//		// - C3 203 - 0 (No Change) = 203
//		// - R 0 + 100 (Inner T amount) = 100
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
//		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(678)));
//		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(201)));
//		Assert.assertThat(factory.getAccountInfo(cosigner2).getBalance(), IsEqual.equalTo(Amount.fromNem(202)));
//		Assert.assertThat(factory.getAccountInfo(cosigner3).getBalance(), IsEqual.equalTo(Amount.fromNem(203)));
//		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
//	}
//
//	private static ValidationResult runMultisigTransferTest(
//			final BlockChainValidatorFactory factory,
//			final Account multisig,
//			final Account cosigner,
//			final Account recipient) {
//		return runMultisigTransferTest(
//				factory,
//				multisig,
//				cosigner,
//				Arrays.asList(),
//				recipient);
//	}
//
//	private static ValidationResult runMultisigTransferTest(
//			final BlockChainValidatorFactory factory,
//			final Account multisig,
//			final Account cosigner,
//			final List<Account> otherCosigners,
//			final Account recipient) {
//		// Arrange:
//		final BlockChainValidator validator = factory.create();
//
//		factory.setCosigner(multisig, cosigner);
//		for (final Account otherCosigner : otherCosigners) {
//			factory.setCosigner(multisig, otherCosigner);
//		}
//
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
//		parentBlock.sign();
//
//		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
//		Transaction transfer = createTransfer(multisig, recipient, Amount.fromNem(100));
//		transfer.setFee(Amount.fromNem(10));
//		transfer = prepareTransaction(transfer);
//		transfer.setSignature(null);
//
//		final MultisigTransaction msTransaction = new MultisigTransaction(currentTime, cosigner, transfer);
//		msTransaction.setFee(Amount.fromNem(200));
//
//		for (final Account otherCosigner : otherCosigners) {
//			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, otherCosigner, multisig, transfer);
//			signatureTransaction.setFee(Amount.fromNem(6));
//			msTransaction.addSignature(prepareTransaction(signatureTransaction));
//		}
//
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
//		blocks.get(0).addTransaction(prepareTransaction(msTransaction));
//		resignBlocks(blocks);
//
//		// Act:
//		return validator.isValid(parentBlock, blocks);
//	}
//
//	@Test
//	public void blockConvertingAccountToMultisigCannotAlsoMakeOtherTransactionsFromThatAccountInSameBlock() {
//		// Arrange:
//		final BlockChainValidatorFactory factory = createValidatorFactory();
//		final BlockChainValidator validator = factory.create();
//		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK);
//		parentBlock.sign();
//
//		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
//		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(10000));
//		final Account cosigner = factory.createAccountWithBalance(Amount.fromNem(10000));
//
//		// - make the account multisig
//		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
//		final Block block = blocks.get(1);
//		final Transaction transaction1 = new MultisigAggregateModificationTransaction(
//				currentTime,
//				multisig,
//				Arrays.asList(new MultisigModification(MultisigModificationType.Add, cosigner)));
//		block.addTransaction(prepareTransaction(transaction1));
//
//		// - create a transfer transaction from the multisig account
//		final Transaction transaction2 = createTransfer(
//				multisig,
//				Utils.generateRandomAccount(),
//				Amount.fromNem(100));
//		block.addTransaction(prepareTransaction(transaction2));
//		block.sign();
//
//		// Act:
//		final ValidationResult result = validator.isValid(parentBlock, blocks);
//
//		// Assert:
//		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
//	}
//

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

		private MockTransaction createValidSignedTransaction() {
			final TimeInstant timeInstant = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME / 2);
			final MockTransaction transaction = new MockTransaction(12, timeInstant);
			transaction.sign();
			return prepareMockTransaction(transaction);
		}

		private MockTransaction prepareMockTransaction(final MockTransaction transaction) {
			this.prepareAccount(transaction.getSigner(), Amount.fromNem(100));
			return transaction;
		}
	}
}
