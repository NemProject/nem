package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class ValidationContextTest {

	@Test
	public void canCreateDefaultContext() {
		// Arrange:
		final ValidationContext context = new ValidationContext();

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(context.getDebitPredicate(), IsNull.notNullValue());
		this.assertDefaultDebitPredicateBehavior(context.getDebitPredicate());
	}

	@Test
	public void canCreateDefaultContextWithCustomBlockHeight() {
		// Arrange:
		final ValidationContext context = new ValidationContext(new BlockHeight(11));

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getDebitPredicate(), IsNull.notNullValue());
		this.assertDefaultDebitPredicateBehavior(context.getDebitPredicate());
	}

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
	public void canCreateDefaultContextWithCustomCurrentAndConfirmedBlockHeights() {
		// Arrange:
		final ValidationContext context = new ValidationContext(new BlockHeight(11), new BlockHeight(7));

		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(7)));
		Assert.assertThat(context.getDebitPredicate(), IsNull.notNullValue());
		this.assertDefaultDebitPredicateBehavior(context.getDebitPredicate());
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

	private void assertDefaultDebitPredicateBehavior(final DebitPredicate debitPredicate) {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(10));

		// Assert:
		Assert.assertThat(debitPredicate.canDebit(account, Amount.fromNem(9)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account, Amount.fromNem(10)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account, Amount.fromNem(11)), IsEqual.equalTo(false));
	}
}