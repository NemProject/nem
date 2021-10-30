package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.*;

public class AggregateSingleTransactionValidatorBuilderTest
		extends
			AggregateValidatorBuilderTest<AggregateSingleTransactionValidatorBuilder, SingleTransactionValidator, Transaction> {

	// region AggregateValidatorBuilderTest

	@Override
	public AggregateSingleTransactionValidatorBuilder createBuilder() {
		return new AggregateSingleTransactionValidatorBuilder();
	}

	@Override
	protected SingleTransactionValidator createValidator(final ValidationResult result) {
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		return validator;
	}

	@Override
	public Transaction createParam() {
		return Mockito.mock(Transaction.class);
	}

	@Override
	public void add(final AggregateSingleTransactionValidatorBuilder builder, final SingleTransactionValidator validator) {
		builder.add(validator);
	}

	@Override
	public SingleTransactionValidator build(final AggregateSingleTransactionValidatorBuilder builder) {
		return builder.build();
	}

	@Override
	public ValidationResult validate(final SingleTransactionValidator validator, final Transaction transaction) {
		return validator.validate(transaction, new ValidationContext(ValidationStates.Throw));
	}

	@Override
	public void verifyValidate(final SingleTransactionValidator validator, final Transaction transaction,
			final VerificationMode verificationMode) {
		Mockito.verify(validator, verificationMode).validate(Mockito.eq(transaction), Mockito.any());
	}

	// endregion

	// region SingleTransactionValidator mapping

	@Test
	public void singleValidationDelegatesToSingleTransactionValidator() {
		// Arrange:
		final Transaction transaction = Mockito.mock(Transaction.class);
		final ValidationContext validationContext = Mockito.mock(ValidationContext.class);
		final SingleTransactionValidator validator = this.createValidator(ValidationResult.SUCCESS);
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();

		// Act:
		builder.add(validator);
		final SingleTransactionValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(transaction, validationContext);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(validator, Mockito.only()).validate(transaction, validationContext);
	}

	// endregion

	// region BatchTransactionValidator mapping

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
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(validator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		MatcherAssert.assertThat(pair.getContext(), IsSame.sameInstance(validationContext));
		MatcherAssert.assertThat(pair.getTransactions(), IsEquivalent.equivalentTo(Collections.singletonList(transaction)));
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private static ArgumentCaptor<List<TransactionsContextPair>> createPairsCaptor() {
		return ArgumentCaptor.forClass((Class) List.class);
	}

	// endregion

	private static BatchTransactionValidator createBatchValidator(final ValidationResult result) {
		final BatchTransactionValidator validator = Mockito.mock(BatchTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}
}
