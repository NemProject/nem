package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultisigSignerModificationTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);
	final MultisigModificationType MODIFICATION_ADD = MultisigModificationType.Add;

	//region constructor
	@Test
	public void cannotCreateMultisigSignerModificationWithNullModifications() {
		// Arrange:
		final Account signer = Mockito.mock(Account.class);

		// Act:
		ExceptionAssert.assertThrows(v -> new MultisigSignerModificationTransaction(TIME, signer, null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMultisigSignerModificationWithEmptyModifications() {
		// Arrange:
		final Account signer = Mockito.mock(Account.class);

		// Act:
		ExceptionAssert.assertThrows(v -> new MultisigSignerModificationTransaction(TIME, signer, new ArrayList<>()), IllegalArgumentException.class);
	}

	@Test
	public void ctorCanCreateMultisigSignerModification() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = createModificationList(modificationType, cosignatory);

		// Act:
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modifications);

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
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);
		final MultisigSignerModificationTransaction originalTransaction = createMultisigSignerModificationTransaction(signer, createModificationList(modificationType, cosignatory));

		// Act:
		final MultisigSignerModificationTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNER_MODIFY));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		final MultisigModification modification = transaction.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(modificationType));
	}

	private MultisigSignerModificationTransaction createRoundTrippedTransaction(
			final MultisigSignerModificationTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new MultisigSignerModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	// endregion

	//region fee

	@Test
	public void minimumFeeIsOneThousandNem() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, createModificationList(modificationType, cosignatory));

		// Act + Assert:
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1000)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modificationList = createModificationList(modificationType, cosignatory);
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationList);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory);
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(1000));
		NotificationUtils.assertCosignatoryModificationNotification(
				notificationCaptor.getAllValues().get(2),
				signer,
				modificationList);
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modificationList = createModificationList(modificationType, cosignatory);
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationList);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory);
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(1000));
		NotificationUtils.assertCosignatoryModificationNotification(
				notificationCaptor.getAllValues().get(0),
				signer,
				modificationList);
	}

	@Test
	public void executeRaisesAccountNotificationForAllModifications() {
		// Arrange:
		final MultisigModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount(Amount.fromNem(90));
		final Account cosignatory1 = Utils.generateRandomAccount();
		final Account cosignatory2 = Utils.generateRandomAccount();
		final List<MultisigModification> modificationList = Arrays.asList(
				new MultisigModification(modificationType, cosignatory1),
				new MultisigModification(modificationType, cosignatory2)
		);

		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationList);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory1);
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(1), cosignatory2);
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), signer, Amount.fromNem(1000));
		NotificationUtils.assertCosignatoryModificationNotification(
				notificationCaptor.getAllValues().get(3),
				signer,
				modificationList);
	}
	// endregion

	private static List<MultisigModification> createModificationList(final MultisigModificationType modificationType, final Account cosignatory) {
		final MultisigModification multisigModification = new MultisigModification(modificationType, cosignatory);
		return Arrays.asList(multisigModification);
	}

	private static MultisigSignerModificationTransaction createMultisigSignerModificationTransaction(
			final Account sender, final List<MultisigModification> modifications) {
		return new MultisigSignerModificationTransaction(TIME, sender, modifications);
	}
}
