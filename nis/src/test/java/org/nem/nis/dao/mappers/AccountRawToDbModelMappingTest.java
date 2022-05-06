package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbAccount;

public class AccountRawToDbModelMappingTest {

	@Test
	public void canMapIdToDbAccountIfIdIsNotNull() {
		// Act:
		final AccountRawToDbModelMapping mapping = new AccountRawToDbModelMapping();
		final DbAccount dbAccount = mapping.map(123L);

		// Assert:
		MatcherAssert.assertThat(dbAccount, IsNull.notNullValue());
		MatcherAssert.assertThat(dbAccount.getId(), IsEqual.equalTo(123L));
	}

	@Test
	public void mapReturnsNullIfIdIsNull() {
		// Act:
		final AccountRawToDbModelMapping mapping = new AccountRawToDbModelMapping();
		final DbAccount dbAccount = mapping.map(null);

		// Assert:
		MatcherAssert.assertThat(dbAccount, IsNull.nullValue());
	}
}
