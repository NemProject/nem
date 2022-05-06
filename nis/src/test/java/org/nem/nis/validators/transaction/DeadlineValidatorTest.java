package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.core.test.MockTransaction;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.function.Function;

public class DeadlineValidatorTest {

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addSeconds(726), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithLessThanMinimumDeadlineIsInvalid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts, ValidationResult.FAILURE_PAST_DEADLINE);
	}

	@Test
	public void transactionWithMinimumDeadlineIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addSeconds(1), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithMaximumDeadlineIsValid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addDays(1), ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
		// Assert:
		assertTimeStampDeadlineValidation(ts -> ts.addDays(1).addSeconds(1), ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTimeStampDeadlineValidation(final Function<TimeInstant, TimeInstant> getDeadlineFromTimeStamp,
			final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(getDeadlineFromTimeStamp.apply(transaction.getTimeStamp()));
		final SingleTransactionValidator validator = new DeadlineValidator();

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}
