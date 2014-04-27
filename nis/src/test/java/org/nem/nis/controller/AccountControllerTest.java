package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.test.MockAccountLookup;
import org.nem.nis.Foraging;
import org.nem.nis.service.AccountIoAdapter;
import org.nem.nis.test.MockTransferDaoImpl;

import java.util.*;

public class AccountControllerTest {

	@Test
	public void unlockAddsAccountToForaging() throws Exception {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		try (final MockForaging foraging = new MockForaging()) {
			final MockTransferDaoImpl mockTransferDao = new MockTransferDaoImpl();
			final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(mockTransferDao, new MockAccountLookup());
			final AccountController controller = new AccountController(foraging, accountIoAdapter);

			// Act:
			controller.accountUnlock(account.getKeyPair().getPrivateKey());

			// Assert:
			Assert.assertThat(foraging.getUnlockedAccounts().size(), IsEqual.equalTo(1));
			Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsEqual.equalTo(account));
			Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsNot.not(IsSame.sameInstance(account)));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountGetReturnsError() throws Exception {
		// Arrange:
		try (final MockForaging foraging = new MockForaging()) {
			final MockTransferDaoImpl mockTransferDao = new MockTransferDaoImpl();
			final AccountIoAdapter accountIoAdapter = new AccountIoAdapter(mockTransferDao, new MockAccountLookup());
			final AccountController controller = new AccountController(foraging, accountIoAdapter);

			// Act:
			controller.accountGet("dummy");
		}
	}

	private static class MockForaging extends Foraging {

		private final List<Account> unlockedAccounts = new ArrayList<>();

		@Override
		public void addUnlockedAccount(final Account account) {
			this.unlockedAccounts.add(account);
		}

		public List<Account> getUnlockedAccounts() { return this.unlockedAccounts; }
	}
}
