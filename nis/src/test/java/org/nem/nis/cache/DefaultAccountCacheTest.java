package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountCache;

import java.util.*;
import java.util.stream.*;

public class DefaultAccountCacheTest {

	//region addAccountToCache

	@Test
	public void accountWithoutPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void accountWithPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void cachedAccountWithPublicKeyIsUnchangedWhenQueryingByPublicKey() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(address);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUnchangedWhenQueryingByEncodedAddress() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(address);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUpdatedWhenQueryingWithPublicKey() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(addressWithoutPublicKey);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2.getAddress(), IsEqual.equalTo(cachedAccount1.getAddress()));
		Assert.assertThat(cachedAccount2.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	//endregion

	//region removeFromCache

	@Test
	public void accountWithoutPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = cache.addAccountToCache(address);
		cache.removeFromCache(account.getAddress());

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void accountWithPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = cache.addAccountToCache(address);
		cache.removeFromCache(account.getAddress());

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void removeAccountFromCacheDoesNothingIfAddressIsNotInCache() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		cache.addAccountToCache(address);
		cache.removeFromCache(Utils.generateRandomAddress());

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
	}

	//endregion

	//region findByAddress

	@Test(expected = MissingResourceException.class)
	public void findByAddressFailsIfAddressIsInvalid() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());
		final String realAddress = address.getEncoded();
		final String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

		// Act:
		cache.findByAddress(Address.fromEncoded(fakeAddress));
	}

	@Test
	public void findByAddressReturnsCachedAddressIfAvailable() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(address);
		final Account foundAddress = cache.findByAddress(address);

		// Assert:
		Assert.assertThat(foundAddress, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void findByAddressReturnsNonCachedAddressIfPublicKeyIsNotFound() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account foundAccount = cache.findByAddress(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	@Test
	public void findByAddressReturnsNonCachedAddressIfEncodedAddressIsNotFound() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account foundAccount = cache.findByAddress(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void findByAddressUpdatesAccountPublicKeyIfQueryingAccountHasPublicKeyButCachedAccountDoesNot() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(addressWithoutPublicKey);
		final Account foundAccount = cache.findByAddress(address);

		// Assert:
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(cachedAccount1.getAddress()));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	//endregion

	//region isKnownAddress

	@Test
	public void isKnownAddressReturnsTrueIfAddressIsKnown() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.isKnownAddress(address), IsEqual.equalTo(true));
	}

	@Test
	public void isKnownAddressReturnsFalseIfAddressIsUnknown() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();

		// Act:
		cache.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cache.isKnownAddress(address2), IsEqual.equalTo(false));
	}

	//endregion

	//region asAutoCache

	@Test
	public void asAutoCacheFindByAddressReturnsCachedAddressIfPublicKeyIsNotFound() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account foundAccount = cache.asAutoCache().findByAddress(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void asAutoCacheFindByAddressReturnsCachedAddressIfEncodedAddressIsNotFound() {
		// Arrange:
		final AccountCache cache = createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account foundAccount = cache.asAutoCache().findByAddress(address);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesFullCopyOfAllAccounts() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final DefaultAccountCache cache = createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);
		final Account account2 = cache.addAccountToCache(address2);
		final Account account3 = cache.addAccountToCache(address3);

		// Act:
		final AccountCache copyCache = cache.copy();

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);
		final Account copyAccount3 = copyCache.findByAddress(address3);

		// Assert: since the items are immutable, it is ok for them to be the same
		Assert.assertThat(copyCache.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		Assert.assertThat(copyAccount3, IsSame.sameInstance(account3));
	}

	@Test
	public void copyCreatesUnlinkedCacheCopy() {
		// Arrange:
		final DefaultAccountCache cache = createAccountCache();
		cache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());

		// Act:
		final AccountCache copyCache = cache.copy();
		copyCache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());
		copyCache.addAccountToCache(Utils.generateRandomAddress());

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(copyCache.size(), IsEqual.equalTo(5));
	}

	@Test
	public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final DefaultAccountCache cache = createAccountCache();

		cache.addAccountToCache(address1);

		// Act:
		final AccountCache copyCache = cache.copy();

		final Account copyAccountFromEncoded = copyCache.findByAddress(Address.fromEncoded(address1.getEncoded()));
		final Account copyAccountFromPublicKey = copyCache.findByAddress(address1);

		// Assert:
		Assert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		Assert.assertThat(copyAccountFromEncoded, IsSame.sameInstance(copyAccountFromPublicKey));
	}

	//endregion

	//region shallowCopyTo

	@Test
	public void shallowCopyToCreatesLinkedCacheCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final DefaultAccountCache cache = createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);
		final Account account2 = cache.addAccountToCache(address2);
		final Account account3 = cache.addAccountToCache(address3);

		// Act:
		final DefaultAccountCache copyCache = createAccountCache();
		cache.shallowCopyTo(copyCache);

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);
		final Account copyAccount3 = copyCache.findByAddress(address3);

		// Assert:
		Assert.assertThat(copyCache.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		Assert.assertThat(copyAccount3, IsSame.sameInstance(account3));
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final DefaultAccountCache cache = createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);

		final DefaultAccountCache copyCache = createAccountCache();
		final Account account2 = copyCache.addAccountToCache(address2);

		// Act:
		cache.shallowCopyTo(copyCache);

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);

		// Assert:
		Assert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsNot.not(IsSame.sameInstance(account2)));
	}

	//endregion

	//region iterator

	@Test
	public void iteratorReturnsAllAccounts() {
		// Arrange:
		final AccountCache cache = createAccountCache();

		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			accounts.add(cache.addAccountToCache(Utils.generateRandomAddress()));
		}

		// Act:
		final List<Account> iteratedAccounts = StreamSupport.stream(cache.spliterator(), false)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(iteratedAccounts.size(), IsEqual.equalTo(3));
		Assert.assertThat(iteratedAccounts, IsEquivalent.equivalentTo(accounts));
	}

	//endregion

	private static DefaultAccountCache createAccountCache() {
		return new DefaultAccountCache();
	}
}