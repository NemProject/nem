package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;

public class AggregateBlockValidatorBuilderTest {

	@Test
	public void canAddSingleValidator() {
		// Arrange:
		final Block block = Mockito.mock(Block.class);
		final BlockValidator validator = createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();

		// Act:
		builder.add(validator);
		final BlockValidator aggregate = builder.build();
		final ValidationResult result = aggregate.validate(block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Mockito.verify(validator, Mockito.only()).validate(block);
	}

	@Test
	public void canAddMultipleValidators() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final BlockValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.block);

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
		final BlockValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.block);

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
		final BlockValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.block);

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
		final BlockValidator aggregate = context.builder.build();
		final ValidationResult result = aggregate.validate(context.block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	private static class ThreeSubValidatorTestContext {
		private final Block block = Mockito.mock(Block.class);
		private final BlockValidator validator1;
		private final BlockValidator validator2;
		private final BlockValidator validator3;
		private final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();

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
			Mockito.verify(this.validator1, Mockito.only()).validate(this.block);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.block);
			Mockito.verify(this.validator3, Mockito.never()).validate(this.block);
		}

		private void assertAllValidatorsCalledOnce() {
			Mockito.verify(this.validator1, Mockito.only()).validate(this.block);
			Mockito.verify(this.validator2, Mockito.only()).validate(this.block);
			Mockito.verify(this.validator3, Mockito.only()).validate(this.block);
		}
	}

	private static BlockValidator createValidator(final ValidationResult result) {
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(result);
		return validator;
	}
}