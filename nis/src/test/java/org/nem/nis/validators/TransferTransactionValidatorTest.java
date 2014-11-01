package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransferTransactionValidatorTest {
	private static final SingleTransactionValidator VALIDATOR = new TransferTransactionValidator();

	//region predicate precedence

	@Test
	public void validateGivesPrecedenceToFailingCanDebitPredicate() {
		// Arrange: (sender-balance == amount + fee)
		final Transaction transaction = this.createTransaction(2, 1, 1);

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(
				VALIDATOR.validate(transaction, new ValidationContext((account, amount) -> false)),
				IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
	}

	@Test
	public void validateGivesPrecedenceToSucceedingCanDebitPredicate() {
		// Arrange: (sender-balance < amount + fee)
		final Transaction transaction = this.createTransaction(2, 2, 1);

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(
				VALIDATOR.validate(transaction, new ValidationContext((account, amount) -> true)),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region amounts

	@Test
	public void transactionsWithNonNegativeAmountAreValid() {
		// Assert:
		final ValidationResult expectedResult = ValidationResult.SUCCESS;
		Assert.assertThat(this.isTransactionAmountValid(100, 0, 1), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 1, 10), IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transactionsUpToSignerBalanceAreValid() {
		// Assert:
		final ValidationResult expectedResult = ValidationResult.SUCCESS;
		Assert.assertThat(this.isTransactionAmountValid(100, 10, 1), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 990, 10), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 50, 950), IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transactionsExceedingSignerBalanceAreInvalid() {
		// Assert:
		final ValidationResult expectedResult = ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		Assert.assertThat(this.isTransactionAmountValid(1000, 990, 11), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 51, 950), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 1001, 11), IsEqual.equalTo(expectedResult));
		Assert.assertThat(this.isTransactionAmountValid(1000, 51, 1001), IsEqual.equalTo(expectedResult));
	}

	private TransferTransaction createTransaction(final int senderBalance, final int amount, final int fee) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(senderBalance));
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, amount, null);
		transaction.setFee(Amount.fromNem(fee));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));
		return transaction;
	}

	private ValidationResult isTransactionAmountValid(final int senderBalance, final int amount, final int fee) {
		// Arrange:
		final TransferTransaction transaction = this.createTransaction(senderBalance, amount, fee);

		// Act:
		return VALIDATOR.validate(transaction);
	}

	//endregion

	//region messages

	@Test
	public void smallMessagesAreValid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(0), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(1), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(511), IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(this.isMessageSizeValid(512), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		Assert.assertThat(this.isMessageSizeValid(513), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
		Assert.assertThat(this.isMessageSizeValid(1001), IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
	}

	private ValidationResult isMessageSizeValid(final int messageSize) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(1000));
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, 1, message);
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Act:
		return VALIDATOR.validate(transaction);
	}

	//endregion

	//region other type

	@Test
	public void otherTransactionTypesPassValidation() {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final MockTransaction transaction = new MockTransaction(account);
		transaction.setFee(Amount.fromNem(200));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	private static TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount, final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message);
	}
}