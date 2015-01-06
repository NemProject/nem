package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbAccount;

public class AccountModelToDbModelMappingTest {

	@Test
	public void canMapAccountToDbAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final AccountDaoLookup accountLookup = Mockito.mock(AccountDaoLookup.class);
		final DbAccount dbAccountReturnedByAccountLookup = Mockito.mock(DbAccount.class);
		Mockito.when(accountLookup.findByAddress(account.getAddress())).thenReturn(dbAccountReturnedByAccountLookup);

		final AccountModelToDbModelMapping mapping = new AccountModelToDbModelMapping(accountLookup);

		// Act:
		final DbAccount dbAccount = mapping.map(account);

		// Assert:
		Assert.assertThat(dbAccount, IsEqual.equalTo(dbAccountReturnedByAccountLookup));
		Mockito.verify(accountLookup, Mockito.only()).findByAddress(account.getAddress());
	}
}