package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.function.Consumer;

public class ImportanceTransferTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);

	//region constructor

	@Test
	public void ctorCanCreateImportanceTransfer() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void ctorCanCreateImportanceTransferRevert() {
		assertCtorCanCreateImportanceTransfer(ImportanceTransferTransaction.Mode.Deactivate);
	}

	private void assertCtorCanCreateImportanceTransfer(final ImportanceTransferTransaction.Mode mode) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		// Act:
		final ImportanceTransferTransaction importanceTransferTransaction = createImportanceTransferTransaction(signer, mode, remote);

		// Assert:
		Assert.assertThat(importanceTransferTransaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransferTransaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransferTransaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(importanceTransferTransaction.getMode(), IsEqual.equalTo(mode));
		Assert.assertThat(importanceTransferTransaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1)));
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
		assertDeserializationFailure(jsonObject -> jsonObject.put("remoteAccount", null));
	}

	@Test
	public void deserializationFailsWhenModeIsInvalid() {
		// Assert:
		assertDeserializationFailure(jsonObject -> jsonObject.put("mode", 123));
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
	public void canRoundTripImportanceTransfer()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransaction.Mode.Activate);
	}

	@Test
	public void canRoundTripImportanceTransferRevert()  {
		assertImportanceTransferCanBeRoundTripped(ImportanceTransferTransaction.Mode.Deactivate);
	}

	public void assertImportanceTransferCanBeRoundTripped(final ImportanceTransferTransaction.Mode mode) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, remote);
		final ImportanceTransferTransaction originalTransaction = createImportanceTransferTransaction(signer, mode, remote);

		// Act:
		final ImportanceTransferTransaction importanceTransferTransaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		Assert.assertThat(importanceTransferTransaction.getTimeStamp(), IsEqual.equalTo(TIME));
		Assert.assertThat(importanceTransferTransaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(importanceTransferTransaction.getRemote(), IsEqual.equalTo(remote));
		Assert.assertThat(importanceTransferTransaction.getMode(), IsEqual.equalTo(mode));
		Assert.assertThat(importanceTransferTransaction.getMinimumFee(), IsEqual.equalTo(Amount.fromNem(1)));
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

	// region validate

	@Test
	public void transactionsWithFeesUpToSenderBalanceAreValid() {
		assertValidationResult(100, 1, ValidationResult.SUCCESS);
		assertValidationResult(100, 100, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionsWithFeesGreaterThanSenderBalanceAreNotValid() {
		assertValidationResult(100, 101, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	public void assertValidationResult(final int senderBalance, final int fee, final ValidationResult expectedResult) {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();

		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(fee));
		transaction.setDeadline(transaction.getTimeStamp().addHours(1));
		signer.incrementBalance(Amount.fromNem(senderBalance));

		// Act:
		final ValidationResult result = transaction.checkValidity();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	//region fee

	@Test
	public void minimumFeeIsOneNem() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);

		// Act + Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeCommitsBalanceChanges() {
		// Arrange:
		final Transaction transaction = createTransactionForExecuteUndoTests();

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(transaction.getSigner().getBalance(), IsEqual.equalTo(Amount.fromNem(80L)));
	}

	@Test
	public void undoCommitsBalanceChanges() {
		// Arrange:
		final Transaction transaction = createTransactionForExecuteUndoTests();

		// Act:
		transaction.undo();

		// Assert:
		Assert.assertThat(transaction.getSigner().getBalance(), IsEqual.equalTo(Amount.fromNem(100L)));
	}

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());

		final AccountNotification notification1 = (AccountNotification)notificationCaptor.getAllValues().get(0);
		Assert.assertThat(notification1.getType(), IsEqual.equalTo(NotificationType.Account));
		Assert.assertThat(notification1.getAccount(), IsEqual.equalTo(remote));

		final BalanceAdjustmentNotification notification2 = (BalanceAdjustmentNotification)notificationCaptor.getAllValues().get(1);
		Assert.assertThat(notification2.getType(), IsEqual.equalTo(NotificationType.BalanceDebit));
		Assert.assertThat(notification2.getAccount(), IsEqual.equalTo(signer));
		Assert.assertThat(notification2.getAmount(), IsEqual.equalTo(Amount.fromNem(10)));

		final ImportanceTransferNotification notification3 = (ImportanceTransferNotification)notificationCaptor.getAllValues().get(2);
		Assert.assertThat(notification3.getType(), IsEqual.equalTo(NotificationType.ImportanceTransfer));
		Assert.assertThat(notification3.getLessor(), IsEqual.equalTo(signer));
		Assert.assertThat(notification3.getLessee(), IsEqual.equalTo(remote));
		Assert.assertThat(notification3.getMode(), IsEqual.equalTo(ImportanceTransferTransaction.Mode.Activate.value()));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());

		final AccountNotification notification1 = (AccountNotification)notificationCaptor.getAllValues().get(2);
		Assert.assertThat(notification1.getType(), IsEqual.equalTo(NotificationType.Account));
		Assert.assertThat(notification1.getAccount(), IsEqual.equalTo(remote));

		final BalanceAdjustmentNotification notification2 = (BalanceAdjustmentNotification)notificationCaptor.getAllValues().get(1);
		Assert.assertThat(notification2.getType(), IsEqual.equalTo(NotificationType.BalanceCredit));
		Assert.assertThat(notification2.getAccount(), IsEqual.equalTo(signer));
		Assert.assertThat(notification2.getAmount(), IsEqual.equalTo(Amount.fromNem(10)));

		final ImportanceTransferNotification notification3 = (ImportanceTransferNotification)notificationCaptor.getAllValues().get(0);
		Assert.assertThat(notification3.getType(), IsEqual.equalTo(NotificationType.ImportanceTransfer));
		Assert.assertThat(notification3.getLessor(), IsEqual.equalTo(signer));
		Assert.assertThat(notification3.getLessee(), IsEqual.equalTo(remote));
		Assert.assertThat(notification3.getMode(), IsEqual.equalTo(ImportanceTransferTransaction.Mode.Activate.value()));
	}

	private Transaction createTransactionForExecuteUndoTests() {
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(Amount.fromNem(90));
		final Account remote = Utils.generateRandomAccount();
		final ImportanceTransferTransaction transaction = createImportanceTransferTransaction(signer, mode, remote);
		transaction.setFee(Amount.fromNem(10));
		return transaction;
	}

	// endregion

	private static ImportanceTransferTransaction createImportanceTransferTransaction(
			final Account sender,
			ImportanceTransferTransaction.Mode mode,
			final Account remote) {
		return new ImportanceTransferTransaction(TIME, sender, mode, remote);
	}
}
