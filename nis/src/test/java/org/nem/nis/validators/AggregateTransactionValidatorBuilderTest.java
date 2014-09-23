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
		final ValidationContext context = Mockito.mock(ValidationContext.class);
		final TransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, context);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Mockito.verify(validator, Mockito.only()).validate(transaction, context);
	}

	@Test
	public void canAddMultipleValidators() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext context = Mockito.mock(ValidationContext.class);
		final TransactionValidator validator1 = createValidator(ValidationResult.SUCCESS);
		final TransactionValidator validator2 = createValidator(ValidationResult.SUCCESS);
		final TransactionValidator validator3 = createValidator(ValidationResult.SUCCESS);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator1);
		builder.add(validator2);
		builder.add(validator3);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, context);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator1, Mockito.only()).validate(transaction, context);
		Mockito.verify(validator2, Mockito.only()).validate(transaction, context);
		Mockito.verify(validator3, Mockito.only()).validate(transaction, context);
	}

	@Test
	public void validationShortCircuitsOnFirstSubValidatorFailure() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext context = Mockito.mock(ValidationContext.class);
		final TransactionValidator validator1 = createValidator(ValidationResult.SUCCESS);
		final TransactionValidator validator2 = createValidator(ValidationResult.FAILURE_CHAIN_INVALID);
		final TransactionValidator validator3 = createValidator(ValidationResult.SUCCESS);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator1);
		builder.add(validator2);
		builder.add(validator3);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, context);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		Mockito.verify(validator1, Mockito.only()).validate(transaction, context);
		Mockito.verify(validator2, Mockito.only()).validate(transaction, context);
		Mockito.verify(validator3, Mockito.never()).validate(transaction, context);
	}

	private static TransactionValidator createValidator(final ValidationResult result) {
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		return validator;
	}
}