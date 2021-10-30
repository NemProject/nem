package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountImportanceViewModel;
import org.nem.nis.harvesting.*;
import org.nem.nis.service.AccountIoAdapter;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AccountControllerTest {

	// region accountUnlock

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
		ExceptionAssert.assertThrows(v -> context.controller.accountUnlock(keyPair.getPrivateKey()), IllegalArgumentException.class);
	}

	// endregion

	// region accountLock

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

	// endregion

	// region isAccountUnlocked

	@Test
	public void accountIsUnlockedReturnsOkWhenAccountFromAddressIsUnlocked() {
		// Arrange:
		assertAccountIsUnlockedReturnsOkWhenAccountIsUnlocked(AccountIsUnlockedTestContext::checkIsUnlockedWithAddress);
	}

	@Test
	public void accountIsUnlockedReturnsOkWhenAccountFromPrivateKeyIsUnlocked() {
		// Assert:
		assertAccountIsUnlockedReturnsOkWhenAccountIsUnlocked(AccountIsUnlockedTestContext::checkIsUnlockedWithPrivateKey);
	}

	private static void assertAccountIsUnlockedReturnsOkWhenAccountIsUnlocked(
			final Function<AccountIsUnlockedTestContext, String> isAccountUnlocked) {
		// Arrange:
		final AccountIsUnlockedTestContext context = new AccountIsUnlockedTestContext();
		context.setIsUnlocked(true);

		// Act:
		final String result = isAccountUnlocked.apply(context);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo("ok"));
	}

	@Test
	public void accountIsUnlockedReturnsNopeWhenAccountFromAddressIsLocked() {
		// Assert:
		assertAccountIsUnlockedReturnsNopeWhenAccountIsLocked(AccountIsUnlockedTestContext::checkIsUnlockedWithAddress);
	}

	@Test
	public void accountIsUnlockedReturnsNopeWhenAccountFromPrivateKeyIsLocked() {
		// Assert:
		assertAccountIsUnlockedReturnsNopeWhenAccountIsLocked(AccountIsUnlockedTestContext::checkIsUnlockedWithPrivateKey);
	}

	private static void assertAccountIsUnlockedReturnsNopeWhenAccountIsLocked(
			final Function<AccountIsUnlockedTestContext, String> isAccountUnlocked) {
		// Arrange:
		final AccountIsUnlockedTestContext context = new AccountIsUnlockedTestContext();
		context.setIsUnlocked(false);

		// Act:
		final String result = isAccountUnlocked.apply(context);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo("nope"));
	}

	private static class AccountIsUnlockedTestContext {
		private final KeyPair keyPair = new KeyPair();
		private final Account account = new Account(this.keyPair);
		private final TestContext context = createContextAroundAccount(this.account, Amount.fromNem(1000));

		public void setIsUnlocked(final boolean result) {
			Mockito.when(this.context.unlockedAccounts.isAccountUnlocked(this.account)).thenReturn(result);
		}

		public String checkIsUnlockedWithAddress() {
			return this.context.controller.isAccountUnlocked(this.account.getAddress());
		}

		public String checkIsUnlockedWithPrivateKey() {
			return this.context.controller.isAccountUnlocked(this.keyPair.getPrivateKey());
		}
	}

	// endregion

	// region unlockedInfo

	@Test
	public void unlockedInfoReturnsUnlockedAccountInformation() {
		// Arrange:
		final TestContext context = createContextAroundAccount(Utils.generateRandomAccount(), Amount.fromNem(1000));
		Mockito.when(context.unlockedAccounts.size()).thenReturn(3);
		Mockito.when(context.unlockedAccounts.maxSize()).thenReturn(8);

		// Act:
		final SerializableEntity entity = context.controller.unlockedInfo();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(entity);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(jsonObject.get("num-unlocked"), IsEqual.equalTo(3));
		MatcherAssert.assertThat(jsonObject.get("max-unlocked"), IsEqual.equalTo(8));
	}

	// endregion

	// region transactionsUnconfirmed

	@Test
	public void transactionsUnconfirmedDelegatesToUnconfirmedTransactions() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final List<Transaction> originalTransactions = Arrays.asList(new MockTransaction(7, new TimeInstant(1)),
				new MockTransaction(11, new TimeInstant(2)), new MockTransaction(5, new TimeInstant(3)));
		final TestContext context = new TestContext();

		Mockito.when(context.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, 25)).thenReturn(originalTransactions);

		// Act:
		final SerializableList<UnconfirmedTransactionMetaDataPair> pairs = context.controller.transactionsUnconfirmed(builder);

		// Assert:
		MatcherAssert.assertThat(
				pairs.asCollection().stream().map(p -> ((MockTransaction) (p.getEntity())).getCustomField()).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(7, 11, 5)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).getMostRecentTransactionsForAccount(address, 25);
	}

	@Test
	public void transactionsUnconfirmedSuppliesHashesOfInnerTransactions() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountIdBuilder builder = new AccountIdBuilder();
		builder.setAddress(address.getEncoded());

		final List<Transaction> originalTransactions = Arrays.asList(RandomTransactionFactory.createTransfer(),
				RandomTransactionFactory.createMultisigTransfer(), RandomTransactionFactory.createTransfer(),
				RandomTransactionFactory.createMultisigTransfer(), RandomTransactionFactory.createMultisigTransfer());
		final List<Hash> expectedHashes = originalTransactions.stream()
				.map(t -> TransactionTypes.MULTISIG == t.getType() ? ((MultisigTransaction) t).getOtherTransactionHash() : null)
				.collect(Collectors.toList());
		final TestContext context = new TestContext();

		Mockito.when(context.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, 25)).thenReturn(originalTransactions);

		// Act:
		final SerializableList<UnconfirmedTransactionMetaDataPair> pairs = context.controller.transactionsUnconfirmed(builder);

		// Assert:
		final Collection<Hash> innerHashes = pairs.asCollection().stream().map(p -> p.getMetaData().getInnerTransactionHash())
				.collect(Collectors.toList());
		MatcherAssert.assertThat(innerHashes.stream().filter(h -> null != h).count(), IsEqual.equalTo(3L));
		MatcherAssert.assertThat(innerHashes, IsEqual.equalTo(expectedHashes));
	}

	// endregion

	// region accountHarvests

	@Test
	public void accountHarvestsDelegatesToAccountIo() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<HarvestInfo> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountIdBuilder idBuilder = new AccountIdBuilder();
		idBuilder.setAddress(address.getEncoded());
		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId("12345678");
		pageBuilder.setPageSize("12");

		Mockito.when(accountIoAdapter.getAccountHarvests(address, 12345678L, 12)).thenReturn(expectedList);

		// Act:
		final SerializableList<HarvestInfo> resultList = context.controller.accountHarvests(idBuilder, pageBuilder);

		// Assert:
		MatcherAssert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountHarvests(address, 12345678L, 12);
	}

	// endregion

	// region getImportances

	@Test
	public void getImportancesReturnsImportanceInformationForAllAccounts() {
		// Arrange:
		final List<AccountState> accountStates = Arrays.asList(createAccountState("alpha", 12, 45), createAccountState("gamma", 0, 0),
				createAccountState("sigma", 4, 88));

		final TestContext context = new TestContext();
		Mockito.when(context.accountStateCache.contents()).thenReturn(new CacheContents<>(accountStates));

		// Act:
		final SerializableList<AccountImportanceViewModel> viewModels = context.controller.getImportances();

		// Assert:
		final List<AccountImportanceViewModel> expectedViewModels = Arrays.asList(createAccountImportanceViewModel("alpha", 12, 45),
				createAccountImportanceViewModel("gamma", 0, 0), createAccountImportanceViewModel("sigma", 4, 88));
		MatcherAssert.assertThat(viewModels.asCollection(), IsEquivalent.equivalentTo(expectedViewModels));
	}

	private static AccountState createAccountState(final String encodedAddress, final int blockHeight, final int importance) {
		final AccountState state = new AccountState(Address.fromEncoded(encodedAddress));
		if (blockHeight > 0) {
			state.getImportanceInfo().setImportance(new BlockHeight(blockHeight), importance);
		}

		return state;
	}

	private static AccountImportanceViewModel createAccountImportanceViewModel(final String encodedAddress, final int blockHeight,
			final int importance) {
		final AccountImportance ai = new AccountImportance();
		if (blockHeight > 0) {
			ai.setImportance(new BlockHeight(blockHeight), importance);
		}

		return new AccountImportanceViewModel(Address.fromEncoded(encodedAddress), ai);
	}

	// endregion

	// region generateAccount

	@Test
	public void generateAccountReturnsKeyPairViewModelWithDefaultNetworkVersion() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final KeyPairViewModel viewModel = context.controller.generateAccount();

		// Assert:
		MatcherAssert.assertThat(viewModel.getKeyPair(), IsNull.notNullValue());
		MatcherAssert.assertThat(viewModel.getNetworkVersion(), IsEqual.equalTo(NetworkInfos.getDefault().getVersion()));
	}

	// endregion

	private static class TestContext {
		private final AccountController controller;
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);

		public TestContext() {
			this(Mockito.mock(AccountIoAdapter.class));
		}

		public TestContext(final AccountIoAdapter accountIoAdapter) {
			this.controller = new AccountController(this.unconfirmedTransactions, this.unlockedAccounts, accountIoAdapter,
					this.accountStateCache);
		}

		private void addAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
		}
	}
}
