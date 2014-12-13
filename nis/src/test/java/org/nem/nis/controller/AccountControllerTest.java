package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
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
import org.nem.nis.state.*;
import org.nem.nis.service.AccountIoAdapter;

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

	//region transactionsUnconfirmed

	@Test
	public void transactionsUnconfirmedDelegatesToUnconfirmedTransactions() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final UnconfirmedTransactions originalUnconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
		final List<Transaction> originalTransactions = Arrays.asList(
				new MockTransaction(7, new TimeInstant(1)),
				new MockTransaction(11, new TimeInstant(2)),
				new MockTransaction(5, new TimeInstant(3)));
		Mockito.when(originalUnconfirmedTransactions.getAll()).thenReturn(originalTransactions);
		final TestContext context = new TestContext();

		Mockito.when(context.unconfirmedTransactions.getTransactionsForAccount(address))
				.thenReturn(originalUnconfirmedTransactions);

		// Act:
		final SerializableList<Transaction> transactions = context.controller.transactionsUnconfirmed(builder);

		// Assert:
		Assert.assertThat(
				transactions.asCollection().stream().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(7, 11, 5)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).getTransactionsForAccount(address);
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
		Mockito.when(context.accountStateCache.spliterator()).thenReturn(accountStates.spliterator());

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
			this.controller = new AccountController(
					this.unconfirmedTransactions,
					this.unlockedAccounts,
					accountIoAdapter,
					this.accountStateCache,
					this.transactionHashCache);
		}

		private Account addAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}
	}
}
