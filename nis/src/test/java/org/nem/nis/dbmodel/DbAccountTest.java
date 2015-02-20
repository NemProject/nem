package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.nis.test.NisUtils;

import java.util.*;
public class DbAccountTest {

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = DESC_TO_DB_ACCOUNT_MAP.get("default").hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("copy").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("uninitialized").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-id").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address").hashCode(), IsEqual.equalTo(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey").hashCode()));
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final DbAccount dbAccount = DESC_TO_DB_ACCOUNT_MAP.get("default");

		// Assert:
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("copy"), IsEqual.equalTo(dbAccount));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("uninitialized"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-id"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address"), IsEqual.equalTo(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey")));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(1L, IsNot.not(IsEqual.equalTo(dbAccount)));
	}

	private static final Map<String, DbAccount> DESC_TO_DB_ACCOUNT_MAP = new HashMap<String, DbAccount>() {
		{
			this.put("default", new DbAccount(1L));
			this.put("copy", new DbAccount(1L));
			this.put("uninitialized", new DbAccount(null, null));
			this.put("diff-id", new DbAccount(2L));
			this.put("diff-address", new DbAccount("TALICE", new PublicKey(new byte[32])));
			this.put("diff-address-and-pubKey", new DbAccount("TALICE", new PublicKey(new byte[33])));
		}
	};
}
