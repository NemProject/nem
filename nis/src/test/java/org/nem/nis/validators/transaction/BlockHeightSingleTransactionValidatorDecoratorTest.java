package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

public class BlockHeightSingleTransactionValidatorDecoratorTest {

	@Test
	public void innerValidatorIsBypassedBeforeForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight.prev());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(context.innerValidator, Mockito.never()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void innerValidatorIsDelegatedToAtForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		Mockito.verify(context.innerValidator, Mockito.only()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void innerValidatorIsDelegatedToAfterForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight.next());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		Mockito.verify(context.innerValidator, Mockito.only()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void getNameIncludesEffectiveBlockHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.innerValidator.getName()).thenReturn("inner");

		// Act:
		final String name = context.validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("inner @ 123"));
		Mockito.verify(context.innerValidator, Mockito.only()).getName();
	}

	private static class TestContext {
		private final BlockHeight effectiveBlockHeight = new BlockHeight(123);
		private final SingleTransactionValidator innerValidator = Mockito.mock(SingleTransactionValidator.class);
		private final SingleTransactionValidator validator = new BlockHeightSingleTransactionValidatorDecorator(this.effectiveBlockHeight,
				this.innerValidator);

		public TestContext() {
			Mockito.when(this.innerValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.FAILURE_UNKNOWN);
		}

		public ValidationResult validateAtHeight(final BlockHeight height) {
			return this.validator.validate(Mockito.mock(Transaction.class), new ValidationContext(height, ValidationStates.Throw));
		}
	}
}
