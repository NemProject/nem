package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

public class BlockHeightBlockValidatorDecoratorTest {

	@Test
	public void innerValidatorIsBypassedBeforeForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight.prev());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Mockito.verify(context.innerValidator, Mockito.never()).validate(Mockito.any());
	}

	@Test
	public void innerValidatorIsDelegatedToAtForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		Mockito.verify(context.innerValidator, Mockito.only()).validate(Mockito.any());
	}

	@Test
	public void innerValidatorIsDelegatedToAfterForkHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(context.effectiveBlockHeight.next());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
		Mockito.verify(context.innerValidator, Mockito.only()).validate(Mockito.any());
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
		private final BlockValidator innerValidator = Mockito.mock(BlockValidator.class);
		private final BlockValidator validator = new BlockHeightBlockValidatorDecorator(this.effectiveBlockHeight, this.innerValidator);

		public TestContext() {
			Mockito.when(this.innerValidator.validate(Mockito.any())).thenReturn(ValidationResult.FAILURE_UNKNOWN);
		}

		public ValidationResult validateAtHeight(final BlockHeight height) {
			final Block block = NisUtils.createRandomBlockWithHeight(height.getRaw());
			return this.validator.validate(block);
		}
	}
}
