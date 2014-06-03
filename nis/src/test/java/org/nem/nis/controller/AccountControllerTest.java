package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.Foraging;
import org.nem.nis.controller.viewmodels.AccountPageBuilder;
import org.nem.nis.service.*;

import java.util.*;

import static org.mockito.Mockito.mock;

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
	public void accountGetDelegatesToAccountIoAdapter() {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final TestContext context = new TestContext(accountLookup);
		accountLookup.setMockAccount(account);

		// Act:
		final Account resultAccount = context.controller.accountGet(account.getAddress().getEncoded());

		// Assert:
		Assert.assertThat(resultAccount, IsSame.sameInstance(account));
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

	private static class TestContext {
		private final MockForaging foraging = new MockForaging();
		private final AccountController controller;

		public TestContext() {
			this(new MockAccountLookup());
		}

		public TestContext(final AccountLookup accountLookup) {
			this(new AccountIoAdapter(
					mock(RequiredTransferDao.class),
					mock(RequiredBlockDao.class),
					accountLookup));
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

		public List<Account> getUnlockedAccounts() { return this.unlockedAccounts; }
	}
}
