package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.test.DebitPredicates;

public class ValidationStateTest {

	@Test
	public void canDebitDelegatesToXemDebitPredicate() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final DebitPredicate<Amount> xemDebitPredicate = createMockXemDebitPredicate(false);
		final ValidationState validationState = new ValidationState(xemDebitPredicate, DebitPredicates.MosaicThrow);

		// Act:
		final boolean result = validationState.canDebit(account, Amount.fromNem(1234));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Mockito.verify(xemDebitPredicate, Mockito.only()).canDebit(account, Amount.fromNem(1234));
	}

	@Test
	public void canDebitDelegatesToMosaicDebitPredicate() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final DebitPredicate<Mosaic> mosaicDebitPredicate = createMockMosaicDebitPredicate(true);
		final ValidationState validationState = new ValidationState(DebitPredicates.XemThrow, mosaicDebitPredicate);

		// Act:
		final boolean result = validationState.canDebit(account, Utils.createMosaic(4, 1234));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(mosaicDebitPredicate, Mockito.only()).canDebit(account, Utils.createMosaic(4, 1234));
	}

	@SuppressWarnings("unchecked")
	private static DebitPredicate<Amount> createMockXemDebitPredicate(final boolean canDebit) {
		return createMockDebitPredicate(canDebit);
	}

	@SuppressWarnings("unchecked")
	private static DebitPredicate<Mosaic> createMockMosaicDebitPredicate(final boolean canDebit) {
		return createMockDebitPredicate(canDebit);
	}

	@SuppressWarnings("unchecked")
	private static DebitPredicate createMockDebitPredicate(final boolean canDebit) {
		final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);
		Mockito.when(debitPredicate.canDebit(Mockito.any(), Mockito.any())).thenReturn(canDebit);
		return debitPredicate;
	}
}