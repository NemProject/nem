package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class MultisigAwareSingleTransactionValidatorTest {

	//region non-multisig transaction

	@Test
	public void validateDelegatesToInnerValidatorForValidNonMultisigTransaction() {
		// Assert:
		assertValidateDelegatesToInnerValidatorForNonMultisigTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void validateDelegatesToInnerValidatorForInvalidNonMultisigTransaction() {
		// Assert:
		assertValidateDelegatesToInnerValidatorForNonMultisigTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertValidateDelegatesToInnerValidatorForNonMultisigTransaction(final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		final MultisigAwareSingleTransactionValidator validator = new MultisigAwareSingleTransactionValidator(innerValidator);

		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		final ValidationContext context = new ValidationContext((account, amount) -> false);
		Mockito.when(innerValidator.validate(transaction, context)).thenReturn(expectedResult);

		// Act:
		final ValidationResult result = validator.validate(transaction, context);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		Mockito.verify(innerValidator, Mockito.only()).validate(transaction, context);
	}

	//endregion

	//region multisig transaction

	@Test
	public void validateSucceedsForValidMultisigTransactionWithValidInnerTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.setMultisigTransactionValidationResult(ValidationResult.SUCCESS);
		context.setInnerTransactionValidationResult(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.validate();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		context.verifyMultisigTransactionValidation(Mockito.times(1));
		context.verifyInnerTransactionValidation(Mockito.times(1));
	}

	@Test
	public void validateFailsForInvalidMultisigTransactionWithValidInnerTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.setMultisigTransactionValidationResult(ValidationResult.FAILURE_ENTITY_UNUSABLE);
		context.setInnerTransactionValidationResult(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.validate();

		// Assert: validation is short-circuited on multisig transaction validation failure
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
		context.verifyMultisigTransactionValidation(Mockito.times(1));
		context.verifyInnerTransactionValidation(Mockito.never());
	}

	@Test
	public void validateFailsForValidMultisigTransactionWithInvalidInnerTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.setMultisigTransactionValidationResult(ValidationResult.SUCCESS);
		context.setInnerTransactionValidationResult(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		final ValidationResult result = context.validate();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		context.verifyMultisigTransactionValidation(Mockito.times(1));
		context.verifyInnerTransactionValidation(Mockito.times(1));
	}

	@Test
	public void validateFailsForInvalidMultisigTransactionWithInvalidInnerTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		context.setMultisigTransactionValidationResult(ValidationResult.FAILURE_ENTITY_UNUSABLE);
		context.setInnerTransactionValidationResult(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		final ValidationResult result = context.validate();

		// Assert: validation is short-circuited on multisig transaction validation failure
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
		context.verifyMultisigTransactionValidation(Mockito.times(1));
		context.verifyInnerTransactionValidation(Mockito.never());
	}

	private static class MultisigTestContext {
		private final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		private final MultisigAwareSingleTransactionValidator validator = new MultisigAwareSingleTransactionValidator(innerValidator);

		private final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		private final MultisigTransaction multisigTransaction = new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), innerTransaction);
		private final ValidationContext context = new ValidationContext((account, amount) -> false);

		private void setMultisigTransactionValidationResult(final ValidationResult result) {
			Mockito.when(this.innerValidator.validate(this.multisigTransaction, this.context)).thenReturn(result);
		}

		private void setInnerTransactionValidationResult(final ValidationResult result) {
			Mockito.when(this.innerValidator.validate(this.innerTransaction, this.context)).thenReturn(result);
		}

		private ValidationResult validate() {
			return this.validator.validate(this.multisigTransaction, this.context);
		}

		private void verifyMultisigTransactionValidation(final VerificationMode mode) {
			Mockito.verify(this.innerValidator, mode).validate(this.multisigTransaction, this.context);
		}

		private void verifyInnerTransactionValidation(final VerificationMode mode) {
			Mockito.verify(this.innerValidator, mode).validate(this.innerTransaction, this.context);
		}
	}

	//endregion
}