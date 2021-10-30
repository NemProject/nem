package org.nem.nis.dbmodel;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

import java.util.*;

public class DbAccountTest {

	// region constructor

	@Test
	public void canCreateAccountUsingDefaultConstructor() {
		// Act:
		final DbAccount account = new DbAccount();

		// Assert:
		MatcherAssert.assertThat(account.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPrintableKey(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void canCreateAccountFromId() {
		// Act:
		final DbAccount account = new DbAccount(7);

		// Assert:
		MatcherAssert.assertThat(account.getId(), IsEqual.equalTo(7L));
		MatcherAssert.assertThat(account.getPrintableKey(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void canCreateAccountFromPrintableKey() {
		// Act:
		final DbAccount account = new DbAccount("TALICE", null);

		// Assert:
		MatcherAssert.assertThat(account.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPrintableKey(), IsEqual.equalTo("TALICE"));
		MatcherAssert.assertThat(account.getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void canCreateAccountFromPrintableKeyAndPublicKey() {
		// Act:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final DbAccount account = new DbAccount("TALICE", publicKey);

		// Assert:
		MatcherAssert.assertThat(account.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPrintableKey(), IsEqual.equalTo("TALICE"));
		MatcherAssert.assertThat(account.getPublicKey(), IsEqual.equalTo(publicKey));
	}

	@Test
	public void canCreateAccountFromAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final DbAccount account = new DbAccount(address);

		// Assert:
		MatcherAssert.assertThat(account.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(account.getPrintableKey(), IsEqual.equalTo(address.getEncoded()));
		MatcherAssert.assertThat(account.getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	// endregion

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, DbAccount> DESC_TO_DB_ACCOUNT_MAP = new HashMap<String, DbAccount>() {
		{
			final PublicKey publicKey = Utils.generateRandomPublicKey();
			this.put("default", createDbAccount(1L, "TALICE", publicKey));
			this.put("copy", createDbAccount(1L, "TALICE", publicKey));
			this.put("uninitialized", new DbAccount());
			this.put("diff-id", createDbAccount(2L, "TALICE", publicKey));
			this.put("diff-address", createDbAccount(1L, "TBOB", publicKey));
			this.put("diff-pubKey", createDbAccount(1L, "TALICE", Utils.generateRandomPublicKey()));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final DbAccount dbAccount = DESC_TO_DB_ACCOUNT_MAP.get("default");

		// Assert:
		final List<String> differentKeys = Arrays.asList("uninitialized", "diff-id");
		for (final Map.Entry<String, DbAccount> entry : DESC_TO_DB_ACCOUNT_MAP.entrySet()) {
			final Matcher<DbAccount> matcher = differentKeys.contains(entry.getKey())
					? IsNot.not(IsEqual.equalTo(dbAccount))
					: IsEqual.equalTo(dbAccount);

			MatcherAssert.assertThat(entry.getValue(), matcher);
		}
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = DESC_TO_DB_ACCOUNT_MAP.get("default").hashCode();

		// Assert:
		final List<String> differentKeys = Arrays.asList("uninitialized", "diff-id");
		for (final Map.Entry<String, DbAccount> entry : DESC_TO_DB_ACCOUNT_MAP.entrySet()) {
			final Matcher<Integer> matcher = differentKeys.contains(entry.getKey())
					? IsNot.not(IsEqual.equalTo(hashCode))
					: IsEqual.equalTo(hashCode);

			MatcherAssert.assertThat(entry.getValue().hashCode(), matcher);
		}
	}

	// endregion

	private static DbAccount createDbAccount(final Long id, final String printableKey, final PublicKey publicKey) {
		final DbAccount account = new DbAccount(printableKey, publicKey);
		account.setId(id);
		return account;
	}
}
