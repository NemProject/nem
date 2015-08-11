package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.MosaicTransferNotification;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.*;

import java.util.*;

@RunWith(Enclosed.class)
public class MosaicBalanceValidatorTest {
	private final static Quantity QUANTITY = Quantity.fromValue(100L);

	// region single mosaic transfer

	public static class SingleTransferTransactionMosaicBalanceValidatorTest extends AbstractMosaicBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final long delta) {
			final Mosaic mosaic = this.createMosaic(delta);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), QUANTITY));
			transaction.setFee(Amount.fromNem(0));
			return transaction;
		}
	}

	// endregion

	// region multiple mosaic transfers

	public static class MultipleTransferTransactionsMosaicBalanceValidatorTest extends AbstractMosaicBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final long delta) {
			final Mosaic mosaic = this.createMosaic(delta);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(50)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(47)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(3)));
			transaction.setFee(Amount.fromNem(0));
			return transaction;
		}

		@Test
		public void mosaicsReceivedEarlierCanBeSpentLater() {
			// Arrange:
			final Mosaic mosaic = this.createMosaic(0);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(50)));
			transaction.addNotification(new MosaicTransferNotification(recipient, sender, mosaic.getMosaicId(), Quantity.fromValue(40)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(70)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(
					transaction,
					createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}

		@Test
		public void cannotSpendMosaicsReceivedLater() {
			// Arrange:
			final Mosaic mosaic = this.createMosaic(0);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(recipient, sender, mosaic.getMosaicId(), Quantity.fromValue(10)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(70)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(
					transaction,
					createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		}
	}

	// endregion

	private static abstract class AbstractMosaicBalanceValidatorTest {
		private final Map<Account, Map<MosaicId, Long>> map = new HashMap<>();

		protected abstract Transaction createTransaction(final long balanceDelta);

		@Test
		public void accountWithLargerThanRequiredBalancePassesValidation() {
			// Assert:
			this.assertValidationResult(ValidationResult.SUCCESS, 1L);
		}

		@Test
		public void accountWithExactRequiredBalancePassesValidation() {
			// Assert:
			this.assertValidationResult(ValidationResult.SUCCESS, 0L);
		}

		@Test
		public void accountWithSmallerThanRequiredBalanceFailsValidation() {
			// Assert:
			this.assertValidationResult(ValidationResult.FAILURE_INSUFFICIENT_BALANCE, -1L);
		}

		private void assertValidationResult(final ValidationResult expectedResult, final long balanceDelta) {
			// Arrange:
			final SingleTransactionValidator validator = new MosaicBalanceValidator();
			final Transaction transaction = this.createTransaction(balanceDelta);

			// Act:
			final ValidationResult result = validator.validate(
					transaction,
					createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

		protected Mosaic createMosaic(final long delta) {
			return new Mosaic(Utils.createMosaicId(10), Quantity.fromValue(QUANTITY.getRaw() + delta));
		}

		protected Account createAccount(final Mosaic mosaic) {
			final Account account = Utils.generateRandomAccount();
			final Map<MosaicId, Long> mosaicIdToLong = this.map.getOrDefault(account, new HashMap<>());
			this.map.put(account, mosaicIdToLong);
			if (null != mosaic) {
				mosaicIdToLong.put(mosaic.getMosaicId(), mosaic.getQuantity().getRaw());
			}
			return account;
		}

		protected DebitPredicate<Mosaic> createMosaicDebitPredicate() {
			return (account, mosaic) -> {
				final Map<MosaicId, Long> mosaicIdLongMap = this.map.getOrDefault(account, new HashMap<>());
				return mosaicIdLongMap.getOrDefault(mosaic.getMosaicId(), 0L).compareTo(mosaic.getQuantity().getRaw()) >= 0;
			};
		}

		protected static ValidationContext createValidationContext(final DebitPredicate<Mosaic> mosaicDebitPredicate) {
			final ValidationState validationState = new ValidationState(DebitPredicates.XemThrow, mosaicDebitPredicate);
			return new ValidationContext(validationState);
		}
	}
}
