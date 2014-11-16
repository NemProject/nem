package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;

import java.util.*;

public class AggregateBatchTransactionValidatorBuilderTest {
	@Test
	public void canAddSingleValidator() {
		// Arrange:
		final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
		final BatchTransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final BatchTransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(groupedTransactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Mockito.verify(validator, Mockito.only()).validate(groupedTransactions);
	}

	@Test
	public void canAddMultipleValidators() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.groupedTransactions);

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.groupedTransactions);

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.groupedTransactions);

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.groupedTransactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	private static class ThreeSubValidatorTestContext {
		private final List<TransactionsContextPair> groupedTransactions = new ArrayList<>();
		private final BatchTransactionValidator validator1;
		private final BatchTransactionValidator validator2;
		private final BatchTransactionValidator validator3;
		private final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();

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
			Mockito.verify(this.validator1, Mockito.only()).validate(this.groupedTransactions);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.groupedTransactions);
			Mockito.verify(this.validator3, Mockito.never()).validate(this.groupedTransactions);
		}

		private void assertAllValidatorsCalledOnce() {
			Mockito.verify(this.validator1, Mockito.only()).validate(this.groupedTransactions);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.groupedTransactions);
			Mockito.verify(this.validator3, Mockito.only()).validate(this.groupedTransactions);
		}
	}

	private static BatchTransactionValidator createValidator(final ValidationResult result) {
		final BatchTransactionValidator validator = Mockito.mock(BatchTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}
}