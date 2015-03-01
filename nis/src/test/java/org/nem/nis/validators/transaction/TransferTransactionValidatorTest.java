package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.*;

public class TransferTransactionValidatorTest {
	private static final TSingleTransactionValidator<TransferTransaction> VALIDATOR = new TransferTransactionValidator();

	//region predicate delegation

	//region non-self

	@Test
	public void directedTransferSucceedsIfSignerHasBalanceGreaterThanSumOfFeeAndAmount() {
		// Assert:
		assertDirectedDebitPredicateDelegation(12, 7, 20, ValidationResult.SUCCESS);
	}

	@Test
	public void directedTransferSucceedsIfSignerHasBalanceEqualToSumOfFeeAndAmount() {
		// Assert:
		assertDirectedDebitPredicateDelegation(12, 7, 19, ValidationResult.SUCCESS);
	}

	@Test
	public void directedTransferFailsIfSignerHasBalanceLessThanSumOfFeeAndAmount() {
		// Assert:
		assertDirectedDebitPredicateDelegation(12, 7, 18, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	//endregion

	//region self (post fork)

	@Test
	public void selfTransferAfterForkSucceedsIfSignerHasBalanceGreaterThanSumOfFeeAndAmount() {
		// Assert:
		assertPostForkSelfDebitPredicateDelegation(12, 7, 20, ValidationResult.SUCCESS);
	}

	@Test
	public void selfTransferAfterForkSucceedsIfSignerHasBalanceEqualToSumOfFeeAndAmount() {
		// Assert:
		assertPostForkSelfDebitPredicateDelegation(12, 7, 19, ValidationResult.SUCCESS);
	}

	@Test
	public void selfTransferAfterForkFailsIfSignerHasBalanceLessThanSumOfFeeAndAmount() {
		// Assert:
		assertPostForkSelfDebitPredicateDelegation(12, 7, 18, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	//endregion

	//region self (pre fork)

	@Test
	public void selfTransferBeforeForkSucceedsIfSignerHasBalanceGreaterThanEachOfFeeAndAmount() {
		// Assert:
		assertPreForkSelfDebitPredicateDelegation(12, 7, 14, ValidationResult.SUCCESS);
		assertPreForkSelfDebitPredicateDelegation(12, 7, 99, ValidationResult.SUCCESS);
	}

	@Test
	public void selfTransferBeforeForkSucceedsIfSignerHasBalanceEqualToLargerOfFeeAndAmount() {
		// Assert:
		assertPreForkSelfDebitPredicateDelegation(12, 7, 12, ValidationResult.SUCCESS);
		assertPreForkSelfDebitPredicateDelegation(7, 12, 12, ValidationResult.SUCCESS);
	}

	@Test
	public void selfTransferBeforeForkFailsIfSignerHasBalanceLessThanLargerOfFeeAndAmount() {
		// Assert:
		assertPreForkSelfDebitPredicateDelegation(12, 7, 11, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		assertPreForkSelfDebitPredicateDelegation(7, 12, 11, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	//endregion

	private static void assertDirectedDebitPredicateDelegation(
			final int amount,
			final int fee,
			final int signerBalance,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TransferTransaction transaction = createTransaction(amount, fee);

		// Assert:
		final BlockHeight height = BlockHeight.ONE;
		assertDebitPredicateDelegation(transaction, height, amount + fee, signerBalance, expectedValidationResult);
	}

	private static void assertPostForkSelfDebitPredicateDelegation(
			final int amount,
			final int fee,
			final int signerBalance,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TransferTransaction transaction = createSelfTransaction(amount, fee);

		// Assert:
		final BlockHeight height = new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK);
		assertDebitPredicateDelegation(transaction, height, amount + fee, signerBalance, expectedValidationResult);
	}

	private static void assertDebitPredicateDelegation(
			final TransferTransaction transaction,
			final BlockHeight height,
			final int requiredAmount,
			final int signerBalance,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any()))
				.then(invocationOnMock -> {
					final Amount requestedAmount = (Amount)invocationOnMock.getArguments()[1];
					return Amount.fromNem(signerBalance).compareTo(requestedAmount) >= 0;
				});

		// Act:
		final ValidationResult result = VALIDATOR.validate(transaction, new ValidationContext(height, debitPredicate));

		// Assert:
		Mockito.verify(debitPredicate, Mockito.only()).canDebit(transaction.getSigner(), Amount.fromNem(requiredAmount));
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	private static void assertPreForkSelfDebitPredicateDelegation(
			final int amount,
			final int fee,
			final int signerBalance,
			final ValidationResult expectedValidationResult) {
		// Arrange:
		final TransferTransaction transaction = createSelfTransaction(amount, fee);

		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any()))
				.then(invocationOnMock -> {
					final Amount requestedAmount = (Amount)invocationOnMock.getArguments()[1];
					return Amount.fromNem(signerBalance).compareTo(requestedAmount) >= 0;
				});

		// Act:
		final BlockHeight height = new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK - 1);
		final ValidationResult result = VALIDATOR.validate(transaction, new ValidationContext(height, debitPredicate));

		// Assert:
		Mockito.verify(debitPredicate, Mockito.times(1)).canDebit(transaction.getSigner(), Amount.fromNem(fee));
		Mockito.verify(debitPredicate, Mockito.atMost(1)).canDebit(transaction.getSigner(), Amount.fromNem(amount));
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	private static TransferTransaction createSelfTransaction(final int amount, final int fee) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final TransferTransaction transaction = createTransferTransaction(signer, signer, amount, null);
		transaction.setFee(Amount.fromNem(fee));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));
		return transaction;
	}

	private static TransferTransaction createTransaction(final int amount, final int fee) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, amount, null);
		transaction.setFee(Amount.fromNem(fee));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));
		return transaction;
	}

	//endregion

	//region zero amount

	@Test
	public void transactionWithZeroAmountIsValid() {
		// Arrange:
		final TransferTransaction transaction = createTransaction(0, 1);

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region messages

	@Test
	public void smallMessagesAreValid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(0), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(95), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(96), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(97), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(this.isMessageSizeValid(1001), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
	}

	private ValidationResult isMessageSizeValid(final int messageSize) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, 1, message);
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Act:
		return validate(transaction);
	}

	//endregion

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount, final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message);
	}

	private static ValidationResult validate(final TransferTransaction transaction) {
		return VALIDATOR.validate(transaction, new ValidationContext(DebitPredicates.True));
	}

	private static ValidationResult validate(final TransferTransaction transaction, final DebitPredicate debitPredicate) {
		return VALIDATOR.validate(transaction, new ValidationContext(debitPredicate));
	}
}