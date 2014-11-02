package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;

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
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, validationContext);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator, Mockito.only()).validate(transaction, validationContext);
	}

	@Test
	public void batchValidationDelegatesToSingleTransactionValidator() {
		// Arrange:
		final BatchTransactionValidatorContext batchContext = new BatchTransactionValidatorContext();
		final SingleTransactionValidator validator = createValidator(ValidationResult.SUCCESS);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(batchContext.groupedTransactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any(), Mockito.any());
		Mockito.verify(validator, Mockito.times(1)).validate(batchContext.transactions1.get(0), batchContext.validationContext1);
		Mockito.verify(validator, Mockito.times(1)).validate(batchContext.transactions1.get(1), batchContext.validationContext1);
		Mockito.verify(validator, Mockito.times(1)).validate(batchContext.transactions2.get(0), batchContext.validationContext2);
	}

	@Test
	public void batchValidationDelegatesToSingleTransactionValidatorButStopsOnFirstFailure() {
		// Arrange:
		final BatchTransactionValidatorContext batchContext = new BatchTransactionValidatorContext();
		final SingleTransactionValidator validator = createValidator(ValidationResult.FAILURE_CHAIN_INVALID);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(batchContext.groupedTransactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		Mockito.verify(validator, Mockito.times(1)).validate(Mockito.any(), Mockito.any());
		Mockito.verify(validator, Mockito.times(1)).validate(batchContext.transactions1.get(0), batchContext.validationContext1);
		Mockito.verify(validator, Mockito.never()).validate(batchContext.transactions1.get(1), batchContext.validationContext1);
		Mockito.verify(validator, Mockito.never()).validate(batchContext.transactions2.get(0), batchContext.validationContext2);
	}

	//endregion

	//region BatchTransactionValidator mapping

	@Test
	public void singleValidationDelegatesToBatchTransactionValidator() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		final BatchTransactionValidator validator = createValidator(ValidationResult.SUCCESS);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
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

	@Test
	public void batchValidationDelegatesToBatchTransactionValidator() {
		// Arrange:
		final BatchTransactionValidatorContext batchContext = new BatchTransactionValidatorContext();
		final BatchTransactionValidator validator = createValidator(ValidationResult.SUCCESS);
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final TransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(batchContext.groupedTransactions);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator, Mockito.only()).validate(batchContext.groupedTransactions);
	}

	//endregion

	private static class BatchTransactionValidatorContext {
		final List<Transaction> transactions1 = Arrays.asList(Mockito.mock(Transaction.class), Mockito.mock(Transaction.class));
		final ValidationContext validationContext1 = Mockito.mock(ValidationContext.class);
		final List<Transaction> transactions2 = Arrays.asList(Mockito.mock(Transaction.class));
		final ValidationContext validationContext2 = Mockito.mock(ValidationContext.class);
		final List<TransactionsContextPair> groupedTransactions = Arrays.asList(
				new TransactionsContextPair(this.transactions1, this.validationContext1),
				new TransactionsContextPair(this.transactions2, this.validationContext2));
	}

	//endregion

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