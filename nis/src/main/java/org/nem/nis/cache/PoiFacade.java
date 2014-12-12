package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;
import org.nem.nis.validators.DebitPredicate;

/**
 * A repository of all mutable NEM account state.
 * TODO 20141211 - rename to something else!
 */
public interface PoiFacade extends Iterable<PoiAccountState> {

	/**
	 * Finds a poi account state given an address. This function will NOT return
	 * forwarded states.
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	public PoiAccountState findStateByAddress(final Address address);

	/**
	 * Finds the latest poi account state given an address following all forwards.
	 * - When passed a remote harvester, it will return the state for the "owner" (the account harvesting remotely)
	 * - Otherwise, it will return the state for the passed in address
	 * <br/>
	 * Let's say we have account A and remote account B,
	 * A has link (B, height, HarvestingRemotely)
	 * B has link (A, height, RemoteHarvester)
	 * <br/>
	 * findForwardedStateByAddress(A *) should return A
	 * findForwardedStateByAddress(B, h+1439) should return B
	 * findForwardedStateByAddress(B, h+1440) should return B
	 * <br/>
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	public PoiAccountState findLatestForwardedStateByAddress(final Address address);

	/**
	 * Finds a poi account state given an address following all forwards at a height.
	 * - When passed a remote harvester, it will return the state for the "owner" (the account harvesting remotely)
	 * - Otherwise, it will return the state for the passed in address
	 * <br/>
	 * Let's say we have account A and remote account B,
	 * A has link (B, height, HarvestingRemotely)
	 * B has link (A, height, RemoteHarvester)
	 * <br/>
	 * findForwardedStateByAddress(A *) should return A
	 * findForwardedStateByAddress(B, h+1439) should return B
	 * findForwardedStateByAddress(B, h+1440) should return A
	 * <br/>
	 *
	 * @param address The address.
	 * @param height Height at which check should be performed.
	 * @return The poi account state.
	 */
	public PoiAccountState findForwardedStateByAddress(final Address address, final BlockHeight height);

	/**
	 * Gets the number of account states.
	 *
	 * @return The number of account states.
	 */
	public int size();

	/**
	 * Gets the size of the last poi vector (needed for time synchronization).
	 *
	 * @return The size of the last poi vector.
	 */
	public int getLastPoiVectorSize();

	/**
	 * Gets the height at which the last recalculation was (needed for time synchronization).
	 *
	 * @return The the height at which the last recalculation was.
	 */
	public BlockHeight getLastPoiRecalculationHeight();

	/**
	 * Removes an account state from the cache if it is in the cache.
	 *
	 * @param address The address of the account state to remove.
	 */
	public void removeFromCache(final Address address);

	/**
	 * Undoes weighted balances vesting to a given block height.
	 *
	 * @param height The block height.
	 */
	public void undoVesting(final BlockHeight height);

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 */
	public void recalculateImportances(final BlockHeight blockHeight);

	/**
	 * Gets a debit predicate that checks balances against the account information stored in this cache.
	 *
	 * @return The debit predicate.
	 */
	public DebitPredicate getDebitPredicate();
}
