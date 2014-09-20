package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.MockTransaction;

public class UniversalTransactionValidatorTest {
	private static final TransactionValidator VALIDATOR = new UniversalTransactionValidator();

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithLessThanMinimumDeadlineIsInvalid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp());

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void transactionWithMinimumDeadlineIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithMaximumDeadlineIsValid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1).addSeconds(1));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
	}

	@Test
	public void transactionWithSignerBalanceLessThanFeeIsInvalid() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));
		transaction.setFee(transaction.getSigner().getBalance().add(Amount.fromNem(1)));

		// Assert:
		Assert.assertThat(VALIDATOR.validate(transaction), IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
	}

	@Test
	public void debitPredicateHasPrecedenceOverSignerBalanceCheck() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));
		transaction.setFee(transaction.getSigner().getBalance().add(Amount.fromNem(1)));

		// Assert:
		Assert.assertThat(
				VALIDATOR.validate(transaction, (account, amount) -> true),
				IsEqual.equalTo(ValidationResult.SUCCESS));
	}
}