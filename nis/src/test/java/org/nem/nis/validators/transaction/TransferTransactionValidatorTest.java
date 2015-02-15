package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.*;

public class TransferTransactionValidatorTest {
	private static final TSingleTransactionValidator<TransferTransaction> VALIDATOR = new TransferTransactionValidator();

	//region predicate delegation

	@Test
	public void validatorDelegatesToDebitPredicateWithFeeAndAmountAndUsesResultWhenDebitPredicateSucceeds() {
		// Assert:
		this.assertDebitPredicateDelegation(true, ValidationResult.SUCCESS);
	}

	@Test
	public void validatorDelegatesToDebitPredicateWithFeeAndAmountAndUsesResultWhenDebitPredicateFails() {
		// Assert:
		this.assertDebitPredicateDelegation(false, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private void assertDebitPredicateDelegation(final boolean predicateResult, final ValidationResult expectedValidationResult) {
		// Arrange:
		final TransferTransaction transaction = createTransaction(12, 7);

		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any())).thenReturn(predicateResult);

		// Act:
		final ValidationResult result = validate(transaction, debitPredicate);

		// Assert:
		Mockito.verify(debitPredicate, Mockito.only()).canDebit(transaction.getSigner(), Amount.fromNem(19));
		Assert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
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