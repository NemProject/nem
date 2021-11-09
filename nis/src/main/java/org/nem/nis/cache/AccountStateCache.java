package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

/**
 * A repository of all NEM account state.
 */
public interface AccountStateCache extends ReadOnlyAccountStateCache {

	/**
	 * Finds a poi account state given an address. This function will NOT return forwarded states.
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	AccountState findStateByAddress(Address address);

	/**
	 * Finds the latest poi account state given an address following all forwards. When passed a remote harvester, it will return the state
	 * for the "owner" (the account harvesting remotely). Otherwise, it will return the state for the passed in address. <br>
	 * Let's say we have account A and remote account B: A has link (B, height, HarvestingRemotely); B has link (A, height, RemoteHarvester)
	 * <br>
	 * findForwardedStateByAddress(A *) should return A; findForwardedStateByAddress(B, h+1439) should return B;
	 * findForwardedStateByAddress(B, h+1440) should return B
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	AccountState findLatestForwardedStateByAddress(Address address);

	/**
	 * Finds a poi account state given an address following all forwards at a height. When passed a remote harvester, it will return the
	 * state for the "owner" (the account harvesting remotely). Otherwise, it will return the state for the passed in address <br>
	 * Let's say we have account A and remote account B: A has link (B, height, HarvestingRemotely); B has link (A, height, RemoteHarvester)
	 * <br>
	 * findForwardedStateByAddress(A *) should return A; findForwardedStateByAddress(B, h+1439) should return B;
	 * findForwardedStateByAddress(B, h+1440) should return A
	 *
	 * @param address The address.
	 * @param height Height at which check should be performed.
	 * @return The poi account state.
	 */
	AccountState findForwardedStateByAddress(Address address, BlockHeight height);

	/**
	 * Gets the number of account states.
	 *
	 * @return The number of account states.
	 */
	int size();

	/**
	 * Removes an account state from the cache if it is in the cache.
	 *
	 * @param address The address of the account state to remove.
	 */
	void removeFromCache(Address address);

	/**
	 * Undoes weighted balances vesting to a given block height.
	 *
	 * @param height The block height.
	 */
	void undoVesting(BlockHeight height);

	/**
	 * Gets the mutable contents of this cache.
	 *
	 * @return The cache contents.
	 */
	CacheContents<AccountState> mutableContents();
}
