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

	//region constructor

	@Test
	public void ctorCanCreateImportanceTransfer() {
		this.assertCtorCanCreateImportanceTransfer(ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void ctorCanCreateImportanceTransferRevert() {
		this.assertCtorCanCreateImportanceTransfer(ImportanceTransferTransaction.Mode.Deactivate);
	}

	private void assertCtorCanCreateImportanceTransfer(final ImportanceTransferTransaction.Mode mode) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);

		// Assert:
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(transaction.getMode(), IsEqual.equalTo(mode));
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void transferCannotBeCreatedWithoutRemote() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		createImportanceTransferTransaction(signer, ImportanceTransferTransaction.Mode.Activate, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void transferCannotBeCreatedWithUnknownDirection() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		createImportanceTransferTransaction(signer, ImportanceTransferTransaction.Mode.Unknown, remote);
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
				ImportanceTransferTransaction.Mode.Activate,
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
		this.assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void canRoundTripImportanceTransferRevert() {
		this.assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransaction.Mode.Deactivate);
	}

	public void assertImportanceTransferCanBeRoundTripped(final ImportanceTransferTransaction.Mode mode) {
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
		Assert.assertThat(transaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(transaction.getMode(), IsEqual.equalTo(mode));
		Assert.assertThat(transaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(2)));
	}

	private ImportanceTransferTransaction createRoundTrippedTransaction(
			final ImportanceTransferTransaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new ImportanceTransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	// endregion

	//region fee

	@Test
	public void minimumFeeIsTwoNem() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);

		// Act + Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(2)));
	}

	//endregion

	//region getAccounts

	@Test
	public void getAccountsIncludesSignerAndRemoteAccounts() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
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
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
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
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(10));
		NotificationUtils.assertImportanceTransferNotification(
				notificationCaptor.getAllValues().get(2),
				signer,
				remote,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
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
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), signer, Amount.fromNem(10));
		NotificationUtils.assertImportanceTransferNotification(
				notificationCaptor.getAllValues().get(0),
				signer,
				remote,
				ImportanceTransferTransaction.Mode.Activate.value());
	}

	// endregion

	private static ImportanceTransferTransaction createImportanceTransferTransaction(
			final Account sender,
			final ImportanceTransferTransaction.Mode mode,
			final Account remote) {
		return new ImportanceTransferTransaction(TIME, sender, mode, remote);
	}
}
