package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.test.MockAccountLookup;
import org.nem.nis.Foraging;
import org.nem.nis.service.AccountIoAdapter;
import org.nem.nis.service.RequiredBlockDao;
import org.nem.nis.service.RequiredTransferDao;
import org.nem.nis.test.MockTransferDaoImpl;

import java.util.*;

import static org.mockito.Mockito.mock;

public class AccountControllerTest {

	@Test
	public void unlockAddsAccountToForaging() throws Exception {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final MockForaging foraging = new MockForaging();
		final RequiredTransferDao mockRequiredTransferDao = mock(RequiredTransferDao.class);
		final RequiredBlockDao mockRequiredBlockDao = mock(RequiredBlockDao.class);
		final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(mockRequiredTransferDao, mockRequiredBlockDao, new MockAccountLookup());
		final AccountController controller = new AccountController(foraging, accountIoAdapter);

		// Act:
		controller.accountUnlock(account.getKeyPair().getPrivateKey());

		// Assert:
		Assert.assertThat(foraging.getUnlockedAccounts().size(), IsEqual.equalTo(1));
		Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsEqual.equalTo(account));
		Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsNot.not(IsSame.sameInstance(account)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountGetReturnsError() throws Exception {
		// Arrange:
		final MockForaging foraging = new MockForaging();
		final RequiredTransferDao mockRequiredTransferDao = mock(RequiredTransferDao.class);
		final RequiredBlockDao mockRequiredBlockDao = mock(RequiredBlockDao.class);
		final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(mockRequiredTransferDao, mockRequiredBlockDao, new MockAccountLookup());
		final AccountController controller = new AccountController(foraging, accountIoAdapter);

		// Act:
		controller.accountGet("dummy");
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
