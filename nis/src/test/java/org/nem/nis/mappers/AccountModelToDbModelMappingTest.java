package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class AccountModelToDbModelMappingTest {

	@Test
	public void canMapAccountToDbAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final AccountDaoLookup accountLookup = Mockito.mock(AccountDaoLookup.class);
		final org.nem.nis.dbmodel.Account dbAccountReturnedByAccountLookup = Mockito.mock(org.nem.nis.dbmodel.Account.class);
		Mockito.when(accountLookup.findByAddress(account.getAddress())).thenReturn(dbAccountReturnedByAccountLookup);

		final AccountModelToDbModelMapping mapping = new AccountModelToDbModelMapping(accountLookup);

		// Act:
		final org.nem.nis.dbmodel.Account dbAccount = mapping.map(account);

		// Assert:
		Assert.assertThat(dbAccount, IsEqual.equalTo(dbAccountReturnedByAccountLookup));
		Mockito.verify(accountLookup, Mockito.only()).findByAddress(account.getAddress());
	}
}