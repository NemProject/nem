package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;

public class ValidationContextTest {

	@Test
	public void canCreateContextWithCustomValidationState() {
		// Arrange:
		final ValidationState validationState = Mockito.mock(ValidationState.class);
		final ValidationContext context = new ValidationContext(validationState);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}

	@Test
	public void canCreateContextWithCustomValidationStateAndBlockHeight() {
		// Arrange:
		final ValidationState validationState = Mockito.mock(ValidationState.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), validationState);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}

	@Test
	public void canCreateContextWithAllCustomParameters() {
		// Arrange:
		final ValidationState validationState = Mockito.mock(ValidationState.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7), validationState);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		Assert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}
}