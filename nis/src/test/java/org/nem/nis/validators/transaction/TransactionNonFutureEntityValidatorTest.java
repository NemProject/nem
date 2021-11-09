package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ValidationResult;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

public class TransactionNonFutureEntityValidatorTest {

	@Test
	public void transactionWithTimeStampLessThanFutureThresholdIsValid() {
		// Assert:
		this.assertTransactionValidationResult(11, 20, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithTimeStampEqualToFutureThresholdIsValid() {
		// Assert:
		this.assertTransactionValidationResult(11, 21, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithTimeStampGreaterThanFutureThresholdIsNotValid() {
		// Assert:
		this.assertTransactionValidationResult(11, 22, ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE);
	}

	private void assertTransactionValidationResult(final int currentTime, final int entityTime, final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(7, new TimeInstant(entityTime));
		final SingleTransactionValidator validator = createValidator(currentTime);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static SingleTransactionValidator createValidator(final int currentTime) {
		return new TransactionNonFutureEntityValidator(Utils.createMockTimeProvider(currentTime));
	}
}
