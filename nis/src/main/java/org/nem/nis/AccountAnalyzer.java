package org.nem.nis;

import org.nem.core.model.Address;
import org.nem.nis.poi.*;

/**
 * Account analyzer that is the central point for accessing all NIS-related account information.
 */
public class AccountAnalyzer {
	private final AccountCache accountCache;
	private final PoiFacade poiFacade;

	/**
	 * Creates a new account analyzer.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 */
	public AccountAnalyzer(final AccountCache accountCache, final PoiFacade poiFacade) {
		this.accountCache = accountCache;
		this.poiFacade = poiFacade;
	}

	/**
	 * Gets the account cache.
	 *
	 * @return The account cache.
	 */
	public AccountCache getAccountCache() { return this.accountCache; }

	/**
	 * Gets the POI facade.
	 *
	 * @return The POI facade.
	 */
	public PoiFacade getPoiFacade() { return this.poiFacade; }

	/**
	 * Creates a copy of this analyzer.
	 *
	 * @return A copy of this analyzer.
	 */
	public AccountAnalyzer copy() {
		return new AccountAnalyzer(this.accountCache.copy(), this.poiFacade.copy());
	}

	/**
	 * Shallow copies this account analyzer into another account analyzer.
	 *
	 * @param rhs The other analyzer.
	 */
	public void shallowCopyTo(final AccountAnalyzer rhs) {
		this.accountCache.shallowCopyTo(rhs.accountCache);
		this.poiFacade.shallowCopyTo(rhs.poiFacade);
	}

	/**
	 * Removes an account from the analyzer if it is in the analyzer.
	 *
	 * @param address The address of the account to remove.
	 */
	public void removeAccount(final Address address) {
		this.accountCache.removeFromCache(address);
		this.poiFacade.removeFromCache(address);
	}
}
