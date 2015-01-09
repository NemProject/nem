package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public abstract class MultisigAggregateModificationTransactionTest {
	public static final TimeInstant TIME = new TimeInstant(123);

	protected abstract MultisigModificationType getModification();

	//region constructor

	// TODO 20150103 at least assert the list size

	@Test
	public void ctorCanCreateMultisigAggregateModificationTransaction() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = createModificationList(modificationType, cosignatory);

		// Act:
		final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);

		// Assert:
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getModifications().size(), IsEqual.equalTo(1));
		final MultisigModification modification = transaction.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(modificationType));
	}
	// endregion

	// region roundtrip

	@Test
	public void canRoundtripMultisigModification() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);
		final MultisigAggregateModificationTransaction originalTransaction = createTransaction(signer,
				createModificationList(modificationType, cosignatory));

		// Act:
		final MultisigAggregateModificationTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		final MultisigModification modification = transaction.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(modificationType));
	}

	private MultisigAggregateModificationTransaction createRoundTrippedTransaction(
			final MultisigAggregateModificationTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new MultisigAggregateModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	// endregion

	//region fee

	@Test
	public void minimumFeeIsOneThousandNem() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigAggregateModificationTransaction transaction = createTransaction(signer,
				createModificationList(modificationType, cosignatory));

		// Act + Assert:
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(200)));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(200)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = createModificationList(modificationType, cosignatory);
		final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(0));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), signer, Amount.fromNem(200));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = createModificationList(modificationType, cosignatory);
		final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(0));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), signer, Amount.fromNem(200));
	}

	@Test
	public void executeRaisesAppropriateNotificationForAllModifications() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory1 = Utils.generateRandomAccount();
		final Account cosignatory2 = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(modificationType, cosignatory1),
				new MultisigModification(modificationType, cosignatory2));

		final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory1);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(0));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory2);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(3), signer, modifications.get(1));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(4), signer, Amount.fromNem(300));
	}

	@Test
	public void undoRaisesAppropriateNotificationForAllModifications() {
		// Arrange:
		final MultisigModificationType modificationType = this.getModification();
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory1 = Utils.generateRandomAccount();
		final Account cosignatory2 = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = Arrays.asList(
				new MultisigModification(modificationType, cosignatory1),
				new MultisigModification(modificationType, cosignatory2));

		final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(4), cosignatory1);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(3), signer, modifications.get(0));
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory2);
		NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(1));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), signer, Amount.fromNem(300));
	}

	// endregion

	private static List<MultisigModification> createModificationList(final MultisigModificationType modificationType, final Account cosignatory) {
		final MultisigModification multisigModification = new MultisigModification(modificationType, cosignatory);
		return Arrays.asList(multisigModification);
	}

	public static MultisigAggregateModificationTransaction createTransaction(final Account sender, final List<MultisigModification> modifications) {
		return new MultisigAggregateModificationTransaction(TIME, sender, modifications);
	}
}
