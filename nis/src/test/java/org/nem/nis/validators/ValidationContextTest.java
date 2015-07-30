package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;

public class ValidationContextTest {

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithCustomXemDebitPredicate() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(xemDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsNull.nullValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithCustomXemDebitPredicateAndCustomMosaicDebitPredicate() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final DebitPredicate mosaicDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(xemDebitPredicate,mosaicDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsEqual.equalTo(mosaicDebitPredicate));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithCustomXemDebitPredicateAndBlockHeight() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), xemDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsNull.nullValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithCustomXemDebitPredicateAndBlockHeightAndConfirmedBlockHeight() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7), xemDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsNull.nullValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithCustomXemDebitPredicateAndCustomMosaicDebitPredicateAndBlockHeight() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final DebitPredicate mosaicDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), xemDebitPredicate, mosaicDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsEqual.equalTo(mosaicDebitPredicate));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canCreateDefaultContextWithAllCustomParameters() {
		// Arrange:
		final DebitPredicate xemDebitPredicate = Mockito.mock(DebitPredicate.class);
		final DebitPredicate mosaicDebitPredicate = Mockito.mock(DebitPredicate.class);
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7), xemDebitPredicate, mosaicDebitPredicate);

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		Assert.assertThat(context.getXemDebitPredicate(), IsEqual.equalTo(xemDebitPredicate));
		Assert.assertThat(context.getMosaicDebitPredicate(), IsEqual.equalTo(mosaicDebitPredicate));
	}
}