package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.AccountIoAdapter;

import java.util.*;
import java.util.function.*;

public class AccountTransfersControllerTest {

	//region accountTransfers[All|Incoming|Outgoing]

	//region accountTransfersMethodsDelegatesToIoWhenIdIsProvided

	@Test
	public void accountTransfersAllDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(address.getEncoded());
		pageBuilder.setHash("ffeeddccbbaa99887766554433221100");
		pageBuilder.setId("1");

		Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, 1L, transferType)).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingId(address, 1L, transferType);
	}

	//endregion

	//region accountTransfersMethodsUsesIoAdapterWhenHashIsProvided

	@Test
	public void accountTransfersAllDelegatesToIoAdapterWhenHashIsProvided() {
		this.accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenHashIsProvided() {
		this.accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenHashIsProvided() {
		this.accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::accountTransfersOutgoing);
	}

	public void accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
		Mockito.when(accountIoAdapter.getAccountTransfersUsingHash(address, hash, new BlockHeight(12), transferType))
				.thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingHash(address, hash, new BlockHeight(12), transferType);
		Mockito.verify(context.transactionHashCache, Mockito.times(1)).get(hash);
	}

	//endregion

	//region accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided

	@Test
	public void accountTransfersAllDelegatesToIoAdapterWhenNeitherIdNorHashIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenNeitherIdNorHashIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenNeitherIdNorHashIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(address.getEncoded());

		Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, transferType)).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersUsingId(address, null, transferType);
	}

	//endregion

	//region accountTransfersFailsWhenUnknownHashIsProvided

	@Test
	public void accountTransfersAllFailsWhenUnknownHashIsProvided() {
		this.accountTransfersMethodsFailsWhenUnknownHashIsProvided(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenUnknownHashIsProvided() {
		this.accountTransfersMethodsFailsWhenUnknownHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenUnknownHashIsProvided() {
		this.accountTransfersMethodsFailsWhenUnknownHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsFailsWhenUnknownHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");

		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(address.getEncoded());
		pageBuilder.setHash(hash.toString());

		Mockito.when(context.transactionHashCache.get(hash)).thenReturn(null);

		// Act:
		ExceptionAssert.assertThrows(
				v -> controllerMethod.apply(context.controller, pageBuilder),
				IllegalArgumentException.class);
	}

	//endregion

	//endregion

	//region localAccountTransfersMethodsDecodeMessages

	@Test
	public void localAccountTransfersAllReturnsTransactionsWithDecodedMessagesIfPossible() {
		this.localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::localAccountTransfersAll);
	}

	@Test
	public void localAccountTransfersIncomingReturnsTransactionsWithDecodedMessagesIfPossible() {
		this.localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::localAccountTransfersIncoming);
	}

	@Test
	public void localAccountTransfersOutgoingReturnsTransactionsWithDecodedMessagesIfPossible() {
		this.localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::localAccountTransfersOutgoing);
	}

	private void localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
		final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(Arrays.asList(pair));
		final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
		Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, transferType)).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pagePrivateKeyPair);

		// Assert:
		final TransferTransaction tx = (TransferTransaction)resultList.get(0).getTransaction();
		Assert.assertThat(tx, IsNot.not(IsSame.sameInstance(pair.getTransaction())));
		Assert.assertThat(tx.getMessage(), IsInstanceOf.instanceOf(PlainMessage.class));
		Assert.assertThat(new String(tx.getMessage().getDecodedPayload()), IsEqual.equalTo("This is a secret message"));
	}

	//endregion

	//region localAccountTransfersMethodsLeavesTransactionsUntouchedIfDecodingIsNotPossible

	@Test
	public void localAccountTransfersAllLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
		this.localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::localAccountTransfersAll);
	}

	@Test
	public void localAccountTransfersIncomingAllLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
		this.localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::localAccountTransfersIncoming);
	}

	@Test
	public void localAccountTransfersOutgoingAllLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
		this.localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::localAccountTransfersOutgoing);
	}

	private void localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod) {
		// Arrange:
		final KeyPair senderKeyPair = new KeyPair();
		final Address address = Address.fromPublicKey(senderKeyPair.getPublicKey());
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);
		final TransactionMetaDataPair pair = createPairWithUndecodableSecureMessage(senderKeyPair);
		final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(Arrays.asList(pair));
		final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
		Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, transferType)).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pagePrivateKeyPair);

		// Assert:
		Assert.assertThat(resultList.get(0).getTransaction(), IsSame.sameInstance(pair.getTransaction()));
	}

	//endregion

	//region localAccountTransfersMethodsDelegateToAccountTransfersMethods

	@Test
	public void localAccountTransfersAllMethodsDelegateToAccountTransfersAllMethods() {
		this.localAccountTransfersMethodsDelegateToAccountTransfersMethods(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountTransfersController::localAccountTransfersAll,
				0x01);
	}

	@Test
	public void localAccountTransfersIncomingMethodsDelegateToAccountTransfersIncomingMethods() {
		this.localAccountTransfersMethodsDelegateToAccountTransfersMethods(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountTransfersController::localAccountTransfersIncoming,
				0x02);
	}

	@Test
	public void localAccountTransfersOutgoingMethodsDelegateToAccountTransfersOutgoingMethods() {
		this.localAccountTransfersMethodsDelegateToAccountTransfersMethods(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountTransfersController::localAccountTransfersOutgoing,
				0x04);
	}

	private void localAccountTransfersMethodsDelegateToAccountTransfersMethods(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountTransfersController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod,
			final int callPattern) {
		// Arrange:
		final KeyPair senderKeyPair = new KeyPair();
		final Address address = Address.fromPublicKey(senderKeyPair.getPublicKey());
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);
		final AccountPrivateKeyTransactionsPage pagePrivateKeyPair = new AccountPrivateKeyTransactionsPage(senderKeyPair.getPrivateKey());
		Mockito.when(accountIoAdapter.getAccountTransfersUsingId(address, null, transferType)).thenReturn(new SerializableList<>(1));

		// Act:
		controllerMethod.apply(context.controller, pagePrivateKeyPair);

		// Assert:
		Mockito.verify(context.controller, Mockito.times(callPattern & 0x01)).accountTransfersAll(Mockito.any());
		Mockito.verify(context.controller, Mockito.times((callPattern & 0x02) >> 1)).accountTransfersIncoming(Mockito.any());
		Mockito.verify(context.controller, Mockito.times((callPattern & 0x04) >> 2)).accountTransfersOutgoing(Mockito.any());
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
		return new TransactionMetaDataPair(transaction, new TransactionMetaData(BlockHeight.ONE, 1L));
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
		}
	}
}
