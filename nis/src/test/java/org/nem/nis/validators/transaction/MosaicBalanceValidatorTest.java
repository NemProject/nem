package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.util.*;

@RunWith(Enclosed.class)
public class MosaicBalanceValidatorTest {
	// id of a mosaic with a (self) levy
	private static final int MOSAIC_WITH_LEVY_ID = 999;

	public static class NoBalanceChangeMosaicBalanceValidatorTest {

		@Test
		public void otherNotificationsAreIgnored() {
			// Arrange:
			final SingleTransactionValidator validator = new MosaicBalanceValidator();
			final MockTransaction transaction = new MockTransaction();
			transaction.setTransferAction(observer -> new AccountNotification(transaction.getSigner()));

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}
	}

	// region single mosaic transfer

	public static class SingleTransferTransactionMosaicBalanceValidatorTest extends AbstractMosaicBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final long delta) {
			final Mosaic mosaic = createMosaic(100 + delta);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(100)));
			transaction.setFee(Amount.fromNem(0));
			return transaction;
		}
	}

	public static class SingleTransferTransactionMosaicBalanceWithLevyValidatorTest extends AbstractMosaicBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final long delta) {
			// Arrange: create a real TransferTransaction because all levy logic is in TransferTransaction class :/
			// seed the account with two extra quantity because the levy costs 2
			final Account sender = this.createAccount(createMosaic(MOSAIC_WITH_LEVY_ID, 102 + delta));
			final Account recipient = this.createAccount(null);
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaic(Utils.createMosaicId(MOSAIC_WITH_LEVY_ID), Quantity.fromValue(100));
			final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(1),
					attachment);
			transaction.setFee(Amount.fromNem(0));
			return transaction;
		}
	}

	// endregion

	// region multiple mosaic transfers

	public static class MultipleTransferTransactionsMosaicBalanceValidatorTest extends AbstractMosaicBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final long delta) {
			final Mosaic mosaic = createMosaic(100 + delta);
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
			final Mosaic mosaic = createMosaic(100);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(50)));
			transaction.addNotification(new MosaicTransferNotification(recipient, sender, mosaic.getMosaicId(), Quantity.fromValue(40)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(70)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(transaction, createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}

		@Test
		public void cannotSpendMosaicsReceivedLater() {
			// Arrange:
			final Mosaic mosaic = createMosaic(100);
			final Account sender = this.createAccount(mosaic);
			final Account recipient = this.createAccount(null);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(recipient, sender, mosaic.getMosaicId(), Quantity.fromValue(10)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic.getMosaicId(), Quantity.fromValue(70)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(transaction, createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		}

		@Test
		public void allMosaicsMustHaveSufficientBalance() {
			// Arrange:
			final Mosaic mosaic1 = createMosaic(1, 100);
			final Mosaic mosaic2 = createMosaic(2, 50);
			final Mosaic mosaic3 = createMosaic(3, 25);
			final Account sender = this.createAccount(mosaic1);
			this.setBalance(sender, mosaic2);
			this.setBalance(sender, mosaic3);
			final Account recipient = this.createAccount(null);

			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic1.getMosaicId(), Quantity.fromValue(40)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic2.getMosaicId(), Quantity.fromValue(10)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic3.getMosaicId(), Quantity.fromValue(8)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic1.getMosaicId(), Quantity.fromValue(20)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic2.getMosaicId(), Quantity.fromValue(30)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic3.getMosaicId(), Quantity.fromValue(2)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(transaction, createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}

		@Test
		public void validationFailsIfAtLeastOneMosaicHasInsufficientBalance() {
			// Arrange:
			final Mosaic mosaic1 = createMosaic(1, 100);
			final Mosaic mosaic2 = createMosaic(2, 50);
			final Mosaic mosaic3 = createMosaic(3, 25);
			final Account sender = this.createAccount(mosaic1);
			this.setBalance(sender, mosaic2);
			this.setBalance(sender, mosaic3);
			final Account recipient = this.createAccount(null);

			final MockTransaction transaction = new MockTransaction(sender);
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic1.getMosaicId(), Quantity.fromValue(40)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic2.getMosaicId(), Quantity.fromValue(21)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic3.getMosaicId(), Quantity.fromValue(8)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic1.getMosaicId(), Quantity.fromValue(20)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic2.getMosaicId(), Quantity.fromValue(30)));
			transaction.addNotification(new MosaicTransferNotification(sender, recipient, mosaic3.getMosaicId(), Quantity.fromValue(2)));
			transaction.setFee(Amount.fromNem(0));
			final SingleTransactionValidator validator = new MosaicBalanceValidator();

			// Act:
			final ValidationResult result = validator.validate(transaction, createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
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
			final ValidationResult result = validator.validate(transaction, createValidationContext(this.createMosaicDebitPredicate()));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

		protected Account createAccount(final Mosaic mosaic) {
			final Account account = Utils.generateRandomAccount();
			this.setBalance(account, mosaic);
			return account;
		}

		protected void setBalance(final Account account, final Mosaic mosaic) {
			final Map<MosaicId, Long> mosaicIdToLong = this.map.getOrDefault(account, new HashMap<>());
			this.map.put(account, mosaicIdToLong);
			if (null != mosaic) {
				mosaicIdToLong.put(mosaic.getMosaicId(), mosaic.getQuantity().getRaw());
			}
		}

		protected DebitPredicate<Mosaic> createMosaicDebitPredicate() {
			return (account, mosaic) -> {
				final Map<MosaicId, Long> mosaicIdLongMap = this.map.getOrDefault(account, new HashMap<>());
				return mosaicIdLongMap.getOrDefault(mosaic.getMosaicId(), 0L).compareTo(mosaic.getQuantity().getRaw()) >= 0;
			};
		}

		protected static ValidationContext createValidationContext(final DebitPredicate<Mosaic> mosaicDebitPredicate) {
			// add a mosaic with a self levy of 2
			final ReadOnlyNisCache readOnlyNisCache = NisCacheFactory.createReal();
			final NisCache copyCache = readOnlyNisCache.copy();
			final MosaicId mosaicId = Utils.createMosaicId(MOSAIC_WITH_LEVY_ID);
			final Account namespaceOwner = Utils.generateRandomAccount();
			copyCache.getNamespaceCache().add(new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE));

			final MosaicDefinition mosaicDefinition = new MosaicDefinition(namespaceOwner, mosaicId, new MosaicDescriptor("awesome mosaic"),
					Utils.createMosaicProperties(),
					new MosaicLevy(MosaicTransferFeeType.Absolute, Utils.generateRandomAccount(), mosaicId, Quantity.fromValue(2)));
			copyCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics().add(mosaicDefinition);
			copyCache.commit();

			final ValidationState validationState = new ValidationState(DebitPredicates.XemThrow, mosaicDebitPredicate,
					NisCacheUtils.createTransactionExecutionState(readOnlyNisCache));
			return new ValidationContext(validationState);
		}
	}

	private static Mosaic createMosaic(final int mosaicId, final long quantity) {
		return new Mosaic(Utils.createMosaicId(mosaicId), Quantity.fromValue(quantity));
	}

	private static Mosaic createMosaic(final long quantity) {
		return createMosaic(10, quantity);
	}
}
