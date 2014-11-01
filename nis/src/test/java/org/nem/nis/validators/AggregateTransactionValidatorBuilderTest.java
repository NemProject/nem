package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;

import java.util.*;

public class AggregateTransactionValidatorBuilderTest {

	//region delegation

	//region canAggregateOneValidator

	@Test
	public void canAggregateOneValidatorAndCallAsSingleValidator() {
		// Assert:
		assertCanAggregateOneValidator(new SingleValidationPolicy());
	}

	@Test
	public void canAggregateOneValidatorAndCallAsBatchValidator() {
		// Assert:
		assertCanAggregateOneValidator(new BatchValidationPolicy());
	}

	private static void assertCanAggregateOneValidator(final ValidationPolicy policy) {
		// Arrange:
		final TransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		policy.assertCalledOnce(validator);
	}

	//endregion

	//region canAggregateMultipleValidators

	@Test
	public void canAggregateMultipleValidatorsAndCallAsSingleValidator() {
		// Assert:
		assertCanAggregateMultipleValidators(new SingleValidationPolicy());
	}

	@Test
	public void canAggregateMultipleValidatorsAndCallAsBatchValidator() {
		// Assert:
		assertCanAggregateMultipleValidators(new BatchValidationPolicy());
	}

	private static void assertCanAggregateMultipleValidators(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//region aggregateShortCircuitsOnFirstSubValidatorFailure

	@Test
	public void aggregateShortCircuitsOnFirstSubValidatorFailureWhenCalledAsSingleValidator() {
		// Assert:
		assertAggregateShortCircuitsOnFirstSubValidatorFailure(new SingleValidationPolicy());
	}

	@Test
	public void aggregateShortCircuitsOnFirstSubValidatorFailureWhenCalledAsBatchValidator() {
		// Assert:
		assertAggregateShortCircuitsOnFirstSubValidatorFailure(new BatchValidationPolicy());
	}

	private static void assertAggregateShortCircuitsOnFirstSubValidatorFailure(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_CHAIN_INVALID,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertOnlyFirstTwoValidatorsCalledOnce();
	}

	//endregion

	//region aggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult

	@Test
	public void aggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResultWhenCalledAsSingleValidator() {
		// Assert:
		assertAggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult(new SingleValidationPolicy());
	}

	@Test
	public void aggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResultWhenCalledAsBatchValidator() {
		// Assert:
		assertAggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult(new BatchValidationPolicy());
	}

	private static void assertAggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.SUCCESS);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//region aggregateGivesHigherPrecedenceToFailureResultThanNeutralResult

	@Test
	public void aggregateGivesHigherPrecedenceToFailureResultThanNeutralResultWhenCalledAsSingleValidator() {
		// Assert:
		assertAggregateGivesHigherPrecedenceToFailureResultThanNeutralResult(new SingleValidationPolicy());
	}

	@Test
	public void aggregateGivesHigherPrecedenceToFailureResultThanNeutralResultWhenCalledAsBatchValidator() {
		// Assert:
		assertAggregateGivesHigherPrecedenceToFailureResultThanNeutralResult(new BatchValidationPolicy());
	}

	private static void assertAggregateGivesHigherPrecedenceToFailureResultThanNeutralResult(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.FAILURE_CHAIN_INVALID);

		// Act:
		final TransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	private static TransactionValidator createValidator(final ValidationResult result) {
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		Mockito.when(((BatchTransactionValidator)validator).validate(Mockito.any())).thenReturn(result);
		return validator;
	}

	//region ThreeSubValidatorTestContext

	private static class ThreeSubValidatorTestContext {
		private final ValidationPolicy policy;
		private final TransactionValidator validator1;
		private final TransactionValidator validator2;
		private final TransactionValidator validator3;
		private final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		private ThreeSubValidatorTestContext(
				final ValidationPolicy policy,
				final ValidationResult result1,
				final ValidationResult result2,
				final ValidationResult result3) {
			this.policy = policy;
			this.validator1 = createValidator(result1);
			this.validator2 = createValidator(result2);
			this.validator3 = createValidator(result3);

			this.builder.add(this.validator1);
			this.builder.add(this.validator2);
			this.builder.add(this.validator3);
		}

		private void assertOnlyFirstTwoValidatorsCalledOnce() {
			this.policy.assertCalledOnce(this.validator1);
			this.policy.assertCalledOnce(this.validator2);
			this.policy.assertCalledNever(this.validator3);
		}

		private void assertAllValidatorsCalledOnce() {
			this.policy.assertCalledOnce(this.validator1);
			this.policy.assertCalledOnce(this.validator2);
			this.policy.assertCalledOnce(this.validator3);
		}
	}

	//endregion

	//region ValidationPolicy

	private static interface ValidationPolicy {
		public ValidationResult validate(final TransactionValidator validator);

		public void assertCalledOnce(final TransactionValidator validator);

		public void assertCalledNever(final TransactionValidator validator);
	}

	private static class BatchValidationPolicy implements ValidationPolicy {
		final List<TransactionsContextPair> transactionGroups = Arrays.asList(Mockito.mock(TransactionsContextPair.class));

		@Override
		public ValidationResult validate(final TransactionValidator validator) {
			return validator.validate(this.transactionGroups);
		}

		@Override
		public void assertCalledOnce(final TransactionValidator validator) {
			Mockito.verify(validator, Mockito.only()).validate(this.transactionGroups);
		}

		@Override
		public void assertCalledNever(final TransactionValidator validator) {
			Mockito.verify(validator, Mockito.never()).validate(this.transactionGroups);
		}
	}

	private static class SingleValidationPolicy implements ValidationPolicy {
		private final Transaction transaction = Mockito.mock(Transaction.class);
		private final ValidationContext validationContext = Mockito.mock(ValidationContext.class);

		@Override
		public ValidationResult validate(final TransactionValidator validator) {
			return validator.validate(this.transaction, this.validationContext);
		}

		@Override
		public void assertCalledOnce(final TransactionValidator validator) {
			Mockito.verify(validator, Mockito.only()).validate(this.transaction, this.validationContext);
		}

		@Override
		public void assertCalledNever(final TransactionValidator validator) {
			Mockito.verify(validator, Mockito.never()).validate(this.transaction, this.validationContext);
		}
	}

	//endregion
}