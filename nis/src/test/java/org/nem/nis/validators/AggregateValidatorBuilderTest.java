package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.model.ValidationResult;

/**
 * Base class for aggregate validator builder tests.
 */
public abstract class AggregateValidatorBuilderTest<TBuilder, TValidator, TParam> {

	//region protected abstract members

	/**
	 * Creates a validator builder.
	 *
	 * @return The builder.
	 */
	public abstract TBuilder createBuilder();

	/**
	 * Creates a validator with the specified result.
	 *
	 * @param result The specified result.
	 * @return The validator.
	 */
	public abstract TValidator createValidator(final ValidationResult result);

	/**
	 * Creates a validator parameter.
	 *
	 * @return The parameter.
	 */
	public abstract TParam createParam();

	/**
	 * Adds a validator to a builder.
	 *
	 * @param builder The builder.
	 * @param validator The validator.
	 */
	public abstract void add(final TBuilder builder, final TValidator validator);

	/**
	 * Creates an aggregate validator using a builder.
	 *
	 * @param builder The builder.
	 * @return The aggregate validator.
	 */
	public abstract TValidator build(final TBuilder builder);

	/**
	 * Calls validate on a validator.
	 *
	 * @param validator The validator.
	 * @param param The parameter.
	 * @return The validation result.
	 */
	public abstract ValidationResult validate(final TValidator validator, final TParam param);

	/**
	 * Asserts validate was called on a validator.
	 *
	 * @param validator The validator.
	 * @param param The parameter.
	 * @param verificationMode The verification mode.
	 */
	public abstract void verifyValidate(final TValidator validator, final TParam param, final VerificationMode verificationMode);

	//endregion

	@Test
	public void canAddSingleValidator() {
		// Arrange:
		final TParam param = this.createParam();
		final TValidator validator = this.createValidator(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final TBuilder builder = this.createBuilder();

		// Act:
		this.add(builder, validator);
		final TValidator aggregate = this.build(builder);
		final ValidationResult result = this.validate(aggregate, param);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		this.verifyValidate(validator, param, Mockito.only());
	}

	@Test
	public void canAddMultipleValidators() {
		// Arrange:
		final ThreeSubValidatorTestContext context = new ThreeSubValidatorTestContext(
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS,
				ValidationResult.SUCCESS);

		// Act:
		final TValidator aggregate = this.build(context.builder);
		final ValidationResult result = this.validate(aggregate, context.param);

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
		final TValidator aggregate = this.build(context.builder);
		final ValidationResult result = this.validate(aggregate, context.param);

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
		final TValidator aggregate = this.build(context.builder);
		final ValidationResult result = this.validate(aggregate, context.param);

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
		final TValidator aggregate = this.build(context.builder);
		final ValidationResult result = this.validate(aggregate, context.param);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CHAIN_INVALID));
		context.assertAllValidatorsCalledOnce();
	}

	private class ThreeSubValidatorTestContext {
		private final TParam param = AggregateValidatorBuilderTest.this.createParam();
		private final TValidator validator1;
		private final TValidator validator2;
		private final TValidator validator3;
		private final TBuilder builder = AggregateValidatorBuilderTest.this.createBuilder();
		private final AggregateValidatorBuilderTest<TBuilder, TValidator, TParam> parent = AggregateValidatorBuilderTest.this;

		private ThreeSubValidatorTestContext(
				final ValidationResult result1,
				final ValidationResult result2,
				final ValidationResult result3) {
			this.validator1 = this.parent.createValidator(result1);
			this.validator2 = this.parent.createValidator(result2);
			this.validator3 = this.parent.createValidator(result3);

			this.parent.add(this.builder, this.validator1);
			this.parent.add(this.builder, this.validator2);
			this.parent.add(this.builder, this.validator3);
		}

		private void assertOnlyFirstTwoValidatorsCalledOnce() {
			this.parent.verifyValidate(this.validator1, this.param, Mockito.only());
			this.parent.verifyValidate(this.validator2, this.param, Mockito.only());
			this.parent.verifyValidate(this.validator3, this.param, Mockito.never());
		}

		private void assertAllValidatorsCalledOnce() {
			this.parent.verifyValidate(this.validator1, this.param, Mockito.only());
			this.parent.verifyValidate(this.validator2, this.param, Mockito.only());
			this.parent.verifyValidate(this.validator3, this.param, Mockito.only());
		}
	}
}
