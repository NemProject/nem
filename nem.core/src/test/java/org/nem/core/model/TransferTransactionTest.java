package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.messages.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.*;

@RunWith(Enclosed.class)
public class TransferTransactionTest {
	private static final Account MOSAIC_RECIPIENT8 = Utils.generateRandomAccount();
	private static final Account MOSAIC_RECIPIENT9 = Utils.generateRandomAccount();
	private static final Account MOSAIC_RECIPIENT11 = Utils.generateRandomAccount();

	//region cross versions

	public static class Main {
		@Test
		public void ctorCreatesTransferTransactionVersionTwoByDefault() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();

			// Act:
			final TransferTransaction transaction = createTransferTransaction(signer, recipient, 123);

			// Assert:
			Assert.assertThat(transaction.getEntityVersion(), IsEqual.equalTo(2));
		}

		@Test
		public void canCreateTransferTransactionWithDifferentVersionThanTwo() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();

			// Act:
			final TransferTransaction transaction1 = createTransferTransaction(1, signer, recipient, 123);
			final TransferTransaction transaction2 = createTransferTransaction(123, signer, recipient, 123);

			// Assert:
			Assert.assertThat(transaction1.getEntityVersion(), IsEqual.equalTo(1));
			Assert.assertThat(transaction2.getEntityVersion(), IsEqual.equalTo(123));
		}

		private static TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final long amount) {
			return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), null);
		}

		private static TransferTransaction createTransferTransaction(
				final int version,
				final Account sender,
				final Account recipient,
				final long amount) {
			return new TransferTransaction(version, TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), null);
		}
	}

	//endregion

	private static abstract class AbstractTransferTransactionTest {
		protected static final Amount ONE_POINT_TWO_XEM = Amount.fromNem(1).add(Amount.fromMicroNem(Amount.MICRONEMS_IN_NEM / 5));

		protected static void setupGlobalsBase() {
			NemGlobals.setTransactionFeeCalculator(new TransactionFeeCalculator() {
				@Override
				public Amount calculateMinimumFee(final Transaction transaction) {
					return Amount.ZERO;
				}

				@Override
				public boolean isFeeValid(final Transaction transaction, final BlockHeight blockHeight) {
					return true;
				}
			});

			NemGlobals.setMosaicTransferFeeCalculator(mosaic -> {
				if (mosaic.getMosaicId().equals(Utils.createMosaicId(8))) {
					return createTestLevy(MOSAIC_RECIPIENT8, MosaicConstants.MOSAIC_ID_XEM, 32);
				}

				if (mosaic.getMosaicId().equals(Utils.createMosaicId(9))) {
					return createTestLevy(MOSAIC_RECIPIENT9, Utils.createMosaicId(19), 14);
				}

				if (mosaic.getMosaicId().equals(Utils.createMosaicId(11))) {
					return createTestLevy(MOSAIC_RECIPIENT11, Utils.createMosaicId(21), 16);
				}

				return null;
			});
		}

		private static MosaicLevy createTestLevy(final Account recipient, final MosaicId mosaicId, final int value) {
			return new MosaicLevy(
					MosaicTransferFeeType.Absolute,
					recipient,
					mosaicId,
					Quantity.fromValue(value));
		}

		protected static void resetGlobalsBase() {
			Utils.resetGlobals();
		}

		//region createTransferTransaction

		protected abstract TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TransferTransactionAttachment attachment);

		protected TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final long amount,
				final TransferTransactionAttachment attachment) {
			return this.createTransferTransaction(sender, recipient, Amount.fromNem(amount), attachment);
		}

		protected TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final long amount,
				final Message message,
				Collection<Mosaic> mosaics) {
			mosaics = null == mosaics ? Collections.emptyList() : mosaics;
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);
			mosaics.forEach(attachment::addMosaic);
			return this.createTransferTransaction(sender, recipient, amount, attachment);
		}

		protected TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final long amount,
				final Message message) {
			return this.createTransferTransaction(sender, recipient, amount, message, null);
		}

		//endregion

		//region basic

		//region constructor

		@Test(expected = IllegalArgumentException.class)
		public void recipientIsRequired() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Message message = new PlainMessage(new byte[] { 12, 50, 21 });

			// Act:
			this.createTransferTransaction(signer, null, 123, message);
		}

		@Test
		public void canCreateTransactionWithMessageAndWithoutMosaics() {
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
			Assert.assertThat(transaction.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
		}

		@Test
		public void canCreateTransactionWithoutMessageAndWithoutMosaics() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();

			// Act:
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, (Message)null);

			// Assert:
			assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
			Assert.assertThat(transaction.getMessage(), IsNull.nullValue());
			Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(0));
			Assert.assertThat(transaction.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
		}

		@Test
		public void canCreateTransactionWithoutMessageAndWithMosaics() { // TODO
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Collection<Mosaic> mosaics = createMosaics();

			// Act:
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, null, mosaics);

			// Assert:
			assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
			Assert.assertThat(transaction.getAttachment().getMosaics(), IsEquivalent.equivalentTo(mosaics));
		}

		@Test
		public void canCreateTransactionWithMessageAndWithMosaics() { // TODO
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Message message = new PlainMessage(new byte[] { 12, 50, 21 });
			final Collection<Mosaic> mosaics = createMosaics();

			// Act:
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 123, message, mosaics);

			// Assert:
			assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
			Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
			Assert.assertThat(transaction.getMessageLength(), IsEqual.equalTo(3));
			Assert.assertThat(transaction.getAttachment().getMosaics(), IsEquivalent.equivalentTo(mosaics));
		}

		//endregion

		//region transfer accessors

		@Test
		public void getXemTransferAmountReturnsAmountWhenNoMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithoutMosaics(ONE_POINT_TWO_XEM);

			// Act:
			final Amount amount = transaction.getXemTransferAmount();

			// Act:
			Assert.assertThat(amount, IsEqual.equalTo(ONE_POINT_TWO_XEM));
		}

		@Test
		public void getMosaicsReturnsEmptyWhenNoMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithoutMosaics(ONE_POINT_TWO_XEM);

			// Act:
			final Collection<Mosaic> mosaics = transaction.getMosaics();

			// Act:
			Assert.assertThat(mosaics.isEmpty(), IsEqual.equalTo(true));
		}

		private TransferTransaction createTransferWithoutMosaics(final Amount amount) {
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			return this.createTransferTransaction(signer, recipient, amount, null);
		}

		//endregion

		//region round trip

		@Test
		public void transactionCanBeRoundTrippedWithMessageAndWithoutMosaics() {
			// Arrange:
			this.assertCanBeRoundTripped(new byte[] { 12, 50, 21 }, null);
		}

		@Test
		public void transactionCanBeRoundTrippedWithoutMessageAndWithoutMosaics() {
			// Assert:
			this.assertCanBeRoundTripped(null, null);
		}

		//endregion

		protected void assertCanBeRoundTripped(final byte[] messageBytes, final Collection<Mosaic> mosaics) {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
			final Message message = null == messageBytes ? null : new PlainMessage(messageBytes);
			final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, message, mosaics);

			final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
			final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

			// Assert:
			assertTransactionFields(transaction, signer, recipient, Amount.fromNem(123L));
			Assert.assertThat(
					null == message ? transaction.getMessage() : transaction.getMessage().getDecodedPayload(),
					IsEqual.equalTo(null == message ? null : messageBytes));
			Assert.assertThat(
					transaction.getAttachment().getMosaics(),
					IsEquivalent.equivalentTo(null == mosaics ? Collections.emptyList() : mosaics));
		}

		private static void assertTransactionFields(
				final TransferTransaction transaction,
				final Account signer,
				final Account recipient,
				final Amount amount) {
			Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
			Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(amount));
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
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, (Message)null);

			// Act:
			final Collection<Account> accounts = transaction.getAccounts();

			// Assert:
			Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer, recipient));
		}

		//endregion

		//region execute

		@Test
		public void executeRaisesAppropriateNotificationsForXemTransfers() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, (Message)null);
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
		public void undoRaisesAppropriateNotificationsForXemTransfers() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final TransferTransaction transaction = this.createTransferTransaction(signer, recipient, 99, (Message)null);
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
			final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

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
			final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

			// Assert:
			Assert.assertThat(transaction.getMessage().canDecode(), IsEqual.equalTo(false));
			Assert.assertThat(transaction.getMessage().getDecodedPayload(), IsNull.nullValue());
		}

		//endregion

		protected static TransferTransaction createRoundTrippedTransaction(
				final Transaction originalTransaction,
				final AccountLookup accountLookup) {
			// Act:
			final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
			deserializer.readInt("type");
			return new TransferTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
		}

		protected static Collection<Mosaic> createMosaics() {
			return IntStream.range(0, 5).mapToObj(Utils::createMosaic).collect(Collectors.toList());
		}
	}

	public static class AbstractTransferTransactionV1Test extends AbstractTransferTransactionTest {

		// TODO 20150806 J-J: we should really fix how we do v1 transactions since the fee setups shouldn't be needed here!

		@BeforeClass
		public static void setupGlobals() {
			setupGlobalsBase();
		}

		@AfterClass
		public static void resetGlobals() {
			resetGlobalsBase();
		}

		protected TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TransferTransactionAttachment attachment) {
			return new TransferTransaction(1, TimeInstant.ZERO, sender, recipient, amount, attachment);
		}

		//region deserialization

		@Test
		public void transactionCannotBeRoundTrippedWithoutMessageAndWithMosaics() { // TODO
			// Arrange:
			this.assertCannotBeRoundTripped(null);
		}

		@Test
		public void transactionCannotBeRoundTrippedWithMessageAndWithMosaics() { // TODO
			// Assert:
			this.assertCannotBeRoundTripped(new byte[] { 12, 50, 21 });
		}

		protected void assertCannotBeRoundTripped(final byte[] messageBytes) {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
			final Message message = null == messageBytes ? null : new PlainMessage(messageBytes);
			final TransferTransaction originalTransaction = this.createTransferTransaction(signer, recipient, 123, message, createMosaics());

			final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, recipient);
			final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, accountLookup);

			// Assert: the mosaic transfers are not persisted in v1 transactions
			Assert.assertThat(originalTransaction.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(false));
			Assert.assertThat(transaction.getAttachment().getMosaics().isEmpty(), IsEqual.equalTo(true));
		}

		//endregion
	}

	public static class AbstractTransferTransactionV2Test extends AbstractTransferTransactionTest {

		@BeforeClass
		public static void setupGlobals() {
			setupGlobalsBase();
		}

		@AfterClass
		public static void resetGlobals() {
			resetGlobalsBase();
		}

		protected TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TransferTransactionAttachment attachment) {
			return new TransferTransaction(2, TimeInstant.ZERO, sender, recipient, amount, attachment);
		}

		//region deserialization

		@Test
		public void transactionCanBeRoundTrippedWithoutMessageAndWithMosaics() {
			// Arrange:
			this.assertCanBeRoundTripped(null, createMosaics());
		}

		@Test
		public void transactionCanBeRoundTrippedWithMessageAndWithMosaics() {
			// Assert:
			this.assertCanBeRoundTripped(new byte[] { 12, 50, 21 }, createMosaics());
		}

		//endregion

		//region execute /undo

		// note: in the following execute/undo tests, mosaic transfers with mosaic id 7 do not trigger an additional transfer fee notification

		@Test
		public void executeRaisesAppropriateNotificationsForMosaicTransfers() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaics(signer, recipient, false);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(7)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(0), recipient);
			NotificationUtils.assertMosaicTransferNotification(notifications.get(1), signer, recipient, Utils.createMosaicId(11), new Quantity(5 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), signer, MOSAIC_RECIPIENT11, Utils.createMosaicId(21), new Quantity(16));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(3), signer, recipient, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(4), signer, recipient, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(5), signer, MOSAIC_RECIPIENT9, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceDebitNotification(notifications.get(6), signer, Amount.fromNem(10));
		}

		@Test
		public void executeRaisesAppropriateNotificationsForMosaicTransfersIncludingXem() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaics(signer, recipient, true);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(6)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(0), recipient);
			NotificationUtils.assertBalanceTransferNotification(notifications.get(1), signer, recipient, Amount.fromMicroNem(5 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), signer, recipient, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(3), signer, recipient, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(4), signer, MOSAIC_RECIPIENT9, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceDebitNotification(notifications.get(5), signer, Amount.fromNem(10));
		}

		@Test
		public void executeRaisesAppropriateNotificationsForMosaicTransfersWithXemLevy() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaicContainingXemLevy(signer, recipient);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(7)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(0), recipient);
			NotificationUtils.assertMosaicTransferNotification(notifications.get(1), signer, recipient, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), signer, recipient, Utils.createMosaicId(8), new Quantity(30 * 20));
			NotificationUtils.assertBalanceTransferNotification(notifications.get(3), signer, MOSAIC_RECIPIENT8, Amount.fromMicroNem(32));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(4), signer, recipient, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(5), signer, MOSAIC_RECIPIENT9, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceDebitNotification(notifications.get(6), signer, Amount.fromNem(10));
		}

		@Test
		public void undoRaisesAppropriateNotificationsForMosaicTransfers() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaics(signer, recipient, false);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.undo(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(7)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(6), recipient);
			NotificationUtils.assertMosaicTransferNotification(notifications.get(5), recipient, signer, Utils.createMosaicId(11), new Quantity(5 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(4), MOSAIC_RECIPIENT11, signer, Utils.createMosaicId(21), new Quantity(16));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(3), recipient, signer, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), recipient, signer, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(1), MOSAIC_RECIPIENT9, signer, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceCreditNotification(notifications.get(0), signer, Amount.fromNem(10));
		}

		@Test
		public void undoRaisesAppropriateNotificationsForMosaicTransfersIncludingXem() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaics(signer, recipient, true);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.undo(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(6)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(5), recipient);
			NotificationUtils.assertBalanceTransferNotification(notifications.get(4), recipient, signer, Amount.fromMicroNem(5 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(3), recipient, signer, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), recipient, signer, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(1), MOSAIC_RECIPIENT9, signer, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceCreditNotification(notifications.get(0), signer, Amount.fromNem(10));
		}

		@Test
		public void undoRaisesAppropriateNotificationsForMosaicTransfersWithXemLevy() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = this.createTransferWithMosaicContainingXemLevy(signer, recipient);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.undo(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(7)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();

			NotificationUtils.assertAccountNotification(notifications.get(6), recipient);
			NotificationUtils.assertMosaicTransferNotification(notifications.get(5), recipient, signer, Utils.createMosaicId(7), new Quantity(12 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(4), recipient, signer, Utils.createMosaicId(8), new Quantity(30 * 20));
			NotificationUtils.assertBalanceTransferNotification(notifications.get(3), MOSAIC_RECIPIENT8, signer, Amount.fromMicroNem(32));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(2), recipient, signer, Utils.createMosaicId(9), new Quantity(24 * 20));
			NotificationUtils.assertMosaicTransferNotification(notifications.get(1), MOSAIC_RECIPIENT9, signer, Utils.createMosaicId(19), new Quantity(14));
			NotificationUtils.assertBalanceCreditNotification(notifications.get(0), signer, Amount.fromNem(10));
		}

		private Transaction createTransferWithMosaics(final Account signer, final Account recipient, final boolean transferXem) {
			final Collection<Mosaic> mosaics = Arrays.asList(
					Utils.createMosaic(7, 12),
					transferXem
							? new Mosaic(Utils.createMosaicDefinition("nem", "xem").getId(), new Quantity(5))
							: Utils.createMosaic(11, 5),
					Utils.createMosaic(9, 24));
			return this.createTransferWithMosaics(signer, recipient, mosaics);
		}

		private Transaction createTransferWithMosaicContainingXemLevy(final Account signer, final Account recipient) {
			final Collection<Mosaic> mosaics = Arrays.asList(
					Utils.createMosaic(7, 12),
					Utils.createMosaic(8, 30),
					Utils.createMosaic(9, 24));
			return this.createTransferWithMosaics(signer, recipient, mosaics);
		}

		private Transaction createTransferWithMosaics(final Account signer, final Account recipient, final Collection<Mosaic> mosaics) {
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			mosaics.forEach(attachment::addMosaic);
			final Transaction transaction = this.createTransferTransaction(signer, recipient, 20, attachment);
			transaction.setFee(Amount.fromNem(10));
			return transaction;
		}

		//endregion

		//region transfer accessors

		@Test
		public void getXemTransferAmountReturnsNullWhenNoXemMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithMosaics(ONE_POINT_TWO_XEM, false);

			// Act:
			final Amount amount = transaction.getXemTransferAmount();

			// Act:
			Assert.assertThat(amount, IsNull.nullValue());
		}

		@Test
		public void getXemTransferAmountReturnsAmountWhenXemMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithMosaics(ONE_POINT_TWO_XEM, true);

			// Act:
			final Amount amount = transaction.getXemTransferAmount();

			// Act:
			Assert.assertThat(amount, IsEqual.equalTo(Amount.fromMicroNem(6))); // 5 * 1.2
		}

		@Test
		public void getMosaicsReturnsAllMosaicsWhenNonXemMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithMosaics(ONE_POINT_TWO_XEM, false);

			// Act:
			final Collection<Mosaic> mosaics = transaction.getMosaics();

			// Act:
			final Collection<Mosaic> expectedMosaics = Arrays.asList(
					Utils.createMosaic(7, 14),    // 12 * 1.2 = 14.4
					Utils.createMosaic(11, 6),    //  5 * 1.2 =  6.0
					Utils.createMosaic(9, 28));   // 24 * 1.2 = 28.8
			Assert.assertThat(mosaics, IsEquivalent.equivalentTo(expectedMosaics));
		}

		@Test
		public void getMosaicsReturnsNonXemMosaicsWhenXemMosaicsArePresent() {
			// Arrange:
			final TransferTransaction transaction = this.createTransferWithMosaics(ONE_POINT_TWO_XEM, true);

			// Act:
			final Collection<Mosaic> mosaics = transaction.getMosaics();

			// Act:
			final Collection<Mosaic> expectedMosaics = Arrays.asList(
					Utils.createMosaic(7, 14),    // 12 * 1.2 = 14.4
					Utils.createMosaic(9, 28));   // 24 * 1.2 = 28.8
			Assert.assertThat(mosaics, IsEquivalent.equivalentTo(expectedMosaics));
		}

		private TransferTransaction createTransferWithMosaics(final Amount amount, final boolean transferXem) {
			final Account signer = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Collection<Mosaic> mosaics = Arrays.asList(
					Utils.createMosaic(7, 12),
					transferXem
							? new Mosaic(Utils.createMosaicDefinition("nem", "xem").getId(), new Quantity(5))
							: Utils.createMosaic(11, 5),
					Utils.createMosaic(9, 24));
			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			mosaics.forEach(attachment::addMosaic);
			return this.createTransferTransaction(signer, recipient, amount, attachment);
		}

		//endregion
	}
}
