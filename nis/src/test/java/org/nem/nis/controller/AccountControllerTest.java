package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountMetaDataPair;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.IsEquivalent;
import org.nem.core.test.Utils;
import org.nem.nis.Foraging;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.service.*;

import java.util.*;

public class AccountControllerTest {

	@Test
	public void unlockAddsAccountToForaging() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final TestContext context = new TestContext();

		// Act:
		context.controller.accountUnlock(account.getKeyPair().getPrivateKey());

		// Assert:
		Assert.assertThat(context.foraging.getUnlockedAccounts().size(), IsEqual.equalTo(1));
		Assert.assertThat(context.foraging.getUnlockedAccounts().get(0), IsEqual.equalTo(account));
		Assert.assertThat(context.foraging.getUnlockedAccounts().get(0), IsNot.not(IsSame.sameInstance(account)));
	}

	@Test
	public void lockRemovesAccountFromForaging() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final PrivateKey privateKey = new PrivateKey(account.getKeyPair().getPrivateKey().getRaw());
		final TestContext context = new TestContext();

		// Act:
		context.controller.accountUnlock(account.getKeyPair().getPrivateKey());
		context.controller.accountLock(privateKey);

		// Assert:
		Assert.assertThat(context.foraging.getUnlockedAccounts().size(), IsEqual.equalTo(0));
	}

	@Test
	public void accountGetDelegatesToAccountIoAdapter() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		Mockito.when(accountIoAdapter.findByAddress(account.getAddress())).thenReturn(account);
		final TestContext context = new TestContext(accountIoAdapter);

		// Act:
		final AccountMetaDataPair metaDataPair = context.controller.accountGet(account.getAddress().getEncoded());

		// Assert:
		Assert.assertThat(metaDataPair.getAccount(), IsSame.sameInstance(account));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountGetReturnsErrorForInvalidAccount() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.controller.accountGet("dummy");
	}

	@Test
	public void accountTransfersDelegatesToIoAdapter() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<TransactionMetaDataPair> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountPageBuilder pageBuilder = new AccountPageBuilder();
		pageBuilder.setAddress(address.getEncoded());
		pageBuilder.setTimestamp("12345");

		Mockito.when(accountIoAdapter.getAccountTransfers(address, "12345")).thenReturn(expectedList);

		// Act:
		final SerializableList<TransactionMetaDataPair> resultList =
				context.controller.accountTransfers(pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountTransfers(address, "12345");
	}

	@Test
	public void accountBlocksDelegatesToIoAdapter() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableList<Block> expectedList = new SerializableList<>(10);
		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		final TestContext context = new TestContext(accountIoAdapter);

		final AccountPageBuilder pageBuilder = new AccountPageBuilder();
		pageBuilder.setAddress(address.getEncoded());
		pageBuilder.setTimestamp("12345");

		Mockito.when(accountIoAdapter.getAccountBlocks(address, "12345")).thenReturn(expectedList);

		// Act:
		final SerializableList<Block> resultList = context.controller.accountBlocks(pageBuilder);

		// Assert:
		Assert.assertThat(resultList, IsSame.sameInstance(expectedList));
		Mockito.verify(accountIoAdapter, Mockito.times(1)).getAccountBlocks(address, "12345");
	}

	@Test
	public void getImportancesReturnsImportanceInformationForAllAccounts() {
		// Arrange:
		final List<Account> accounts = Arrays.asList(
				createAccount("alpha", 12, 45),
				createAccount("gamma", 0, 0),
				createAccount("sigma", 4, 88));

		final AccountIoAdapter accountIoAdapter = Mockito.mock(AccountIoAdapter.class);
		Mockito.when(accountIoAdapter.spliterator()).thenReturn(accounts.spliterator());

		final TestContext context = new TestContext(accountIoAdapter);

		// Act:
		final SerializableList<AccountImportanceViewModel> viewModels = context.controller.getImportances();

		// Assert:
		final List<AccountImportanceViewModel> expectedViewModels = Arrays.asList(
				createAccountImportanceViewModel("alpha", 12, 45),
				createAccountImportanceViewModel("gamma", 0, 0),
				createAccountImportanceViewModel("sigma", 4, 88));
		Assert.assertThat(viewModels.asCollection(), IsEquivalent.equivalentTo(expectedViewModels));
	}

	private static Account createAccount(
			final String encodedAddress,
			final int blockHeight,
			final int importance) {
		final Account account = new Account(Address.fromEncoded(encodedAddress));
		if (blockHeight > 0)
			account.getImportanceInfo().setImportance(new BlockHeight(blockHeight), importance);

		return account;
	}

	private static AccountImportanceViewModel createAccountImportanceViewModel(
			final String encodedAddress,
			final int blockHeight,
			final int importance) {
		final AccountImportance ai = new AccountImportance();
		if (blockHeight > 0)
			ai.setImportance(new BlockHeight(blockHeight), importance);

		return new AccountImportanceViewModel(Address.fromEncoded(encodedAddress), ai);
	}

	private static class TestContext {
		private final MockForaging foraging = new MockForaging();
		private final AccountController controller;

		public TestContext() {
			this(Mockito.mock(AccountIoAdapter.class));
		}

		public TestContext(final AccountIoAdapter accountIoAdapter) {
			this.controller = new AccountController(this.foraging, accountIoAdapter);
		}
	}

	private static class MockForaging extends Foraging {
		private final List<Account> unlockedAccounts = new ArrayList<>();

		MockForaging() {
			super(null, null, null, null);
		}

		@Override
		public void addUnlockedAccount(final Account account) {
			this.unlockedAccounts.add(account);
		}

		@Override
		public void removeUnlockedAccount(final Account account) {
			this.unlockedAccounts.remove(account);
		}

		public List<Account> getUnlockedAccounts() { return this.unlockedAccounts; }
	}
}
