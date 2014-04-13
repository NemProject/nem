package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.serialization.*;
import org.nem.nis.Foraging;

import java.util.ArrayList;
import java.util.List;

public class AccountControllerTest {

	@Test
	public void unlockAddsAccountToForaging() throws Exception {
		// Arrange:
		final Account account = org.nem.core.test.Utils.generateRandomAccount();
		final JsonDeserializer deserializer = getPrivateKeyDeserializer(account);
		try (final MockForaging foraging = new MockForaging()) {
			final AccountController controller = new AccountController(foraging);

			// Act:
			controller.accountUnlock(deserializer);

			// Assert:
			Assert.assertThat(foraging.getUnlockedAccounts().size(), IsEqual.equalTo(1));
			Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsEqual.equalTo(account));
			Assert.assertThat(foraging.getUnlockedAccounts().get(0), IsNot.not(IsSame.sameInstance(account)));
		}
	}

	private static JsonDeserializer getPrivateKeyDeserializer(final Account account) {
		final JsonSerializer serializer = new JsonSerializer();
		account.getKeyPair().getPrivateKey().serialize(serializer);
		return new JsonDeserializer(serializer.getObject(), new DeserializationContext(null));
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
