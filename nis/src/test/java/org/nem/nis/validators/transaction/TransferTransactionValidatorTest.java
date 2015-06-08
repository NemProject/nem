package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

public class TransferTransactionValidatorTest {
	private static final TSingleTransactionValidator<TransferTransaction> VALIDATOR = new TransferTransactionValidator();
	private static final int OLD_MAX_MESSAGE_SIZE = 96;
	private static final int MAX_MESSAGE_SIZE = 160;
	private static final long FORK_HEIGHT = BlockMarkerConstants.MULTISIG_M_OF_N_FORK;


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
		Assert.assertThat(isMessageSizeValid(0), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE + 1), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(isMessageSizeValid(1001), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
	}

	//region increase of allowed message size at fork height

	@Test
	public void messagesWithSizeUpToOldMaximumAreValidBeforeForkHeight() {
		Assert.assertThat(isMessageSizeValid(0, 1L), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(1, 1L), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE - 1, 1L), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE, 1L), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(0, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(1, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE - 1, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void messagesWithSizeLargerThanOldMaximumAreInvalidBeforeForkHeight() {
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE + 1, 1L), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE + 10, 1L), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE + 1, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE + 10, FORK_HEIGHT - 1), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
	}

	@Test
	public void messagesWithSizeUpToNewMaximumAreValidAtForkHeight() {
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE - 1, FORK_HEIGHT), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE, FORK_HEIGHT), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void messagesWithSizeUpToNewMaximumAreValidAfterForkHeight() {
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE - 1, FORK_HEIGHT + 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE, FORK_HEIGHT + 1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(MAX_MESSAGE_SIZE - 1, FORK_HEIGHT + 10), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(isMessageSizeValid(OLD_MAX_MESSAGE_SIZE, FORK_HEIGHT + 10), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	private static ValidationResult isMessageSizeValid(final int messageSize) {
		return isMessageSizeValid(messageSize, Long.MAX_VALUE);
	}

	private static ValidationResult isMessageSizeValid(final int messageSize, final Long height) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, 1, message);
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Act:
		return validate(transaction, new BlockHeight(height));
	}

	//endregion

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount, final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message);
	}

	private static ValidationResult validate(final TransferTransaction transaction) {
		return validate(transaction, BlockHeight.MAX);
	}

	private static ValidationResult validate(final TransferTransaction transaction, final BlockHeight height) {
		return VALIDATOR.validate(transaction, new ValidationContext(height, DebitPredicates.Throw));
	}
}