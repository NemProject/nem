package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;

public class AggregateTransactionValidatorBuilderTest {

	@Test
	public void canAddSingleValidator() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		final TransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Mockito.verify(validator, Mockito.only()).validate(transaction, validationContext);
	}

	@Test
	public void canAddMultipleValidators() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.transaction, context.validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		context.assertAllValidatorsCalledOnce();
	}

	@Test
	public void validationShortCircuitsOnFirstSubValidatorFailure() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_CHAIN_INVALID,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.transaction, context.validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertOnlyFirstTwoValidatorsCalledOnce();
	}

	@Test
	public void validationDoesNotShortCircuitOnFirstSubValidatorNeutralResult() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.transaction, context.validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		context.assertAllValidatorsCalledOnce();
	}

	@Test
	public void validationFailureHasHigherPrecedenceThanNeutralResult() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.FAILURE_CHAIN_INVALID);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.transaction, context.validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	private static class ThreeSubValidatorTestContext {
		private final Transaction transaction = Mockito.mock(Transaction.class);
		private final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		private final TransactionValidator validator1;
		private final TransactionValidator validator2;
		private final TransactionValidator validator3;
		private final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		private ThreeSubValidatorTestContext(
				final ValidationResult result1,
				final ValidationResult result2,
				final ValidationResult result3) {
			this.validator1 = createValidator(result1);
			this.validator2 = createValidator(result2);
			this.validator3 = createValidator(result3);

			this.builder.add(this.validator1);
			this.builder.add(this.validator2);
			this.builder.add(this.validator3);
		}

		private void assertOnlyFirstTwoValidatorsCalledOnce() {
			Mockito.verify(this.validator1, Mockito.only()).validate(this.transaction, this.validationContext);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.transaction, this.validationContext);
			Mockito.verify(this.validator3, Mockito.never()).validate(this.transaction, this.validationContext);
		}

		private void assertAllValidatorsCalledOnce() {
			Mockito.verify(this.validator1, Mockito.only()).validate(this.transaction, this.validationContext);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.transaction, this.validationContext);
			Mockito.verify(this.validator3, Mockito.only()).validate(this.transaction, this.validationContext);
		}
	}

	private static TransactionValidator createValidator(final ValidationResult result) {
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		return validator;
	}
}