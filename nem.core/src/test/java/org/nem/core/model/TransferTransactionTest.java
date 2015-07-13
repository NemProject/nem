package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.messages.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.*;

public class TransferTransactionTest {

	//region Constructor

	@Test(expected = IllegalArgumentException.class)
	public void recipientIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		this.createTransferTransaction(signer, null, 123, message);
	}

	@Test
	public void canCreateTransactionWithMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, message);

		// Assert:
		assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
		Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(3));
		Assert.assertThat(transaction.getSmartTileBag().getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateTransactionWithoutMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, null);

		// Assert:
		assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
		Assert.assertThat(transaction.getMessage(), IsNull.nullValue());
		Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(0));
		Assert.assertThat(transaction.getSmartTileBag().getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateTransactionWithSmartTiles() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final SmartTileBag bag = new SmartTileBag(createSmartTiles());

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, null, bag);

		// Assert:
		assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
		Assert.assertThat(transaction.getSmartTileBag().getSmartTiles(), IsEquivalent.equivalentTo(createSmartTiles()));
	}

	@Test
	public void canCreateTransactionWithoutSmartTiles() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, null, null);

		// Assert:
		assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
		Assert.assertThat(transaction.getSmartTileBag().getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionCanBeRoundTrippedWithMessage() {
		// Arrange:
		assertCanBeRoundTripped(new byte[] { 12, 50, 21 }, null);
	}

	@Test
	public void transactionCanBeRoundTrippedWithoutMessage() {
		// Assert:
		assertCanBeRoundTripped(null, null);
	}

	@Test
	public void transactionCanBeRoundTrippedWithSmartTiles() {
		// Arrange:
		assertCanBeRoundTripped(null, new SmartTileBag(createSmartTiles()));
	}

	@Test
	public void transactionCanBeRoundTrippedWithoutSmartTiles() {
		// Assert:
		assertCanBeRoundTripped(null, null);
	}

	private void assertCanBeRoundTripped(final byte[] messageBytes, final SmartTileBag bag) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		final Message message = null == messageBytes ? null : new PlainMessage(messageBytes);
		final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, message, bag);

		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
		final TransferTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
		Assert.assertThat(
				null == message ? transaction.getMessage() : transaction.getMessage().getDecodedPayload(),
				IsEqual.equalTo(null == message ? null : messageBytes));
		Assert.assertThat(
				transaction.getSmartTileBag().getSmartTiles(),
				IsEquivalent.equivalentTo(null == bag ? Collections.emptyList() : createSmartTiles()));
	}

	private static void assertTransactionFields(
			final TransferTransaction transaction,
			final Account signer,
			final Account recipient,
			final Amount amount) {
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123L)));
	}

	//endregion

	//region Message Length

	@Test
	public void messageLengthReturnsEncodedLength() {
		// Arrange:
		final TransferTransaction transaction = this.createTransactionWithMockMessage(100, 44);

		// Assert:
		Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(100));
	}

	private TransferTransaction createTransactionWithMockMessage(final int encodedMessageSize, final int decodedMessageSize) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final MockMessage message = new MockMessage(7);
		message.setEncodedPayload(new byte[encodedMessageSize]);
		message.setDecodedPayload(new byte[decodedMessageSize]);
		return this.createTransferTransaction(signer, recipient, 0, message);
	}

	//endregion

	//region Message set to null if payload length is 0

	@Test
	public void messageIsSetToNullIfPlainMessagePayloadIsNull() {
		// Assert:
		this.assertMessageFieldIsNull(null, MessageTypes.PLAIN, true);
	}

	@Test
	public void messageIsSetToNullIfPlainMessagePayloadLengthIsZero() {
		// Assert:
		this.assertMessageFieldIsNull(new byte[] {}, MessageTypes.PLAIN, true);
	}

	@Test
	public void messageIsNotSetToNullIfPlainMessagePayloadLengthIsNotZero() {
		// Assert:
		this.assertMessageFieldIsNull(new byte[] { 1 }, MessageTypes.PLAIN, false);
	}

	@Test
	public void messageIsSetToNullIfSecureMessagePayloadIsNull() {
		// Assert:
		this.assertMessageFieldIsNull(null, MessageTypes.SECURE, true);
	}

	@Test
	public void messageIsNotSetToNullIfSecureMessagePayloadLengthIsZero() {
		// Assert:
		this.assertMessageFieldIsNull(new byte[] {}, MessageTypes.SECURE, false);
	}

	@Test
	public void messageIsNotSetToNullIfSecureMessagePayloadLengthIsNotZero() {
		// Assert:
		this.assertMessageFieldIsNull(new byte[] { 1 }, MessageTypes.SECURE, false);
	}

	private void assertMessageFieldIsNull(final byte[] messageBytes, final int messageType, final boolean isNullMessageExpected) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		Message message = null;
		if (null != messageBytes) {
			message = MessageTypes.PLAIN == messageType
					? new PlainMessage(messageBytes)
					: SecureMessage.fromDecodedPayload(signer, recipient, messageBytes);
		}

		final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, message);
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");

		// Act:
		final TransferTransaction transaction = new TransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);

		// Assert:
		Assert.assertThat(transaction.getMessage(), isNullMessageExpected ? IsNull.nullValue() : IsEqual.equalTo(message));
	}

	//endregion

	//region getAccounts

	@Test
	public void getAccountsIncludesSignerAndRecipientAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, null);

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer, recipient));
	}

	//endregion

	//region Execute

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, null);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), recipient);
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(1), signer, recipient, Amount.fromNem(99));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(2), signer, Amount.fromNem(10));
	}

	//endregion

	//region undo

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, null);
		transaction.setFee(Amount.fromNem(10));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), recipient);
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(1), recipient, signer, Amount.fromNem(99));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), signer, Amount.fromNem(10));
	}

	//endregion

	//region Secure Message Consistency

	@Test
	public void consistentSecureMessageCanBeDecoded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account senderPublicKeyOnly = Utils.createPublicOnlyKeyAccount(sender);
		final Account recipient = Utils.generateRandomAccount();
		final Account recipientPublicKeyOnly = Utils.createPublicOnlyKeyAccount(recipient);
		final Message message = SecureMessage.fromDecodedPayload(sender, recipientPublicKeyOnly, new byte[] { 1, 2, 3 });
		final TransferTransaction originalTransaction = this.createTransferTransaction(sender, recipientPublicKeyOnly, 1L, message);

		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(senderPublicKeyOnly);
		accountLookup.setMockAccount(recipient);

		// Act:
		final TransferTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getMessage().canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 3 }));
	}

	@Test
	public void secureMessageCannotBeDecodedWithoutSenderAndRecipientPrivateKey() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Message message = SecureMessage.fromDecodedPayload(sender, recipient, new byte[] { 1, 2, 3 });
		final TransferTransaction originalTransaction = this.createTransferTransaction(sender, recipient, 1L, message);

		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(sender));
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(recipient));

		// Act:
		final TransferTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getMessage().canDecode(), IsEqual.equalTo(false));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsNull.nullValue());
	}

	//endregion

	private TransferTransaction createTransferTransaction(
			final Account sender,
			final Account recipient,
			final long amount,
			final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message);
	}

	private TransferTransaction createTransferTransaction(
			final Account sender,
			final Account recipient,
			final long amount,
			final Message message,
			final SmartTileBag smartTileBag) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message, smartTileBag);
	}

	private TransferTransaction createRoundTrippedTransaction(
			final Transaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new TransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	private static Collection<SmartTile> createSmartTiles() {
		return IntStream.range(0, 5).mapToObj(Utils::createSmartTile).collect(Collectors.toList());
	}
}