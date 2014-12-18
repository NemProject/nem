package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.*;

public class TransactionDeadlineValidatorTest {

	@Test
	public void validateReturnsSuccessIfTransactionHasNotExpired() {
		// Assert:
		assertValidationResult(1, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsSuccessIfTransactionHasNotExpiredAndDeadlineIsEqualToTimeStamp() {
		// Assert:
		assertValidationResult(0, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsFailureIfTransactionHasExpired() {
		// Assert:
		assertValidationResult(-1, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	private static void assertValidationResult(final int relativeDeadline, final ValidationResult expectedResult) {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(100);
		final TransactionDeadlineValidator validator = new TransactionDeadlineValidator(timeProvider);

		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(100 + relativeDeadline));

		// Act:
		final ValidationResult result = validator.validate(transaction, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
