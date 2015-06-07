package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.DefaultHashCache;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.service.AccountIoAdapter;
import org.nem.specific.deploy.NisConfiguration;

import java.util.*;

@RunWith(Enclosed.class)
public class AccountTransfersControllerTest {

	private static abstract class SingleDirectionTransfersTest {

		protected abstract ReadOnlyTransferDao.TransferType getTransferType();

		protected abstract int getLocalCallPattern();

		protected abstract SerializableList<TransactionMetaDataPair> execute(
				final AccountTransfersController controller,
				final AccountTransactionsPageBuilder builder);

		protected abstract SerializableList<TransactionMetaDataPair> executeLocal(
				final AccountTransfersController controller,
				final AccountPrivateKeyTransactionsPage page);

		//region accountTransfers

		@Test
		public void accountTransfersDelegatesToIoAdapterWhenIdIsProvided() {
			// Arrange:
			final Address address = Utils.generateRandomAddress();
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);

			final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
			pageBuilder.setAddress(address.getEncoded());
			pageBuilder.setHash("ffeeddccbbaa99887766554433221100");
			pageBuilder.setId("1");

			Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, 1L, this.getTransferType())).thenReturn(expectedList);

			// Act:
			final SerializableList<TransactionMetaDataPair> resultList = this.execute(context.controller, pageBuilder);

			// Assert:
			Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
			Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingId(address, 1L, this.getTransferType());
		}

		@Test
		public void accountTransfersUsesIoAdapterWhenHashIsProvided() {
			// Arrange:
			final Address address = Utils.generateRandomAddress();
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);
			context.enableHashLookup();

			final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");
			final HashMetaData metaData = new HashMetaData(new BlockHeight(12), new TimeInstant(123));

			final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
			pageBuilder.setAddress(address.getEncoded());
			pageBuilder.setHash(hash.toString());

			Mockito.when(context.transactionHashCache.get(hash)).thenReturn(metaData);
			Mockito.when(accountIoAdapter.getAccountTransfersUsingHash(address, hash, new BlockHeight(12), this.getTransferType()))
					.thenReturn(expectedList);

			// Act:
			final SerializableList<TransactionMetaDataPair> resultList = this.execute(context.controller, pageBuilder);

			// Assert:
			Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
			Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingHash(address, hash, new BlockHeight(12), this.getTransferType());
			Mockito.verify(context.transactionHashCache, Mockito.times(1)).get(hash);
		}

		@Test
		public void accountTransfersDelegatesToIoWhenNeitherIdNorHashIsProvided() {
			// Arrange:
			final Address address = Utils.generateRandomAddress();
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);

			final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
			pageBuilder.setAddress(address.getEncoded());

			Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, this.getTransferType())).thenReturn(expectedList);

			// Act:
			final SerializableList<TransactionMetaDataPair> resultList = this.execute(context.controller, pageBuilder);

			// Assert:
			Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
			Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingId(address, null, this.getTransferType());
		}

		@Test
		public void accountTransfersFailsWhenUnknownHashIsProvided() {
			// Arrange:
			final Address address = Utils.generateRandomAddress();
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);
			context.enableHashLookup();

			final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");

			final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
			pageBuilder.setAddress(address.getEncoded());
			pageBuilder.setHash(hash.toString());

			Mockito.when(context.transactionHashCache.get(hash)).thenReturn(null);

			// Act:
			ExceptionAssert.assertThrows(
					v -> this.execute(context.controller, pageBuilder),
					IllegalArgumentException.class);
		}

		@Test
		public void accountTransfersFailsWhenHashIsProvidedAndHashLookupIsNotSupported() {
			// Arrange:
			final Address address = Utils.generateRandomAddress();
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);

			final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");
			final HashMetaData metaData = new HashMetaData(new BlockHeight(12), new TimeInstant(123));

			final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
			pageBuilder.setAddress(address.getEncoded());
			pageBuilder.setHash(hash.toString());

			Mockito.when(context.transactionHashCache.get(hash)).thenReturn(metaData);
			Mockito.when(accountIoAdapter.getAccountTransfersUsingHash(address, hash, new BlockHeight(12), this.getTransferType()))
					.thenReturn(expectedList);

			// Act:
			ExceptionAssert.assertThrows(
					v -> this.execute(context.controller, pageBuilder),
					UnsupportedOperationException.class);
		}

		//endregion

		//region localAccountTransfers

		@Test
		public void localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible() {
			// Arrange:
			final KeyPair senderKeyPair = new KeyPair();
			final KeyPair recipientKeyPair = new KeyPair();
			final Address address = Address.fromPublicKey(senderKeyPair.getPublicKey());
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);
			final TransactionMetaDataPair pair = createPairWithDecodableSecureMessage(
					senderKeyPair,
					recipientKeyPair,
					"This is a secret message");
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(Collections.singletonList(pair));
			final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
			Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, this.getTransferType())).thenReturn(expectedList);

			// Act:
			final SerializableList<TransactionMetaDataPair> resultList = this.executeLocal(context.controller, pagePrivateKeyPair);

			// Assert:
			final TransferTransaction tx = (TransferTransaction)resultList.get(0).getTransaction();
			Assert.assertThat(tx, IsNot.not(IsSame.sameInstance(pair.getTransaction())));
			Assert.assertThat(tx.getMessage(), IsInstanceOf.instanceOf(PlainMessage.class));
			Assert.assertThat(new String(tx.getMessage().getDecodedPayload()), IsEqual.equalTo("This is a secret message"));
		}

		@Test
		public void localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
			// Arrange:
			final KeyPair senderKeyPair = new KeyPair();
			final Address address = Address.fromPublicKey(senderKeyPair.getPublicKey());
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);
			final TransactionMetaDataPair pair = createPairWithUndecodableSecureMessage(senderKeyPair);
			final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(Collections.singletonList(pair));
			final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
			Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, this.getTransferType())).thenReturn(expectedList);

			// Act:
			final SerializableList<TransactionMetaDataPair> resultList = this.executeLocal(context.controller, pagePrivateKeyPair);

			// Assert:
			Assert.assertThat(resultList.get(0).getTransaction(), IsSame.sameInstance(pair.getTransaction()));
		}

		@Test
		public void localAccountTransfersDelegateToAccountTransfers() {
			// Arrange:
			final KeyPair senderKeyPair = new KeyPair();
			final Address address = Address.fromPublicKey(senderKeyPair.getPublicKey());
			final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
			final TestContext context = new TestContext(accountIoAdapter);
			final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
			Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, this.getTransferType())).thenReturn(new SerializableList<>(1));

			// Act:
			this.executeLocal(context.controller, pagePrivateKeyPair);

			// Assert:
			final int callPattern = this.getLocalCallPattern();
			Mockito.verify(context.controller, Mockito.times(callPattern & 0x01)).accountTransfersAll(Mockito.any());
			Mockito.verify(context.controller, Mockito.times((callPattern & 0x02) >> 1)).accountTransfersIncoming(Mockito.any());
			Mockito.verify(context.controller, Mockito.times((callPattern & 0x04) >> 2)).accountTransfersOutgoing(Mockito.any());
		}

		//endregion
	}

	//region concrete classes

	public static class IncomingTransfersTest extends SingleDirectionTransfersTest {

		@Override
		protected ReadOnlyTransferDao.TransferType getTransferType() {
			return ReadOnlyTransferDao.TransferType.INCOMING;
		}

		@Override
		protected int getLocalCallPattern() {
			return 0x02;
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> execute(
				final AccountTransfersController controller,
				final AccountTransactionsPageBuilder builder) {
			return controller.accountTransfersIncoming(builder);
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> executeLocal(
				final AccountTransfersController controller,
				final AccountPrivateKeyTransactionsPage page) {
			return controller.localAccountTransfersIncoming(page);
		}
	}

	public static class OutgoingTransfersTest extends SingleDirectionTransfersTest {

		@Override
		protected ReadOnlyTransferDao.TransferType getTransferType() {
			return ReadOnlyTransferDao.TransferType.OUTGOING;
		}

		@Override
		protected int getLocalCallPattern() {
			return 0x04;
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> execute(
				final AccountTransfersController controller,
				final AccountTransactionsPageBuilder builder) {
			return controller.accountTransfersOutgoing(builder);
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> executeLocal(
				final AccountTransfersController controller,
				final AccountPrivateKeyTransactionsPage page) {
			return controller.localAccountTransfersOutgoing(page);
		}
	}

	public static class AllTransfersTest extends SingleDirectionTransfersTest {

		@Override
		protected ReadOnlyTransferDao.TransferType getTransferType() {
			return ReadOnlyTransferDao.TransferType.ALL;
		}

		@Override
		protected int getLocalCallPattern() {
			return 0x01;
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> execute(
				final AccountTransfersController controller,
				final AccountTransactionsPageBuilder builder) {
			return controller.accountTransfersAll(builder);
		}

		@Override
		protected SerializableList<TransactionMetaDataPair> executeLocal(
				final AccountTransfersController controller,
				final AccountPrivateKeyTransactionsPage page) {
			return controller.localAccountTransfersAll(page);
		}
	}

	//endregion

	private static TransactionMetaDataPair createPairWithDecodableSecureMessage(
			final KeyPair senderKeyPair,
			final KeyPair recipientKeyPair,
			final String message) {
		final Account sender = new Account(senderKeyPair);
		final Account recipient = new Account(recipientKeyPair);
		final SecureMessage secureMessage = SecureMessage.fromDecodedPayload(
				sender,
				recipient,
				message.getBytes());
		return createPairWithSecureMessage(sender, recipient, secureMessage);
	}

	private static TransactionMetaDataPair createPairWithUndecodableSecureMessage(final KeyPair senderKeyPair) {
		final Account sender = new Account(senderKeyPair);
		final Account recipient = new Account(Utils.generateRandomAddress());
		final SecureMessage secureMessage = SecureMessage.fromEncodedPayload(
				sender,
				recipient,
				Utils.generateRandomBytes());
		return createPairWithSecureMessage(sender, recipient, secureMessage);
	}

	private static TransactionMetaDataPair createPairWithSecureMessage(
			final Account sender,
			final Account recipient,
			final SecureMessage secureMessage) {
		final TransferTransaction transaction = new TransferTransaction(
				new TimeInstant(10),
				sender,
				recipient,
				Amount.fromNem(1),
				secureMessage);
		return new TransactionMetaDataPair(transaction, new TransactionMetaData(BlockHeight.ONE, 1L, Hash.ZERO));
	}

	private static class TestContext {
		private final AccountTransfersController controller;
		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		private final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);

		public TestContext() {
			this(Mockito.mock(AccountIoAdapter.class));
		}

		public TestContext(final AccountIoAdapter accountIoAdapter) {
			this.controller = Mockito.spy(new AccountTransfersController(
					accountIoAdapter,
					this.transactionHashCache,
					this.nisConfiguration));
			Mockito.when(this.nisConfiguration.getOptionalFeatures()).thenReturn(new NodeFeature[] {});
		}

		public void enableHashLookup() {
			Mockito.when(this.nisConfiguration.getOptionalFeatures()).thenReturn(new NodeFeature[] { NodeFeature.TRANSACTION_HASH_LOOKUP });
		}
	}
}
