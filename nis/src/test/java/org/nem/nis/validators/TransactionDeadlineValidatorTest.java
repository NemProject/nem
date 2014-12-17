package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockTransaction;
import org.nem.core.time.*;

public class TransactionDeadlineValidatorTest {
	private static TimeProvider TIME_PROVIDER = new SystemTimeProvider();
	private static final TransactionDeadlineValidator VALIDATOR = new TransactionDeadlineValidator(TIME_PROVIDER);

	@Test
	public void validateReturnsSuccessIfTransactionHasNotExpired() {
		// Arrange:
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(TIME_PROVIDER.getCurrentTime());

		// Act:
		ValidationResult result = VALIDATOR.validate(transaction, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void validateReturnsFailureIfTransactionHasExpired() {
		// Arrange:
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(TIME_PROVIDER.getCurrentTime().addSeconds(-1));

		// Act:
		ValidationResult result = VALIDATOR.validate(transaction, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}
}
