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

import java.util.Collection;
import java.util.function.Consumer;

public class ImportanceTransferTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);
	private static final Amount EXPECTED_FEE = Amount.fromNem(6);

	@Test
	public void ctorCanCreateImportanceTransfer() {
		this.assertCtorCanCreateImportanceTransfer(ImportanceTransferMode.Activate);
	}

	@Test
	public void ctorCanCreateImportanceTransferRevert() {
		this.assertCtorCanCreateImportanceTransfer(ImportanceTransferMode.Deactivate);
	}

	private void assertCtorCanCreateImportanceTransfer(final ImportanceTransferMode mode) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);

		// Assert:
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(transaction.getMode(), IsEqual.equalTo(mode));
	}

	@Test(expected = IllegalArgumentException.class)
	public void transferCannotBeCreatedWithoutRemote() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		createImportanceTransferTransaction(signer, ImportanceTransferMode.Activate, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void transferCannotBeCreatedWithUnknownDirection() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		createImportanceTransferTransaction(signer, ImportanceTransferMode.Unknown, remote);
	}

	@Test
	public void deserializationFailsWhenAddressIsMissing() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("remoteAccount", null));
	}

	@Test
	public void deserializationFailsWhenModeIsInvalid() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("mode", 123));
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);

		final ImportanceTransferTransaction originalEntity = createImportanceTransferTransaction(
				signer,
				ImportanceTransferMode.Activate,
				remote);
		originalEntity.sign();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalEntity);
		invalidateJsonConsumer.accept(jsonObject); // invalidate the json

		// Act:
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(accountLookup));
		deserializer.readInt("type");
		ExceptionAssert.assertThrows(
				v -> new ImportanceTransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer),
				SerializationException.class);
	}

	// endregion

	// region roundtrip

	@Test
	public void canRoundTripImportanceTransfer() {
		this.assertImportanceTransferCanBeRoundTripped(ImportanceTransferMode.Activate);
	}

	@Test
	public void canRoundTripImportanceTransferRevert() {
		this.assertImportanceTransferCanBeRoundTripped(ImportanceTransferMode.Deactivate);
	}

	public void assertImportanceTransferCanBeRoundTripped(final ImportanceTransferMode mode) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);
		final ImportanceTransferTransaction originalTransaction = createImportanceTransferTransaction(signer, mode, remote);

		// Act:
		final ImportanceTransferTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.IMPORTANCE_TRANSFER));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(transaction.getMode(), IsEqual.equalTo(mode));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(EXPECTED_FEE));
	}

	private ImportanceTransferTransaction createRoundTrippedTransaction(
			final ImportanceTransferTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new ImportanceTransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	//endregion

	//region getAccounts

	@Test
	public void getAccountsIncludesSignerAndRemoteAccounts() {
		// Arrange:
		final ImportanceTransferMode mode = ImportanceTransferMode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer, remote));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final ImportanceTransferMode mode = ImportanceTransferMode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), remote);
		NotificationUtils.assertImportanceTransferNotification(
				notificationCaptor.getAllValues().get(1),
				signer,
				remote,
				ImportanceTransferMode.Activate);
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), signer, Amount.fromNem(10));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final ImportanceTransferMode mode = ImportanceTransferMode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), remote);
		NotificationUtils.assertImportanceTransferNotification(
				notificationCaptor.getAllValues().get(1),
				signer,
				remote,
				ImportanceTransferMode.Activate);
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), signer, Amount.fromNem(10));
	}

	// endregion

	private static ImportanceTransferTransaction createImportanceTransferTransaction(
			final Account sender,
			final ImportanceTransferMode mode,
			final Account remote) {
		return new ImportanceTransferTransaction(TIME, sender, mode, remote);
	}
}
