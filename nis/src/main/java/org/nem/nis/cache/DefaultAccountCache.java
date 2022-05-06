package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.nis.cache.delta.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * A simple, in-memory account cache that implements AccountLookup and provides the lookup of accounts by their addresses.
 */
public class DefaultAccountCache implements ExtendedAccountCache<DefaultAccountCache> {
	private static final int INITIAL_CAPACITY = 65536;

	private final ImmutableObjectDeltaMap<Address, Account> addressToAccountMap;
	private boolean isCopy = false;

	/**
	 * Creates a new account cache.
	 */
	public DefaultAccountCache() {
		this(new ImmutableObjectDeltaMap<>(INITIAL_CAPACITY));
	}

	private DefaultAccountCache(final ImmutableObjectDeltaMap<Address, Account> addressToAccountMap) {
		this.addressToAccountMap = addressToAccountMap;
	}

	@Override
	public int size() {
		return this.addressToAccountMap.size();
	}

	@Override
	public CacheContents<Account> contents() {
		// TODO 2015 J-J move to delta map?
		return new CacheContents<>(this.addressToAccountMap.streamValues().collect(Collectors.toList()));
	}

	@Override
	public void shallowCopyTo(final DefaultAccountCache rhs) {
		this.addressToAccountMap.shallowCopyTo(rhs.addressToAccountMap);
	}

	@Override
	public Account addAccountToCache(final Address address) {
		return this.addAccountToCache(address, Address::isValid);
	}

	private Account addAccountToCache(final Address address, final Predicate<Address> validator) {
		return this.findByAddress(address, validator, () -> {
			final Account account = new Account(address);
			this.addressToAccountMap.put(address, account);
			return account;
		});
	}

	@Override
	public void removeFromCache(final Address address) {
		this.addressToAccountMap.remove(address);
	}

	private Account findByAddress(final Address address, final Predicate<Address> validator, final Supplier<Account> notFoundHandler) {
		if (!validator.test(address)) {
			throw new MissingResourceException("invalid address", Address.class.getName(), address.toString());
		}

		final Account account = this.findByAddressImpl(address);
		return null != account ? account : notFoundHandler.get();
	}

	private Account findByAddressImpl(final Address address) {
		Account account = this.addressToAccountMap.get(address);
		if (null == account) {
			return null;
		}

		if (null == account.getAddress().getPublicKey() && null != address.getPublicKey()) {
			account = new Account(address);
			this.addressToAccountMap.put(address, account);
		}

		return account;
	}

	@Override
	public Account findByAddress(final Address address) {
		return this.findByAddress(address, Address::isValid);
	}

	@Override
	public Account findByAddress(final Address address, final Predicate<Address> validator) {
		return this.findByAddress(address, validator, () -> new Account(address));
	}

	@Override
	public boolean isKnownAddress(final Address address) {
		return this.addressToAccountMap.containsKey(address);
	}

	@Override
	public DefaultAccountCache copy() {
		if (this.isCopy) {
			// TODO 20151013 J-*: add test for this case
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all.
		final DefaultAccountCache copy = new DefaultAccountCache(this.addressToAccountMap.rebase());
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void commit() {
		// TODO 20151013 J-*: add test for commit
		this.addressToAccountMap.commit();
	}

	/**
	 * Creates a deep copy of this account cache.
	 *
	 * @return The deep copy.
	 */
	public DefaultAccountCache deepCopy() {
		// TODO 20151013 J-*: add test for deepCopy
		if (this.isCopy) {
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		return new DefaultAccountCache(this.addressToAccountMap.deepCopy());
	}
}
