package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

public class TransferTransactionValidatorTest {
	private static final TSingleTransactionValidator<TransferTransaction> VALIDATOR = new TransferTransactionValidator();
	private static final int MAX_MESSAGE_SIZE = 160;

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

	//region messages

	@Test
	public void smallMessagesAreValid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(0), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(MAX_MESSAGE_SIZE - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(MAX_MESSAGE_SIZE), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(MAX_MESSAGE_SIZE + 1), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
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
		return VALIDATOR.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}
}