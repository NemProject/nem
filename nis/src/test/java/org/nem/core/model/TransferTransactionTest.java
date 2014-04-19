package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransferTransactionTest {

	//region Constructor

	@Test(expected = IllegalArgumentException.class)
	public void recipientIsRequired() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		createTransferTransaction(signer, null, 123, message);
	}

	@Test
	public void ctorCanCreateTransactionWithMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

		// Act:
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 123, message);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(new Amount(123L)));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
	}

	@Test
	public void ctorCanCreateTransactionWithoutMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

		// Act:
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 123, null);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(new Amount(123L)));
		Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(null));
	}

	@Test
	public void transactionCanBeRoundTrippedWithMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		final Message message = new PlainMessage(new byte[] { 12, 50, 21 });
		final TransferTransaction originalTransaction = createTransferTransaction(signer, recipient, 123, message);
		final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, new AccountLookup() {
			public Account findByAddress(final Address address) {
				return address.equals(signer.getAddress()) ? signer : recipient;
			}
		});

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(new Amount(123L)));
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
	}

	@Test
	public void transactionCanBeRoundTrippedWithoutMessage() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
		final TransferTransaction originalTransaction = createTransferTransaction(signer, recipient, 123, null);
		final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, new AccountLookup() {
			public Account findByAddress(final Address address) {
				return address.equals(signer.getAddress()) ? signer : recipient;
			}
		});

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(new Amount(123L)));
		Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(null));
	}

	//endregion

	//region Fee

	@Test
	public void feeIsCalculatedCorrectlyForEmptyTransaction() {
		// Assert:
		Assert.assertThat(calculateFee(0, 0), IsEqual.equalTo(1L));
	}

	@Test
	public void feeIsCalculatedCorrectlyForTransactionWithoutMessage() {
		// Assert:
		Assert.assertThat(calculateFee(12000, 0), IsEqual.equalTo(12L));
		Assert.assertThat(calculateFee(12001, 0), IsEqual.equalTo(13L));
		Assert.assertThat(calculateFee(13000, 0), IsEqual.equalTo(13L));
	}

	@Test
	public void feeIsCalculatedCorrectlyForTransactionWithMessage() {
		// Assert:
		Assert.assertThat(calculateFee(12000, 1), IsEqual.equalTo(13L));
		Assert.assertThat(calculateFee(12000, 199), IsEqual.equalTo(13L));
		Assert.assertThat(calculateFee(13000, 200), IsEqual.equalTo(14L));
	}

	@Test
	public void feeIsWaivedForGenesisAccount() {
		// Assert:
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 0, 0), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 12000, 0), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 12001, 0), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 13000, 0), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 12000, 1), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 12000, 199), IsEqual.equalTo(0L));
		Assert.assertThat(calculateFee(GenesisBlock.ACCOUNT, 13000, 200), IsEqual.equalTo(0L));
	}

	@Test
	public void messageFeeIsBasedOnEncodedSize() {
		// Assert:
		Assert.assertThat(calculateMessageFee(1000, 2000), IsEqual.equalTo(5L));
		Assert.assertThat(calculateMessageFee(2000, 3000), IsEqual.equalTo(10L));
	}

	private long calculateFee(final long amount, final int messageSize) {
		// Act:
		return calculateFee(Utils.generateRandomAccount(), amount, messageSize);
	}

	private long calculateFee(final Account signer, final long amount, final int messageSize) {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		final TransferTransaction transaction = createTransferTransaction(signer, recipient, amount, message);

		// Act:
		return transaction.getFee().getNumMicroNem();
	}

	private long calculateMessageFee(final int encodedMessageSize, final int decodedMessageSize) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final MockMessage message = new MockMessage(7);
		message.setEncodedPayload(new byte[encodedMessageSize]);
		message.setDecodedPayload(new byte[decodedMessageSize]);
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 0, message);

		// Act:
		return transaction.getFee().getNumMicroNem();
	}

	//endregion

	//region Valid

	@Test
	public void isValidChecksSuperValidity() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		Transaction transaction = new TransferTransaction(new TimeInstant(1), signer, recipient, new Amount(1), null);
		transaction.setDeadline(TimeInstant.ZERO);

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
	}

	@Test
	public void transactionsWithNonNegativeAmountAreValid() {
		// Assert:
		Assert.assertThat(isTransactionAmountValid(100, 0, 1), IsEqual.equalTo(true));
		Assert.assertThat(isTransactionAmountValid(1000, 1, 10), IsEqual.equalTo(true));
	}

	@Test
	public void transactionsUpToSignerBalanceAreValid() {
		// Assert:
		Assert.assertThat(isTransactionAmountValid(100, 10, 1), IsEqual.equalTo(true));
		Assert.assertThat(isTransactionAmountValid(1000, 990, 10), IsEqual.equalTo(true));
		Assert.assertThat(isTransactionAmountValid(1000, 50, 950), IsEqual.equalTo(true));
	}

	@Test
	public void transactionsExceedingSignerBalanceAreInvalid() {
		// Assert:
		Assert.assertThat(isTransactionAmountValid(1000, 990, 11), IsEqual.equalTo(false));
		Assert.assertThat(isTransactionAmountValid(1000, 51, 950), IsEqual.equalTo(false));
		Assert.assertThat(isTransactionAmountValid(1000, 1001, 11), IsEqual.equalTo(false));
		Assert.assertThat(isTransactionAmountValid(1000, 51, 1001), IsEqual.equalTo(false));
	}

	private boolean isTransactionAmountValid(final int senderBalance, final int amount, final int fee) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(senderBalance));
		final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = createTransferTransaction(signer, recipient, amount, null);
		transaction.setFee(new Amount(fee));
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Act:
		return transaction.isValid();
	}

	@Test
	public void smallMessagesAreValid() {
		// Assert:
		Assert.assertThat(isMessageSizeValid(0), IsEqual.equalTo(true));
		Assert.assertThat(isMessageSizeValid(999), IsEqual.equalTo(true));
		Assert.assertThat(isMessageSizeValid(1000), IsEqual.equalTo(true));
	}

	@Test
	public void largeMessagesAreInvalid() {
		// Assert:
		Assert.assertThat(isMessageSizeValid(1001), IsEqual.equalTo(false));
	}

	private boolean isMessageSizeValid(final int messageSize) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(1000));
		final Account recipient = Utils.generateRandomAccount();
		final PlainMessage message = new PlainMessage(new byte[messageSize]);
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 1, message);
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Act:
		return transaction.isValid();
	}

	//endregion

	//region Execute

	@Test
	public void executeTransfersAmountAndFeeFromSigner() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(1000));
		final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 99, null);
		transaction.setFee(new Amount(10));

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(signer.getBalance(), IsEqual.equalTo(new Amount(891L)));
	}

	@Test
	public void executeTransfersAmountToSigner() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(1000));
		final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 99, null);
		transaction.setFee(new Amount(10));

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(recipient.getBalance(), IsEqual.equalTo(new Amount(99L)));
	}

	@Test
	public void executeDoesNotAppendEmptyMessageToAccount() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(1000));
		final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 99, null);
		transaction.setFee(new Amount(10));

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(recipient.getMessages().size(), IsEqual.equalTo(0));
	}

	@Test
	public void executeAppendsNonEmptyMessageToAccount() {
		// Arrange:
		final Message message = new PlainMessage(new byte[] { 0x12, 0x33, 0x0A });
		final Account signer = Utils.generateRandomAccount();
		signer.incrementBalance(new Amount(1000));
		final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = createTransferTransaction(signer, recipient, 99, message);
		transaction.setFee(new Amount(10));

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(recipient.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(recipient.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(new byte[] { 0x12, 0x33, 0x0A }));
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
		final TransferTransaction originalTransaction = createTransferTransaction(sender, recipientPublicKeyOnly, 1L, message);

		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(senderPublicKeyOnly);
		accountLookup.setMockAccount(recipient);

		// Act:
		final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 3 }));
	}

	@Test
	public void inconsistentSecureMessageCannotBeDecoded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account messageSender = Utils.generateRandomAccount();
		final Message message = SecureMessage.fromDecodedPayload(messageSender, recipient, new byte[] { 1, 2, 3 });
		final TransferTransaction originalTransaction = createTransferTransaction(sender, recipient, 1L, message);

		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(sender);
		accountLookup.setMockAccount(recipient);

		// Act:
		final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

		// Assert:
		Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(null));
	}

	//endregion

	private TransferTransaction createTransferTransaction(final Account sender, final Account recipient, final long amount, final Message message) {
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(amount), message);
	}

	private TransferTransaction createRoundTrippedTransaction(
			final Transaction originalTransaction,
			final AccountLookup accountLookup) {
		// Act:
		Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
		deserializer.readInt("type");
		return new TransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}
}