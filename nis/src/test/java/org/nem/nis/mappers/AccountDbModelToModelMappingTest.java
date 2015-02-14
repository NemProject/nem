package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.nis.cache.DefaultAccountCache;
import org.nem.nis.dbmodel.DbAccount;

public class AccountDbModelToModelMappingTest {

	@Test
	public void canMapDbAccountWithPublicKeyToAccount() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		canMapDbAccountToAccount(address.getEncoded(), address.getPublicKey());
	}

	@Test
	public void canMapDbAccountWithoutPublicKeyToAccount() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		canMapDbAccountToAccount(address.getEncoded(), null);
	}

	@Test
	public void canMapDbAccountWithInvalidAddressToAccount() {
		// Arrange:
		// - this is a proxy that the address isn't validated by the mapper
		final Address address = Address.fromEncoded("BLAH");
		final DbAccount dbAccount = new DbAccount(address.getEncoded(), null);

		// - use the real default account cache because that is what did the validation
		final AccountLookup accountLookup = new DefaultAccountCache();
		final AccountDbModelToModelMapping mapping = new AccountDbModelToModelMapping(accountLookup);

		// Act:
		final Account account = mapping.map(dbAccount);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(account.getAddress().isValid(), IsEqual.equalTo(false));
	}

	private static void canMapDbAccountToAccount(final String encodedAddress, final PublicKey publicKey) {
		// Arrange:
		final DbAccount dbAccount = new DbAccount(encodedAddress, publicKey);
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		final Account accountReturnedByAccountLookup = Mockito.mock(Account.class);
		Mockito.when(accountLookup.findByAddress(Mockito.any(), Mockito.any())).thenReturn(accountReturnedByAccountLookup);

		final AccountDbModelToModelMapping mapping = new AccountDbModelToModelMapping(accountLookup);

		// Act:
		final Account account = mapping.map(dbAccount);

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(accountReturnedByAccountLookup));

		final ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
		Mockito.verify(accountLookup, Mockito.only()).findByAddress(addressCaptor.capture(), Mockito.any());
		Assert.assertThat(addressCaptor.getValue().getEncoded(), IsEqual.equalTo(encodedAddress));
		Assert.assertThat(addressCaptor.getValue().getPublicKey(), IsEqual.equalTo(publicKey));
	}
}