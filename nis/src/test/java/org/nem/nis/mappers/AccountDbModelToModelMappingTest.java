package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbAccount;

public class AccountDbModelToModelMappingTest {

	@Test
	public void canMapDbAccountWithPublicKeyToAccount() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		canMapDbAccountWithoutPublicKeyToAccount(address.getEncoded(), address.getPublicKey());
	}

	@Test
	public void canMapDbAccountWithoutPublicKeyToAccount() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		canMapDbAccountWithoutPublicKeyToAccount(address.getEncoded(), null);
	}

	private static void canMapDbAccountWithoutPublicKeyToAccount(final String encodedAddress, final PublicKey publicKey) {
		// Arrange:
		final DbAccount dbAccount = new DbAccount(encodedAddress, publicKey);
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		final Account accountReturnedByAccountLookup = Mockito.mock(Account.class);
		Mockito.when(accountLookup.findByAddress(Mockito.any())).thenReturn(accountReturnedByAccountLookup);

		final AccountDbModelToModelMapping mapping = new AccountDbModelToModelMapping(accountLookup);

		// Act:
		final Account account = mapping.map(dbAccount);

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(accountReturnedByAccountLookup));

		final ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
		Mockito.verify(accountLookup, Mockito.only()).findByAddress(addressCaptor.capture());
		Assert.assertThat(addressCaptor.getValue().getEncoded(), IsEqual.equalTo(encodedAddress));
		Assert.assertThat(addressCaptor.getValue().getPublicKey(), IsEqual.equalTo(publicKey));
	}
}