package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class AccountToAddressMapperTest {
	@Test
	public void addressCanBeCreatedAroundDbAccount() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final String encoded = Address.fromPublicKey(publicKey).getEncoded();
		final org.nem.nis.dbmodel.Account dbAccount = new org.nem.nis.dbmodel.Account(encoded, publicKey);

		// Act:
		final Address address = AccountToAddressMapper.toAddress(dbAccount);

		// Assert:
		Assert.assertThat(address.getEncoded(), IsEqual.equalTo(encoded));
		Assert.assertThat(address.getPublicKey(), IsEqual.equalTo(publicKey));
	}
}
