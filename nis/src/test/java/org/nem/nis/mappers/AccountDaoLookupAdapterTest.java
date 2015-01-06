package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbAccount;
import org.nem.nis.test.MockAccountDao;

public class AccountDaoLookupAdapterTest {

	//region new object

	@Test
	public void newObjectIsCreatedOnCacheMiss() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final MockAccountDao accountDao = new MockAccountDao();
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act:
		final DbAccount dbAccount = accountDaoLookup.findByAddress(address);

		// Assert:
		Assert.assertThat(dbAccount, IsNull.notNullValue());
		Assert.assertThat(dbAccount.getPrintableKey(), IsEqual.equalTo(address.getEncoded()));
		Assert.assertThat(dbAccount.getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	@Test
	public void cachedNewObjectIsReturnedOnCacheHit() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final MockAccountDao accountDao = new MockAccountDao();
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act:
		final DbAccount dbAccount1 = accountDaoLookup.findByAddress(address); // cache miss
		final DbAccount dbAccount2 = accountDaoLookup.findByAddress(address); // cache hit

		// Assert:
		Assert.assertThat(dbAccount2, IsSame.sameInstance(dbAccount1));
	}

	//endregion

	//region existing object

	@Test
	public void existingObjectIsReturnedFromDaoOnCacheMiss() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final MockAccountDao accountDao = new MockAccountDao();
		final DbAccount dbAccountFromDao = new DbAccount();
		accountDao.addMapping(address, dbAccountFromDao);
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act:
		final DbAccount dbAccount = accountDaoLookup.findByAddress(address);

		// Assert:
		Assert.assertThat(dbAccount, IsSame.sameInstance(dbAccountFromDao));
		Assert.assertThat(dbAccount.getPrintableKey(), IsNull.nullValue());
		Assert.assertThat(dbAccount.getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	@Test
	public void cachedExistingObjectIsReturnedOnCacheHit() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final MockAccountDao accountDao = new MockAccountDao();
		final DbAccount dbAccountFromDao = new DbAccount();
		accountDao.addMapping(address, dbAccountFromDao);
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act:
		final DbAccount dbAccount1 = accountDaoLookup.findByAddress(address); // cache miss
		final DbAccount dbAccount2 = accountDaoLookup.findByAddress(address); // cache hit

		// Assert:
		Assert.assertThat(dbAccount2, IsSame.sameInstance(dbAccount1));
		Assert.assertThat(accountDao.getNumGetAccountByPrintableAddressCalls(), IsEqual.equalTo(1));
	}

	//endregion

	//region public key updating

	@Test
	public void cachedObjectPublicKeyCanBeUpdated() {
		// Arrange:
		final Address addressWithPublicKey = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(addressWithPublicKey.getEncoded());
		final MockAccountDao accountDao = new MockAccountDao();
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act: request the account with an address that doesn't have a public key
		final DbAccount dbAccount1 = accountDaoLookup.findByAddress(addressWithoutPublicKey);

		// Assert: the returned account should not have a public key
		Assert.assertThat(dbAccount1.getPublicKey(), IsNull.nullValue());

		// Act: request the account with an address that has the public key
		final DbAccount dbAccount2 = accountDaoLookup.findByAddress(addressWithPublicKey);

		// Assert: the returned account should have a public key
		Assert.assertThat(dbAccount2.getPublicKey(), IsEqual.equalTo(addressWithPublicKey.getPublicKey()));
		Assert.assertThat(dbAccount2, IsSame.sameInstance(dbAccount1));
	}

	@Test
	public void publicKeyIsOnlyUpdatedIfItIsUnset() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PublicKey originalPublicKey = Utils.generateRandomPublicKey();
		final MockAccountDao accountDao = new MockAccountDao();
		final DbAccount dbAccountFromDao = new DbAccount(address.getEncoded(), originalPublicKey);
		accountDao.addMapping(address, dbAccountFromDao);
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);

		// Act: request the account with the "real" address
		final DbAccount dbAccount1 = accountDaoLookup.findByAddress(address);

		// Assert: the returned account should still have the original public key
		Assert.assertThat(address.getPublicKey(), IsNot.not(IsEqual.equalTo(originalPublicKey)));
		Assert.assertThat(dbAccount1.getPublicKey(), IsEqual.equalTo(originalPublicKey));
	}

	//endregion
}
