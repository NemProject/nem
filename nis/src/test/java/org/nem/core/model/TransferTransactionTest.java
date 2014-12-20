package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.messages.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.Collection;

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
	public void ctorCanCreateTransactionWithMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, message);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123L)));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
		Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(3));
	}

	@Test
	public void ctorCanCreateTransactionWithoutMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

		// Act:
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, null);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123L)));
		Assert.assertThat(transaction.getMessage(), IsNull.nullValue());
		Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(0));
	}

	@Test
	public void transactionCanBeRoundTrippedWithMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });
		final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, message);

		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
		final TransferTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123L)));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
	}

	@Test
	public void transactionCanBeRoundTrippedWithoutMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, null);

		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
		final TransferTransaction transaction = this.createRoundTrippedTransaction(
				originalTransaction,
				accountLookup);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123L)));
		Assert.assertThat(transaction.getMessage(), IsNull.nullValue());
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

	//region Fee

	@Test
	public void feeIsCalculatedCorrectlyForEmptyTransaction() {
		// Assert:
		Assert.assertThat(this.calculateFee(0, 0), IsEqual.equalTo(Amount.fromNem(1)));
	}

	@Test
	public void feeIsCalculatedCorrectlyForTransactionWithoutMessage() {
		// Assert:
		Assert.assertThat(this.calculateFee(1, 0), IsEqual.equalTo(Amount.fromNem(1)));
		Assert.assertThat(this.calculateFee(144, 0), IsEqual.equalTo(Amount.fromNem(1)));
		Assert.assertThat(this.calculateFee(145, 0), IsEqual.equalTo(Amount.fromNem(2)));
		Assert.assertThat(this.calculateFee(1024, 0), IsEqual.equalTo(Amount.fromNem(2)));
		Assert.assertThat(this.calculateFee(32768, 0), IsEqual.equalTo(Amount.fromNem(4)));
		Assert.assertThat(this.calculateFee(1048576, 0), IsEqual.equalTo(Amount.fromNem(45)));
	}

	@Test
	public void feeIsCalculatedCorrectlyForTransactionWithMessage() {
		// Assert:
		Assert.assertThat(this.calculateFee(1024, 1), IsEqual.equalTo(Amount.fromNem(3)));
		Assert.assertThat(this.calculateFee(1024, 255), IsEqual.equalTo(Amount.fromNem(6)));
		Assert.assertThat(this.calculateFee(1024, 256), IsEqual.equalTo(Amount.fromNem(7)));
		Assert.assertThat(this.calculateFee(1024, 257), IsEqual.equalTo(Amount.fromNem(7)));
		Assert.assertThat(this.calculateFee(1024, 512), IsEqual.equalTo(Amount.fromNem(12)));
	}

	@Test
	public void feeIsWaivedForNemesisAccount() {
		// Arrange:
		final Account nemesisAccount = new Account(NemesisBlock.ADDRESS);

		// Assert:
		Assert.assertThat(this.calculateFee(nemesisAccount, 0, 0), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 12000, 0), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 12001, 0), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 13000, 0), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 12000, 1), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 12000, 199), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(this.calculateFee(nemesisAccount, 13000, 200), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void messageFeeIsBasedOnEncodedSize() {
		// Assert:
		Assert.assertThat(this.calculateMessageFee(256, 512), IsEqual.equalTo(Amount.fromNem(6)));
		Assert.assertThat(this.calculateMessageFee(512, 256), IsEqual.equalTo(Amount.fromNem(11)));
	}

	private Amount calculateFee(final long amount, final int messageSize) {
		// Act:
		return this.calculateFee(Utils.generateRandomAccount(), amount, messageSize);
	}

	private Amount calculateFee(final Account signer, final long amount, final int messageSize) {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, amount, message);

		// Act:
		return transaction.getFee();
	}

	private Amount calculateMessageFee(final int encodedMessageSize, final int decodedMessageSize) {
		// Arrange:
		final TransferTransaction transaction = this.createTransactionWithMockMessage(encodedMessageSize, decodedMessageSize);

		// Act:
		return transaction.getFee();
	}

	//endregion

	//region getAccounts

	@Test
	public void getAccountsReturnsCorrectAccounts() {
		// Arrange:
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.Activate;
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

	private TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount, final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), message);
	}

	private TransferTransaction createRoundTrippedTransaction(
			final Transaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new TransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}
}