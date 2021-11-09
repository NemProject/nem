package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.ValidationContext;

public class TransferTransactionValidatorTest {
	private static final TSingleTransactionValidator<TransferTransaction> VALIDATOR = new TransferTransactionValidator();
	private static final int ORIGINAL_MAX_MESSAGE_SIZE = 96;
	private static final int MAX_MESSAGE_SIZE_MULTISIG_FORK = 160;
	private static final int CURRENT_MAX_MESSAGE_SIZE = 1024;
	private static final long FORK_HEIGHT_MULTISIG = BlockMarkerConstants
			.MULTISIG_M_OF_N_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);
	private static final long FORK_HEIGHT_REMOTE_ACCOUNT = BlockMarkerConstants
			.REMOTE_ACCOUNT_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);

	// region zero amount

	@Test
	public void transactionWithZeroAmountIsValid() {
		// Arrange:
		final TransferTransaction transaction = createTransaction(0, 1);

		// Act:
		final ValidationResult result = validate(transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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

	// endregion

	// region messages

	@Test
	public void smallMessagesAreValid() {
		// Assert:
		final int[] messageSizes = new int[]{
				0, 1, CURRENT_MAX_MESSAGE_SIZE - 1, CURRENT_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, ValidationResult.SUCCESS);
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		final int[] messageSizes = new int[]{
				CURRENT_MAX_MESSAGE_SIZE + 1, 10001
		};
		assertMessageSizesValidation(messageSizes, ValidationResult.FAILURE_MESSAGE_TOO_LARGE);
	}

	// region increase of allowed message size at multisig fork height - 1 <= height < multisig fork

	@Test
	public void messagesWithSizeUpToOriginalMaximumAreValidBeforeMultisigForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				0, 1, ORIGINAL_MAX_MESSAGE_SIZE - 1, ORIGINAL_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, 1L, ValidationResult.SUCCESS);
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_MULTISIG - 1, ValidationResult.SUCCESS);
	}

	@Test
	public void messagesWithSizeLargerThanOriginalMaximumAreInvalidBeforeMultisigForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				ORIGINAL_MAX_MESSAGE_SIZE + 1, ORIGINAL_MAX_MESSAGE_SIZE + 10, CURRENT_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, 1L, ValidationResult.FAILURE_MESSAGE_TOO_LARGE);
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_MULTISIG - 1, ValidationResult.FAILURE_MESSAGE_TOO_LARGE);
	}

	// endregion

	// region increase of allowed message size at multisig fork height - multisig fork <= height < remote account fork

	@Test
	public void messagesWithSizeUpToMultisigForkMaximumAreValidAtMultisigForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				MAX_MESSAGE_SIZE_MULTISIG_FORK, ORIGINAL_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_MULTISIG, ValidationResult.SUCCESS);
	}

	@Test
	public void messagesWithSizeUpToMultisigForkMaximumAreValidAfterMultisigForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				MAX_MESSAGE_SIZE_MULTISIG_FORK, ORIGINAL_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_MULTISIG + 1, ValidationResult.SUCCESS);
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_MULTISIG + 10, ValidationResult.SUCCESS);
	}

	// endregion

	// region increase of allowed message size at multisig fork height - remote account fork <= height

	@Test
	public void messagesWithSizeUpToRemoteAccountForkMaximumAreValidAtRemoteAccountForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				ORIGINAL_MAX_MESSAGE_SIZE, ORIGINAL_MAX_MESSAGE_SIZE + 10, MAX_MESSAGE_SIZE_MULTISIG_FORK,
				MAX_MESSAGE_SIZE_MULTISIG_FORK + 10, CURRENT_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_REMOTE_ACCOUNT, ValidationResult.SUCCESS);
	}

	@Test
	public void messagesWithSizeUpToRemoteAccountForkMaximumAreValidAfterRemoteAccountForkHeight() {
		// Assert:
		final int[] messageSizes = new int[]{
				CURRENT_MAX_MESSAGE_SIZE, ORIGINAL_MAX_MESSAGE_SIZE
		};
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_REMOTE_ACCOUNT + 1, ValidationResult.SUCCESS);
		assertMessageSizesValidation(messageSizes, FORK_HEIGHT_REMOTE_ACCOUNT + 10, ValidationResult.SUCCESS);
	}

	// endregion

	private static void assertMessageSizesValidation(final int[] messageSizes, final ValidationResult expectedResult) {
		// Assert:
		assertMessageSizesValidation(messageSizes, Long.MAX_VALUE, expectedResult);
	}

	private static void assertMessageSizesValidation(final int[] messageSizes, final Long height, final ValidationResult expectedResult) {
		// Assert:
		for (final int messageSize : messageSizes) {
			MatcherAssert.assertThat(String.format("message size: %d, height: %d", messageSize, height),
					isMessageSizeValid(messageSize, height), IsEqual.equalTo(expectedResult));
		}
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

	// endregion

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount,
			final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount),
				new TransferTransactionAttachment(message));
	}

	private static ValidationResult validate(final TransferTransaction transaction) {
		return validate(transaction, BlockHeight.MAX);
	}

	private static ValidationResult validate(final TransferTransaction transaction, final BlockHeight height) {
		return VALIDATOR.validate(transaction, new ValidationContext(height, ValidationStates.Throw));
	}
}
