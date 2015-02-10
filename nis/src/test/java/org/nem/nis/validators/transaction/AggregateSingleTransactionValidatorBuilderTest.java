package org.nem.nis.validators.transaction;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.validators.*;

import java.util.*;

public class AggregateSingleTransactionValidatorBuilderTest {

	/**
	 * Note that the delegation tests have a few more layers than necessary so that they
	 * can be easily updated if an AggregateBatchTransactionValidatorBuilder is ever needed.
	 */

	//region delegation

	//region canAggregateOneValidator
	@Test
	public void canAggregateOneValidatorAndCallAsSingleValidator() {
		// Assert:
		assertCanAggregateOneValidator(new SingleValidationPolicy());
	}

	private static void assertCanAggregateOneValidator(final ValidationPolicy policy) {
		// Arrange:
		final SingleTransactionValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final SingleTransactionValidator aggregate = builder.build();
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

	private static void assertCanAggregateMultipleValidators(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final SingleTransactionValidator aggregate = context.builder.build();
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

	private static void assertAggregateShortCircuitsOnFirstSubValidatorFailure(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_CHAIN_INVALID,
				ValidationResult.SUCCESS);

		// Act:
		final SingleTransactionValidator aggregate = context.builder.build();
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

	private static void assertAggregateDoesNotShortCircuitOnFirstSubValidatorNeutralResult(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.SUCCESS);

		// Act:
		final SingleTransactionValidator aggregate = context.builder.build();
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

	private static void assertAggregateGivesHigherPrecedenceToFailureResultThanNeutralResult(final ValidationPolicy policy) {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				policy,
				ValidationResult.SUCCESS,
				ValidationResult.NEUTRAL,
				ValidationResult.FAILURE_CHAIN_INVALID);

		// Act:
		final SingleTransactionValidator aggregate = context.builder.build();
		final ValidationResult result = policy.validate(aggregate);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	//endregion

	//endregion

	//region mapping

	//region SingleTransactionValidator mapping

	@Test
	public void singleValidationDelegatesToSingleTransactionValidator() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		final SingleTransactionValidator validator = createValidator(ValidationResult.SUCCESS);
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final SingleTransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator, Mockito.only()).validate(transaction, validationContext);
	}

	//endregion

	//region BatchTransactionValidator mapping

	@Test
	public void singleValidationDelegatesToBatchTransactionValidator() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		final BatchTransactionValidator validator = createBatchValidator(ValidationResult.SUCCESS);
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final SingleTransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(validator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		Assert.assertThat(pair.getContext(), IsSame.sameInstance(validationContext));
		Assert.assertThat(pair.getTransactions(), IsEquivalent.equivalentTo(Arrays.asList(transaction)));
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<List<TransactionsContextPair>> createPairsCaptor() {
		return ArgumentCaptor.forClass((Class)List.class);
	}

	//endregion

	//endregion

	private static SingleTransactionValidator createValidator(final ValidationResult result) {
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		return validator;
	}

	private static BatchTransactionValidator createBatchValidator(final ValidationResult result) {
		final BatchTransactionValidator validator = Mockito.mock(BatchTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}

	//region ThreeSubValidatorTestContext

	private static class ThreeSubValidatorTestContext {
		private final ValidationPolicy policy;
		private final SingleTransactionValidator validator1;
		private final SingleTransactionValidator validator2;
		private final SingleTransactionValidator validator3;
		private final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

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
		public ValidationResult validate(final SingleTransactionValidator validator);

		public void assertCalledOnce(final SingleTransactionValidator validator);

		public void assertCalledNever(final SingleTransactionValidator validator);
	}

	private static class SingleValidationPolicy implements ValidationPolicy {
		private final Transaction transaction = Mockito.mock(Transaction.class);
		private final ValidationContext validationContext = Mockito.mock(ValidationContext.class);

		@Override
		public ValidationResult validate(final SingleTransactionValidator validator) {
			return validator.validate(this.transaction, this.validationContext);
		}

		@Override
		public void assertCalledOnce(final SingleTransactionValidator validator) {
			Mockito.verify(validator, Mockito.only()).validate(this.transaction, this.validationContext);
		}

		@Override
		public void assertCalledNever(final SingleTransactionValidator validator) {
			Mockito.verify(validator, Mockito.never()).validate(this.transaction, this.validationContext);
		}
	}

	//endregion
}