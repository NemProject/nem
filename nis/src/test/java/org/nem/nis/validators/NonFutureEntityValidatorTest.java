package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.*;

public class NonFutureEntityValidatorTest {

	//region transactions

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

	private void assertTransactionValidationResult(
			final int currentTime,
			final int entityTime,
			final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(7, new TimeInstant(entityTime));
		final SingleTransactionValidator validator = createValidator(currentTime);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	//region blocks

	@Test
	public void blockWithTimeStampLessThanFutureThresholdIsValid() {
		// Assert:
		this.assertBlockValidationResult(11, 20, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithTimeStampEqualToFutureThresholdIsValid() {
		// Assert:
		this.assertBlockValidationResult(11, 21, ValidationResult.SUCCESS);
	}

	@Test
	public void blockWithTimeStampGreaterThanFutureThresholdIsNotValid() {
		// Assert:
		this.assertBlockValidationResult(11, 22, ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE);
	}

	private void assertBlockValidationResult(
			final int currentTime,
			final int entityTime,
			final ValidationResult expectedResult) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithTimeStamp(entityTime);
		final BlockValidator validator = createValidator(currentTime);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	//endregion

	private static NonFutureEntityValidator createValidator(final int currentTime) {
		return new NonFutureEntityValidator(Utils.createMockTimeProvider(currentTime));
	}
}