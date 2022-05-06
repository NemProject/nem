package org.nem.nis.validators.unconfirmed;

import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.ValidationResult;
import org.nem.nis.validators.*;

import java.util.*;

public class AggregateBatchTransactionValidatorBuilderTest
		extends
			AggregateValidatorBuilderTest<AggregateBatchTransactionValidatorBuilder, BatchTransactionValidator, List<TransactionsContextPair>> {

	@Override
	public AggregateBatchTransactionValidatorBuilder createBuilder() {
		return new AggregateBatchTransactionValidatorBuilder();
	}

	@Override
	public BatchTransactionValidator createValidator(final ValidationResult result) {
		final BatchTransactionValidator validator = Mockito.mock(BatchTransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}

	@Override
	public List<TransactionsContextPair> createParam() {
		return new ArrayList<>();
	}

	@Override
	public void add(final AggregateBatchTransactionValidatorBuilder builder, final BatchTransactionValidator validator) {
		builder.add(validator);
	}

	@Override
	public BatchTransactionValidator build(final AggregateBatchTransactionValidatorBuilder builder) {
		return builder.build();
	}

	@Override
	public ValidationResult validate(final BatchTransactionValidator validator, final List<TransactionsContextPair> param) {
		return validator.validate(param);
	}

	@Override
	public void verifyValidate(final BatchTransactionValidator validator, final List<TransactionsContextPair> param,
			final VerificationMode verificationMode) {
		Mockito.verify(validator, verificationMode).validate(param);
	}
}
