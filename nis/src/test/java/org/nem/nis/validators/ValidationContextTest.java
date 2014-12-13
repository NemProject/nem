package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;

public class ValidationContextTest {

	@Test
	public void canCreateDefaultContextWithCustomDebitPredicate() {
		// Arrange:
		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(debitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getDebitPredicate(), IsEqual.equalTo(debitPredicate));
	}

	@Test
	public void canCreateDefaultContextWithCustomDebitPredicateAndBlockHeight() {
		// Arrange:
		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), debitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getDebitPredicate(), IsEqual.equalTo(debitPredicate));
	}

	@Test
	public void canCreateDefaultContextWithAllCustomParameters() {
		// Arrange:
		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7), debitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		Assert.assertThat(context.getDebitPredicate(), IsEqual.equalTo(debitPredicate));
	}
}