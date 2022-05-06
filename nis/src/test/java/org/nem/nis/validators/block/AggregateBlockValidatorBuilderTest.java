package org.nem.nis.validators.block;

import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.*;
import org.nem.nis.validators.*;

public class AggregateBlockValidatorBuilderTest
		extends
			AggregateValidatorBuilderTest<AggregateBlockValidatorBuilder, BlockValidator, Block> {

	@Override
	public AggregateBlockValidatorBuilder createBuilder() {
		return new AggregateBlockValidatorBuilder();
	}

	@Override
	public BlockValidator createValidator(final ValidationResult result) {
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}

	@Override
	public Block createParam() {
		return Mockito.mock(Block.class);
	}

	@Override
	public void add(final AggregateBlockValidatorBuilder builder, final BlockValidator validator) {
		builder.add(validator);
	}

	@Override
	public BlockValidator build(final AggregateBlockValidatorBuilder builder) {
		return builder.build();
	}

	@Override
	public ValidationResult validate(final BlockValidator validator, final Block param) {
		return validator.validate(param);
	}

	@Override
	public void verifyValidate(final BlockValidator validator, final Block param, final VerificationMode verificationMode) {
		Mockito.verify(validator, verificationMode).validate(param);
	}
}
