package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.*;

public class ChildAwareSingleTransactionValidatorTest {

	// region getName

	@Test
	public void getNameDelegatesToInnerValidator() {
		// Arrange:
		final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		final SingleTransactionValidator validator = new ChildAwareSingleTransactionValidator(innerValidator);
		Mockito.when(innerValidator.getName()).thenReturn("inner");

		// Act:
		final String name = validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("inner"));
		Mockito.verify(innerValidator, Mockito.only()).getName();
	}

	// endregion

	// region transaction without child transactions

	@Test
	public void validateDelegatesToInnerValidatorForValidTransactionWithoutChildTransactions() {
		// Assert:
		assertValidateDelegatesToInnerValidatorForTransactionWithoutChildTransactions(ValidationResult.SUCCESS);
	}

	@Test
	public void validateDelegatesToInnerValidatorForInvalidTransactionWithoutChildTransactions() {
		// Assert:
		assertValidateDelegatesToInnerValidatorForTransactionWithoutChildTransactions(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertValidateDelegatesToInnerValidatorForTransactionWithoutChildTransactions(
			final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		final SingleTransactionValidator validator = new ChildAwareSingleTransactionValidator(innerValidator);

		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		final ValidationContext context = new ValidationContext(ValidationStates.Throw);
		Mockito.when(innerValidator.validate(transaction, context)).thenReturn(expectedResult);

		// Act:
		final ValidationResult result = validator.validate(transaction, context);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		Mockito.verify(innerValidator, Mockito.only()).validate(transaction, context);
	}

	// endregion

	// region transaction with child transactions

	@Test
	public void validateSucceedsForValidTransactionWithValidChildTransactions() {
		// Arrange:
		final ThreeChildTransactionTestContext context = new ThreeChildTransactionTestContext();
		context.setOuterTransactionValidationResult(ValidationResult.SUCCESS);
		context.setInnerTransactionValidationResult(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.validate();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		context.verifyValidation(true, true, true, true);
	}

	@Test
	public void validateFailsForInvalidTransactionWithValidChildTransactions() {
		// Arrange:
		final ThreeChildTransactionTestContext context = new ThreeChildTransactionTestContext();
		context.setOuterTransactionValidationResult(ValidationResult.FAILURE_UNKNOWN);
		context.setInnerTransactionValidationResult(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.validate();

		// Assert: validation is short-circuited on outer transaction validation failure
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		context.verifyValidation(true, false, false, false);
	}

	@Test
	public void validateFailsForValidTransactionWithAtLeastOneInvalidChildTransaction() {
		// Arrange:
		final ThreeChildTransactionTestContext context = new ThreeChildTransactionTestContext();
		context.setOuterTransactionValidationResult(ValidationResult.SUCCESS);
		context.setInnerTransactionValidationResult(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		final ValidationResult result = context.validate();

		// Assert: validation is short-circuited on first inner transaction validation failure
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		context.verifyValidation(true, true, true, false);
	}

	@Test
	public void validateFailsForInvalidTransactionWithAtLeastOneInvalidChildTransaction() {
		// Arrange:
		final ThreeChildTransactionTestContext context = new ThreeChildTransactionTestContext();
		context.setOuterTransactionValidationResult(ValidationResult.FAILURE_UNKNOWN);
		context.setInnerTransactionValidationResult(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		final ValidationResult result = context.validate();

		// Assert: validation is short-circuited on outer transaction validation failure
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		context.verifyValidation(true, false, false, false);
	}

	private static class ThreeChildTransactionTestContext {
		private final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		private final SingleTransactionValidator validator = new ChildAwareSingleTransactionValidator(this.innerValidator);

		private final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		private final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		private final Transaction innerTransaction3 = new MockTransaction(Utils.generateRandomAccount());
		private final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		private final ValidationContext context = new ValidationContext(ValidationStates.Throw);

		private ThreeChildTransactionTestContext() {
			final Collection<Transaction> transactions = Arrays.asList(this.innerTransaction1, this.innerTransaction2,
					this.innerTransaction3);
			for (final Transaction transaction : transactions) {
				Mockito.when(this.innerValidator.validate(transaction, this.context)).thenReturn(ValidationResult.SUCCESS);
			}

			this.transaction.setChildTransactions(transactions);
		}

		private void setOuterTransactionValidationResult(final ValidationResult result) {
			Mockito.when(this.innerValidator.validate(this.transaction, this.context)).thenReturn(result);
		}

		private void setInnerTransactionValidationResult(final ValidationResult result) {
			Mockito.when(this.innerValidator.validate(this.innerTransaction2, this.context)).thenReturn(result);
		}

		private ValidationResult validate() {
			return this.validator.validate(this.transaction, this.context);
		}

		private void verifyValidation(final boolean isOuterValidated, final boolean isInner1Validated, final boolean isInner2Validated,
				final boolean isInner3Validated) {
			Mockito.verify(this.innerValidator, getModeFromFlag(isOuterValidated)).validate(this.transaction, this.context);
			Mockito.verify(this.innerValidator, getModeFromFlag(isInner1Validated)).validate(this.innerTransaction1, this.context);
			Mockito.verify(this.innerValidator, getModeFromFlag(isInner2Validated)).validate(this.innerTransaction2, this.context);
			Mockito.verify(this.innerValidator, getModeFromFlag(isInner3Validated)).validate(this.innerTransaction3, this.context);
		}

		private static VerificationMode getModeFromFlag(final boolean isValidated) {
			return isValidated ? Mockito.times(1) : Mockito.never();
		}
	}

	// endregion
}
