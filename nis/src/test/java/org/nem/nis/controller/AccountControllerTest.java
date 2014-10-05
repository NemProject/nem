package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.AccountImportance;
import org.nem.nis.service.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AccountControllerTest {

	//region accountUnlock

	@Test
	public void unlockCopiesRelevantAccountData() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account accountFromIo = Mockito.mock(Account.class);
		final Account copyAccount = Mockito.mock(Account.class);
		Mockito.when(accountFromIo.shallowCopyWithKeyPair(keyPair)).thenReturn(copyAccount);

		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		Mockito.when(accountIoAdapter.findByAddress(Address.fromPublicKey(keyPair.getPublicKey()))).thenReturn(accountFromIo);

		final TestContext context = new TestContext(accountIoAdapter);
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);

		// Act:
		context.controller.accountUnlock(keyPair.getPrivateKey());

		// Assert:
		Mockito.verify(accountFromIo, Mockito.times(1)).shallowCopyWithKeyPair(Mockito.any());
	}

	@Test
	public void unlockDelegatesToUnlockedAccounts() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);

		// Act:
		context.controller.accountUnlock(account.getKeyPair().getPrivateKey());

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).addUnlockedAccount(Mockito.any());
	}

	@Test
	public void unlockFailureRaisesException() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final TestContext context = createContextAroundAccount(account, Amount.ZERO);
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.FAILURE_UNKNOWN_ACCOUNT);

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.accountUnlock(account.getKeyPair().getPrivateKey()),
				IllegalArgumentException.class);
	}

	//endregion

	//region accountLock

	@Test
	public void lockDelegatesToUnlockedAccounts() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final TestContext context = createContextAroundAccount(account, Amount.fromNem(1000));
		final PrivateKey privateKey = new PrivateKey(account.getKeyPair().getPrivateKey().getRaw());
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);

		// Act:
		context.controller.accountUnlock(account.getKeyPair().getPrivateKey());
		context.controller.accountLock(privateKey);

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).removeUnlockedAccount(Mockito.any());
	}

	private static TestContext createContextAroundAccount(final Account account, final Amount amount) {
		account.incrementBalance(amount);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		Mockito.when(accountIoAdapter.findByAddress(account.getAddress())).thenReturn(account);
		return new TestContext(accountIoAdapter);
	}

	//endregion

	//region accountGet

	@Test
	public void accountGetDelegatesToAccountInfoFactory() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());
		final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);

		final TestContext context = new TestContext(Mockito.mock(AccountIoAdapter.class));
        Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1L);
		Mockito.when(context.accountInfoFactory.createInfo(address, BlockHeight.ONE)).thenReturn(accountInfo);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(builder);

		// Assert:
		Mockito.verify(context.accountInfoFactory, Mockito.times(1)).createInfo(address, BlockHeight.ONE);
		Assert.assertThat(metaDataPair.getAccount(), IsSame.sameInstance(accountInfo));
	}

	@Test
	public void accountGetDelegatesToUnlockedAccounts() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final TestContext context = new TestContext(Mockito.mock(AccountIoAdapter.class));
        Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1L);
		Mockito.when(context.accountInfoFactory.createInfo(address, BlockHeight.ONE)).thenReturn(Mockito.mock(AccountInfo.class));
		Mockito.when(context.unlockedAccounts.isAccountUnlocked(address)).thenReturn(true);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(builder);

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(address);
		Assert.assertThat(metaDataPair.getMetaData().getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
	}

	//endregion

    //region accountStatus
    @Test
    public void accountStatusDelegatesToUnlockedAccounts()
    {
        assertAccountStatusDelegatesToUnlockedAccounts(true, AccountStatus.UNLOCKED);
        assertAccountStatusDelegatesToUnlockedAccounts(false, AccountStatus.LOCKED);
    }

    private void assertAccountStatusDelegatesToUnlockedAccounts(final boolean returned, final AccountStatus expectedStatus) {
        // Arrange:
        final Address address = Utils.generateRandomAddressWithPublicKey();
        final AccountIdBuilder builder = new AccountIdBuilder();
        builder.setAddress(address.getEncoded());

        final TestContext context = new TestContext(Mockito.mock(AccountIoAdapter.class));
        Mockito.when(context.unlockedAccounts.isAccountUnlocked(address)).thenReturn(returned);

        // Act:
        final AccountMetaData accountMetaData = context.controller.accountStatus(builder);

        // Assert:
        Mockito.verify(context.unlockedAccounts, Mockito.times(1)).isAccountUnlocked(address);
        Assert.assertThat(accountMetaData.getStatus(), IsEqual.equalTo(expectedStatus));
    }
    //endregion

	//region accountTransfers[All|Incoming|Outgoing]

	@Test
	public void accountTransfersAllDelegatesToIoAdapter() {
		this.accountTransfersMethodsDelegatesToIo(ReadOnlyTransferDao.TransferType.ALL, AccountController::accountTransfersAll);
	}

	@Test
	public void accountTransfersIncomingDelegatesToIoAdapter() {
		this.accountTransfersMethodsDelegatesToIo(ReadOnlyTransferDao.TransferType.INCOMING, AccountController::accountTransfersIncoming);
	}

	@Test
	public void accountTransfersOutgoingDelegatesToIoAdapter() {
		this.accountTransfersMethodsDelegatesToIo(ReadOnlyTransferDao.TransferType.OUTGOING, AccountController::accountTransfersOutgoing);
	}

	private void accountTransfersMethodsDelegatesToIo(
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

		final Hash hash = Hash.fromHexString("ffeeddccbbaa99887766554433221100");
		Mockito.when(accountIoAdapter.getAccountTransfersWithHash(address, hash, transferType)).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList = controllerMethod.apply(context.controller, pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfersWithHash(address, hash, transferType);
	}

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
		final List<PoiAccountState> accountStates = Arrays.asList(
				createAccountState("alpha", 12, 45),
				createAccountState("gamma", 0, 0),
				createAccountState("sigma", 4, 88));

		final TestContext context = new TestContext();
		Mockito.when(context.poiFacade.spliterator()).thenReturn(accountStates.spliterator());

		// Act:
		final SerializableList<AccountImportanceViewModel> viewModels = context.controller.getImportances();

		// Assert:
		final List<AccountImportanceViewModel> expectedViewModels = Arrays.asList(
				createAccountImportanceViewModel("alpha", 12, 45),
				createAccountImportanceViewModel("gamma", 0, 0),
				createAccountImportanceViewModel("sigma", 4, 88));
		Assert.assertThat(viewModels.asCollection(), IsEquivalent.equivalentTo(expectedViewModels));
	}

	private static PoiAccountState createAccountState(
			final String encodedAddress,
			final int blockHeight,
			final int importance) {
		final PoiAccountState state = new PoiAccountState(Address.fromEncoded(encodedAddress));
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
		private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final AccountController controller;
		private final AccountInfoFactory accountInfoFactory = Mockito.mock(AccountInfoFactory.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
        private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);

		public TestContext() {
			this(Mockito.mock(AccountIoAdapter.class));
		}

		public TestContext(final AccountIoAdapter accountIoAdapter) {
			this.controller = new AccountController(
					this.unconfirmedTransactions,
					this.unlockedAccounts,
					accountIoAdapter,
                    this.blockChainLastBlockLayer,
					this.accountInfoFactory,
					this.poiFacade);
		}
	}
}
