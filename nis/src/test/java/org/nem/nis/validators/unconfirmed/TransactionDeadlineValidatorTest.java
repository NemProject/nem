package org.nem.nis.validators.unconfirmed;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.*;

import java.util.Collections;

public class TransactionDeadlineValidatorTest {

	// region childless transaction

	@Test
	public void childlessTransactionWithFutureDeadlineValidates() {
		// Assert:
		assertValidationOfChildlessTransactionWithRelativeDeadline(1, ValidationResult.SUCCESS);
	}

	@Test
	public void childlessTransactionWithCurrentDeadlineValidates() {
		// Assert:
		assertValidationOfChildlessTransactionWithRelativeDeadline(0, ValidationResult.SUCCESS);
	}

	@Test
	public void childlessTransactionWithPastDeadlineDoesNotValidate() {
		// Assert:
		assertValidationOfChildlessTransactionWithRelativeDeadline(-1, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	private static void assertValidationOfChildlessTransactionWithRelativeDeadline(final int relativeDeadline,
			final ValidationResult expectedResult) {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(100);
		final TransactionDeadlineValidator validator = new TransactionDeadlineValidator(timeProvider);

		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(100 + relativeDeadline));

		// Act:
		final ValidationResult result = validator.validate(transaction, null);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region single child transaction (always valid child)

	@Test
	public void singleChildTransactionWithFutureDeadlineValidates() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeDeadline(1, ValidationResult.SUCCESS);
	}

	@Test
	public void singleChildTransactionWithCurrentDeadlineValidates() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeDeadline(0, ValidationResult.SUCCESS);
	}

	@Test
	public void singleChildTransactionWithPastDeadlineDoesNotValidate() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeDeadline(-1, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	private static void assertValidationOfSingleChildTransactionWithRelativeDeadline(final int relativeDeadline,
			final ValidationResult expectedResult) {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(100);
		final TransactionDeadlineValidator validator = new TransactionDeadlineValidator(timeProvider);

		final MockTransaction innerTransaction = new MockTransaction();
		innerTransaction.setDeadline(new TimeInstant(110));

		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(100 + relativeDeadline));
		transaction.setChildTransactions(Collections.singletonList(innerTransaction));

		// Act:
		final ValidationResult result = validator.validate(transaction, null);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region single child transaction (always valid root)

	@Test
	public void singleChildTransactionWithChildHavingFutureDeadlineValidates() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeChildDeadline(1, ValidationResult.SUCCESS);
	}

	@Test
	public void singleChildTransactionWithChildHavingCurrentDeadlineValidates() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeChildDeadline(0, ValidationResult.SUCCESS);
	}

	@Test
	public void singleChildTransactionWithChildHavingPastDeadlineDoesNotValidate() {
		// Assert:
		assertValidationOfSingleChildTransactionWithRelativeChildDeadline(-1, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	private static void assertValidationOfSingleChildTransactionWithRelativeChildDeadline(final int relativeDeadline,
			final ValidationResult expectedResult) {
		// Arrange:
		final TimeProvider timeProvider = Utils.createMockTimeProvider(100);
		final TransactionDeadlineValidator validator = new TransactionDeadlineValidator(timeProvider);

		final MockTransaction innerTransaction = new MockTransaction();
		innerTransaction.setDeadline(new TimeInstant(100 + relativeDeadline));

		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(110));
		transaction.setChildTransactions(Collections.singletonList(innerTransaction));

		// Act:
		final ValidationResult result = validator.validate(transaction, null);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion
}
