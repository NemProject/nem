package org.nem.nis.validators;

import org.hamcrest.MatcherAssert;
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
		MatcherAssert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		MatcherAssert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		MatcherAssert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}

	@Test
	public void canCreateContextWithCustomValidationStateAndBlockHeight() {
		// Arrange:
		final ValidationState validationState = Mockito.mock(ValidationState.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), validationState);

		// Assert:
		MatcherAssert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		MatcherAssert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		MatcherAssert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}

	@Test
	public void canCreateContextWithAllCustomParameters() {
		// Arrange:
		final ValidationState validationState = Mockito.mock(ValidationState.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7), validationState);

		// Assert:
		MatcherAssert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		MatcherAssert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		MatcherAssert.assertThat(context.getState(), IsEqual.equalTo(validationState));
	}
}
