package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.function.Function;

public abstract class AccountCacheTest<T extends ExtendedAccountCache<T>> {

	/**
	 * Creates an account cache
	 *
	 * @return The account cache.
	 */
	protected abstract T createAccountCache();

	// region addAccountToCache

	@Test
	public void accountWithoutPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void accountWithPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void cachedAccountWithPublicKeyIsUnchangedWhenQueryingByPublicKey() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(address);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUnchangedWhenQueryingByEncodedAddress() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(address);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUpdatedWhenQueryingWithPublicKey() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = cache.addAccountToCache(addressWithoutPublicKey);
		final Account cachedAccount2 = cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cachedAccount2.getAddress(), IsEqual.equalTo(cachedAccount1.getAddress()));
		MatcherAssert.assertThat(cachedAccount2.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	// endregion

	// region removeFromCache

	@Test
	public void accountWithoutPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = cache.addAccountToCache(address);
		cache.removeFromCache(account.getAddress());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void accountWithPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = cache.addAccountToCache(address);
		cache.removeFromCache(account.getAddress());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void removeAccountFromCacheDoesNothingIfAddressIsNotInCache() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		cache.addAccountToCache(address);
		cache.removeFromCache(Utils.generateRandomAddress());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region findByAddress

	@Test(expected = MissingResourceException.class)
	public void findByAddressFailsIfAddressIsInvalid() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());
		final String realAddress = address.getEncoded();
		final String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

		// Act:
		cache.findByAddress(Address.fromEncoded(fakeAddress));
	}

	@Test
	public void findByAddressReturnsCachedAccountIfAvailable() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount = cache.addAccountToCache(address);
		final Account foundAddress = cache.findByAddress(address);

		// Assert:
		MatcherAssert.assertThat(foundAddress, IsSame.sameInstance(cachedAccount));
	}

	@Test
	public void findByAddressReturnsNonCachedAccountIfPublicKeyIsNotFound() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		this.assertFindByAddressReturnsNonCachedAccount(address, cache -> cache.findByAddress(address));
	}

	@Test
	public void findByAddressReturnsNonCachedAccountIfEncodedAddressIsNotFound() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		this.assertFindByAddressReturnsNonCachedAccount(address, cache -> cache.findByAddress(address));
	}

	@Test
	public void findByAddressUpdatesAccountPublicKeyIfQueryingAccountHasPublicKeyButCachedAccountDoesNot() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount = cache.addAccountToCache(addressWithoutPublicKey);
		final Account foundAccount = cache.findByAddress(address);

		// Assert:
		MatcherAssert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(cachedAccount.getAddress()));
		MatcherAssert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	@Test
	public void findByAddressReturnsNonCachedAccountIfEncodedAddressIsNotFoundAndCustomValidatorSucceeds() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		this.assertFindByAddressReturnsNonCachedAccount(address, cache -> cache.findByAddress(address, a -> true));
	}

	@Test(expected = MissingResourceException.class)
	public void findByAddressFailsIfEncodedAddressIsNotFoundAndCustomValidatorFails() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Assert:
		cache.findByAddress(address, a -> false);
	}

	private void assertFindByAddressReturnsNonCachedAccount(final Address address, final Function<AccountCache, Account> findByAddress) {
		// Arrange:
		final AccountCache cache = this.createAccountCache();

		// Act:
		final Account foundAccount = findByAddress.apply(cache);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	// endregion

	// region isKnownAddress

	@Test
	public void isKnownAddressReturnsTrueIfAddressIsKnown() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();

		// Act:
		cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.isKnownAddress(address), IsEqual.equalTo(true));
	}

	@Test
	public void isKnownAddressReturnsFalseIfAddressIsUnknown() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();
		final Address address = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();

		// Act:
		cache.addAccountToCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.isKnownAddress(address2), IsEqual.equalTo(false));
	}

	// endregion

	// region copy

	@Test
	public void copyCreatesFullCopyOfAllAccounts() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final T cache = this.createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);
		final Account account2 = cache.addAccountToCache(address2);
		final Account account3 = cache.addAccountToCache(address3);
		cache.commit();

		// Act:
		final AccountCache copyCache = cache.copy();

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);
		final Account copyAccount3 = copyCache.findByAddress(address3);

		// Assert: since the items are immutable, it is ok for them to be the same
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		MatcherAssert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		MatcherAssert.assertThat(copyAccount3, IsSame.sameInstance(account3));
	}

	@Test
	public void copyCreatesUnlinkedCacheCopy() {
		// Arrange:
		final T cache = this.createAccountCache();
		cache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());
		cache.commit();

		// Act:
		final T copyCache = cache.copy();
		copyCache.addAccountToCache(Utils.generateRandomAddress());
		cache.addAccountToCache(Utils.generateRandomAddress());
		copyCache.addAccountToCache(Utils.generateRandomAddress());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(5));
	}

	@Test
	public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final T cache = this.createAccountCache();

		cache.addAccountToCache(address1);
		cache.commit();

		// Act:
		final AccountCache copyCache = cache.copy();

		final Account copyAccountFromEncoded = copyCache.findByAddress(Address.fromEncoded(address1.getEncoded()));
		final Account copyAccountFromPublicKey = copyCache.findByAddress(address1);

		// Assert:
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copyAccountFromEncoded, IsSame.sameInstance(copyAccountFromPublicKey));
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCreatesLinkedCacheCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final T cache = this.createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);
		final Account account2 = cache.addAccountToCache(address2);
		final Account account3 = cache.addAccountToCache(address3);

		// Act:
		final T copyCache = this.createAccountCache();
		cache.shallowCopyTo(copyCache);

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);
		final Account copyAccount3 = copyCache.findByAddress(address3);

		// Assert:
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		MatcherAssert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		MatcherAssert.assertThat(copyAccount3, IsSame.sameInstance(account3));
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final T cache = this.createAccountCache();

		final Account account1 = cache.addAccountToCache(address1);

		final T copyCache = this.createAccountCache();
		final Account account2 = copyCache.addAccountToCache(address2);

		// Act:
		cache.shallowCopyTo(copyCache);

		final Account copyAccount1 = copyCache.findByAddress(address1);
		final Account copyAccount2 = copyCache.findByAddress(address2);

		// Assert:
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		MatcherAssert.assertThat(copyAccount2, IsNot.not(IsSame.sameInstance(account2)));
	}

	// endregion

	// region contents

	@Test
	public void contentsReturnsAllAccounts() {
		// Arrange:
		final AccountCache cache = this.createAccountCache();

		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			accounts.add(cache.addAccountToCache(Utils.generateRandomAddress()));
		}

		// Act:
		final Collection<Account> iteratedAccounts = cache.contents().asCollection();

		// Assert:
		MatcherAssert.assertThat(iteratedAccounts.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(iteratedAccounts, IsEquivalent.equivalentTo(accounts));
	}

	// endregion
}
