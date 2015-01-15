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
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.AccountIoAdapter;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AccountControllerTest {

	//region accountUnlock

	@Test
	public void unlockDelegatesToUnlockedAccounts() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);

		// Act:
		context.controller.accountUnlock(keyPair.getPrivateKey());

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).addUnlockedAccount(Mockito.any());
	}

	@Test
	public void unlockFailureRaisesException() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final TestContext context = createContextAroundAccount(account, Amount.ZERO);
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.FAILURE_UNKNOWN_ACCOUNT);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.accountUnlock(keyPair.getPrivateKey()),
				IllegalArgumentException.class);
	}

	//endregion

	//region accountLock

	@Test
	public void lockDelegatesToUnlockedAccounts() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);

		// Act:
		context.controller.accountUnlock(keyPair.getPrivateKey());
		context.controller.accountLock(keyPair.getPrivateKey());

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).removeUnlockedAccount(Mockito.any());
	}

	private static TestContext createContextAroundAccount(final Account account, final Amount amount) {
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		Mockito.when(accountIoAdapter.findByAddress(account.getAddress())).thenReturn(account);
		final TestContext context = new TestContext(accountIoAdapter);
		context.addAccount(account, amount);
		return context;
	}

	//endregion

	//region accountIsUnlocked

	@Test
	public void accountIsUnlockedReturnsOkWhenAccountIsUnlocked() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.isAccountUnlocked(account)).thenReturn(true);

		// Act:
		final String result = context.controller.accountIsUnlocked(account.getAddress());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo("ok"));
	}

	@Test
	public void accountIsUnlockedReturnsNopeWhenAccountIsLocked() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.isAccountUnlocked(account)).thenReturn(false);

		// Act:
		final String result = context.controller.accountIsUnlocked(account.getAddress());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo("nope"));
	}

	//endregion

	//region accountTransfers[All|Incoming|Outgoing]

	//region accountTransfersMethodsDelegatesToIoWhenIdIsProvided

	@Test
	public void accountTransfersAllDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.ALL,
				AccountController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenIdIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsDelegatesToIoWhenIdIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenHashIsProvided() {
		this.accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenHashIsProvided() {
		this.accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::accountTransfersOutgoing);
	}

	public void accountTransfersMethodsUsesIoAdapterWhenHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenNeitherIdNorHashIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenNeitherIdNorHashIsProvided() {
		this.accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsDelegatesToIoWhenNeitherIdNorHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapterWhenUnknownHashIsProvided() {
		this.accountTransfersMethodsFailsWhenUnknownHashIsProvided(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapterWhenUnknownHashIsProvided() {
		this.accountTransfersMethodsFailsWhenUnknownHashIsProvided(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsFailsWhenUnknownHashIsProvided(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountTransactionsPageBuilder, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::localAccountTransfersAll);
	}

	@Test
	public void localAccountTransfersIncomingReturnsTransactionsWithDecodedMessagesIfPossible() {
		this.localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::localAccountTransfersIncoming);
	}

	@Test
	public void localAccountTransfersOutgoingReturnsTransactionsWithDecodedMessagesIfPossible() {
		this.localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::localAccountTransfersOutgoing);
	}

	private void localAccountTransfersReturnsTransactionsWithDecodedMessagesIfPossible(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::localAccountTransfersAll);
	}

	@Test
	public void localAccountTransfersIncomingAllLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
		this.localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::localAccountTransfersIncoming);
	}

	@Test
	public void localAccountTransfersOutgoingAllLeavesTransactionsUntouchedIfDecodingIsNotPossible() {
		this.localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::localAccountTransfersOutgoing);
	}

	private void localAccountTransfersLeavesTransactionsUntouchedIfDecodingIsNotPossible(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod) {
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
				AccountController::localAccountTransfersAll,
				0x01);
	}

	@Test
	public void localAccountTransfersIncomingMethodsDelegateToAccountTransfersIncomingMethods() {
		this.localAccountTransfersMethodsDelegateToAccountTransfersMethods(
				ReadOnlyTransferDao.TransferType.INCOMING,
				AccountController::localAccountTransfersIncoming,
				0x02);
	}

	@Test
	public void localAccountTransfersOutgoingMethodsDelegateToAccountTransfersOutgoingMethods() {
		this.localAccountTransfersMethodsDelegateToAccountTransfersMethods(
				ReadOnlyTransferDao.TransferType.OUTGOING,
				AccountController::localAccountTransfersOutgoing,
				0x04);
	}

	private void localAccountTransfersMethodsDelegateToAccountTransfersMethods(
			final ReadOnlyTransferDao.TransferType transferType,
			final BiFunction<AccountController, AccountPrivateKeyTransactionsPage, SerializableList<TransactionMetaDataPair>> controllerMethod,
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

	//region transactionsUnconfirmed

	@Test
	public void transactionsUnconfirmedDelegatesToUnconfirmedTransactions() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final List<Transaction> originalTransactions = Arrays.asList(
				new MockTransaction(7, new TimeInstant(1)),
				new MockTransaction(11, new TimeInstant(2)),
				new MockTransaction(5, new TimeInstant(3)));
		final TestContext context = new TestContext();

		Mockito.when(context.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, 25))
				.thenReturn(originalTransactions);

		// Act:
		final SerializableList<Transaction> transactions = context.controller.transactionsUnconfirmed(builder);

		// Assert:
		Assert.assertThat(
				transactions.asCollection().stream().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(7, 11, 5)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).getMostRecentTransactionsForAccount(address, 25);
	}

	//endregion

	//region accountHarvests

	@Test
	public void accountHarvestsDelegatesToAccountIo() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<HarvestInfo> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(address.getEncoded());
		pageBuilder.setHash("ffeeddccbbaa99887766554433221100");

		final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");
		Mockito.when(accountIoAdapter.getAccountHarvests(address, hash)).thenReturn(expectedList);

		// Act:
		final SerializableList<HarvestInfo> resultList = context.controller.accountHarvests(pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountHarvests(address, hash);
	}

	//endregion

	//region getImportances

	@Test
	public void getImportancesReturnsImportanceInformationForAllAccounts() {
		// Arrange:
		final List<AccountState> accountStates = Arrays.asList(
				createAccountState("alpha", 12, 45),
				createAccountState("gamma", 0, 0),
				createAccountState("sigma", 4, 88));

		final TestContext context = new TestContext();
		Mockito.when(context.accountStateCache.contents()).thenReturn(new CacheContents<>(accountStates));

		// Act:
		final SerializableList<AccountImportanceViewModel> viewModels = context.controller.getImportances();

		// Assert:
		final List<AccountImportanceViewModel> expectedViewModels = Arrays.asList(
				createAccountImportanceViewModel("alpha", 12, 45),
				createAccountImportanceViewModel("gamma", 0, 0),
				createAccountImportanceViewModel("sigma", 4, 88));
		Assert.assertThat(viewModels.asCollection(), IsEquivalent.equivalentTo(expectedViewModels));
	}

	private static AccountState createAccountState(
			final String encodedAddress,
			final int blockHeight,
			final int importance) {
		final AccountState state = new AccountState(Address.fromEncoded(encodedAddress));
		if (blockHeight > 0) {
			state.getImportanceInfo().setImportance(new BlockHeight(blockHeight), importance);
		}

		return state;
	}

	private static AccountImportanceViewModel createAccountImportanceViewModel(
			final String encodedAddress,
			final int blockHeight,
			final int importance) {
		final AccountImportance ai = new AccountImportance();
		if (blockHeight > 0) {
			ai.setImportance(new BlockHeight(blockHeight), importance);
		}

		return new AccountImportanceViewModel(Address.fromEncoded(encodedAddress), ai);
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
		private final AccountController controller;
		private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);

		public TestContext() {
			this(Mockito.mock(AccountIoAdapter.class));
		}

		public TestContext(final AccountIoAdapter accountIoAdapter) {
			this.controller = Mockito.spy(new AccountController(
					this.unconfirmedTransactions,
					this.unlockedAccounts,
					accountIoAdapter,
					this.accountStateCache,
					this.transactionHashCache));
		}

		private Account addAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}
	}
}
