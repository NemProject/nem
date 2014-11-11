package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.function.Consumer;

public class MultisigSignerModificationTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);
	final MultisigSignerModificationTransaction.ModificationType MODIFICATION_ADD
			= MultisigSignerModificationTransaction.ModificationType.Add;
	final MultisigSignerModificationTransaction.ModificationType MODIFICATION_UNKNOWN
			= MultisigSignerModificationTransaction.ModificationType.Unknown;

	//region constructor

	@Test
	public void ctorCanCreateMultisigModification() {
		// Arrange:
		final MultisigSignerModificationTransaction.ModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();

		// Act:
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Assert:
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(transaction.getModificationType(), IsEqual.equalTo(modificationType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void multisigModificationCannotBeCreatedWithoutCosignatory() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		createMultisigSignerModificationTransaction(signer, MODIFICATION_ADD, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multisigModificationCannotBeCreatedWithUnknownModificationType() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();

		// Act:
		createMultisigSignerModificationTransaction(signer, MODIFICATION_UNKNOWN, cosignatory);
	}

	@Test
	public void deserializationFailsWhenAddressIsMissing() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("cosignatoryAccount", null));
	}

	@Test
	public void deserializationFailsWhenModeIsInvalid() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("modificationType", 123));
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);

		final MultisigSignerModificationTransaction originalEntity = createMultisigSignerModificationTransaction(
				signer,
				MODIFICATION_ADD,
				cosignatory);
		originalEntity.sign();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalEntity);
		invalidateJsonConsumer.accept(jsonObject); // invalidate the json

		// Act:
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(accountLookup));
		deserializer.readInt("type");
		ExceptionAssert.assertThrows(
				v -> new MultisigSignerModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer),
				SerializationException.class);
	}

	// endregion

	// region roundtrip

	@Test
	public void canRoundtripMultisigModification() {
		// Arrange:
		final MultisigSignerModificationTransaction.ModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);
		final MultisigSignerModificationTransaction originalTransaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Act:
		final MultisigSignerModificationTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNER_MODIFY));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(transaction.getModificationType(), IsEqual.equalTo(modificationType));
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
		final MultisigSignerModificationTransaction.ModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);

		// Act + Assert:
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1000)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigSignerModificationTransaction.ModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);
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
				cosignatory,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MultisigSignerModificationTransaction.ModificationType modificationType = MODIFICATION_ADD;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account cosignatory = Utils.generateRandomAccount();
		final MultisigSignerModificationTransaction transaction = createMultisigSignerModificationTransaction(signer, modificationType, cosignatory);
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
				cosignatory,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	// endregion

	private static MultisigSignerModificationTransaction createMultisigSignerModificationTransaction(
			final Account sender,
			final MultisigSignerModificationTransaction.ModificationType modificationType,
			final Account cosignatory) {
		return new MultisigSignerModificationTransaction(TIME, sender, modificationType, cosignatory);
	}
}
