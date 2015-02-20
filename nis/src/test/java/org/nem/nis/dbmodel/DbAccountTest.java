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
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-id"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey"), IsNot.not(IsEqual.equalTo(dbAccount)));
		Assert.assertThat(DESC_TO_DB_ACCOUNT_MAP.get("diff-address"), IsEqual.equalTo(DESC_TO_DB_ACCOUNT_MAP.get("diff-address-and-pubKey")));
	}

	private static final Map<String, DbAccount> DESC_TO_DB_ACCOUNT_MAP = new HashMap<String, DbAccount>() {
		{
			this.put("default", NisUtils.createDbAccount(1L));
			this.put("copy", NisUtils.createDbAccount(1L));
			this.put("diff-id", NisUtils.createDbAccount(2L));
			this.put("diff-address", NisUtils.createDbAccount("TALICE", new PublicKey(new byte[32])));
			this.put("diff-address-and-pubKey", NisUtils.createDbAccount("TALICE", new PublicKey(new byte[33])));
		}
	};
}
