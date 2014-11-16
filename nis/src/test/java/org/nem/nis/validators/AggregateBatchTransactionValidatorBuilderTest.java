package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;

import java.util.*;

public class AggregateBatchTransactionValidatorBuilderTest {

	/*
	 * tests are shamelessly stolen from AggregateBatchTransactionValidatorBuilderTest
	 */
	
	//region delegation

	//region canAggregateOneValidator
	@Test
	public void canAggregateOneValidatorAndCallAsBatchValidator() {
		// Assert:
		assertCanAggregateOneValidator(new BatchValidationPolicy());
	}

	private static void assertCanAggregateOneValidator(final ValidationPolicy policy) {
		// Arrange:
		final BatchTransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final BatchTransactionValidator aggregate = builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		policy.assertCalledOnce(validator);
	}

	//endregion

	//region canAggregateMultipleValidators

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//region aggregateShortCircuitsOnFirstSubValidatorFailure

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertOnlyFirstTwoValidatorsCalledOnce();
	}

	//endregion

	//region aggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//region aggregateGivesHigherPrecedenceToFailureResultThanNeutralResult

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
		final BatchTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//endregion

	//region mapping

	//region BatchTransactionValidator mapping

	@Test
	public void batchValidationDelegatesToBatchTransactionValidator() {
		// Arrange:
		final TransactionsContextPair transactionsContextPair = Mockito.mock(TransactionsContextPair.class);
		final List<TransactionsContextPair> transactions = Arrays.asList(transactionsContextPair);

		final BatchTransactionValidator validator = createValidator(ValidationResult.SUCCESS);
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final BatchTransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(validator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		Assert.assertThat(pair, IsSame.sameInstance(transactionsContextPair));
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<List<TransactionsContextPair>> createPairsCaptor() {
		return ArgumentCaptor.forClass((Class)List.class);
	}

	//endregion

	//endregion

	private static BatchTransactionValidator createValidator(final ValidationResult result) {
		final BatchTransactionValidator validator = Mockito.mock(BatchTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}

	//region ThreeSubValidatorTestContext

	private static class ThreeSubValidatorTestContext {
		private final ValidationPolicy policy;
		private final BatchTransactionValidator validator1;
		private final BatchTransactionValidator validator2;
		private final BatchTransactionValidator validator3;
		private final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();

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
		public ValidationResult validate(final BatchTransactionValidator validator);

		public void assertCalledOnce(final BatchTransactionValidator validator);

		public void assertCalledNever(final BatchTransactionValidator validator);
	}

	private static class BatchValidationPolicy implements ValidationPolicy {
		private final TransactionsContextPair transactionsContextPair = Mockito.mock(TransactionsContextPair.class);
		private final List<TransactionsContextPair> transactions = Arrays.asList(transactionsContextPair);

		@Override
		public ValidationResult validate(final BatchTransactionValidator validator) {
			return validator.validate(transactions);
		}

		@Override
		public void assertCalledOnce(final BatchTransactionValidator validator) {
			Mockito.verify(validator, Mockito.only()).validate(transactions);
		}

		@Override
		public void assertCalledNever(final BatchTransactionValidator validator) {
			Mockito.verify(validator, Mockito.never()).validate(transactions);
		}
	}

	//endregion
}