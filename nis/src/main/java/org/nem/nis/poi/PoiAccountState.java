package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.CircularStack;
import org.nem.nis.secret.*;

/**
 * Class containing extrinsic NIS-account information that is used to calculate POI.
 * TODO-CR 20140808 J->ALL i think the naming is confusing between PoiAccountState and PoiAccountInfo ... should try to think of better names
 * G->J, I think we can safely call it AccountState, can't we?
 */
public class PoiAccountState {
	private static final int REMOTE_STATE_SIZE = 2;

	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private BlockHeight height;

	// I thought we're gonna need 3 elements, but it would work with 2.
	//    1. let's say there is alias created at block 1000, it becomes effective at block 2440
	//    2. now at block 2500 user removes alias, removal will become effective at block 3940
	//    3. user won't be able to create new alias before 3940, so there is no need, for this to have 3 elements
	//        as eventual (blockchain) rollback won't change anything, so I'm changing REMOTE_STATE_SIZE to 2
	private CircularStack<RemoteState> remoteStateStack;

	/**
	 * Creates a new NIS account state.
	 */
	public PoiAccountState(final Address address) {
		this(address, new AccountImportance(), new WeightedBalances(), null);
	}

	private PoiAccountState(
			final Address address,
			final AccountImportance importance,
			final WeightedBalances weightedBalances,
			final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.weightedBalances = weightedBalances;
		this.height = height;

		this.remoteStateStack = new CircularStack<>(REMOTE_STATE_SIZE);
	}

	/**
	 * Gets the account address.
	 *
	 * @return The account address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the weighted balances.
	 *
	 * @return The weighted balances.
	 */
	public WeightedBalances getWeightedBalances() {
		return this.weightedBalances;
	}

	/**
	 * Gets the importance information.
	 *
	 * @return The importance information.
	 */
	public AccountImportance getImportanceInfo() {
		return this.importance;
	}

	/**
	 * Returns height of an account.
	 *
	 * @return The height of an account - when the account has been created.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Sets height of an account if the account does not already have a height.
	 *
	 * @param height The height.
	 */
	public void setHeight(final BlockHeight height) {
		if (null == this.height) {
			this.height = height;
		}
	}

	/**
	 * Creates association between "remote" account and "owner" account.
	 *  @param address Address of an owner account.
	 * @param height Height where association was created.
	 * @param direction
	 */
	public void remoteFor(final Address address, final BlockHeight height, int direction) {
		this.remoteStateStack.add(new RemoteState(address, height, direction, true));
	}

	/**
	 * Creates association between "current" (owner) account and "remote" account.
	 *  @param address Address of remote account.
	 * @param height Height where association was created.
	 * @param direction Direction of an association.
	 */
	public void setRemote(final Address address, final BlockHeight height, final int direction) {
		this.remoteStateStack.add(new RemoteState(address, height, direction, false));
	}

	/**
	 * Removes association between "owner" and "remote".
	 *
	 * @param address Address of the account of other side of association.
	 * @param height Height where association was created.
	 * @param direction Direction of an association.
	 */
	public void resetRemote(final Address address, final BlockHeight height, final int direction) {
		// between changes of remoteState there must be 1440 blocks
		final Address rAddr = this.remoteStateStack.get().getRemoteAddress();
		if (this.remoteStateStack.get().getDirection() != direction ||
				(rAddr != address && (rAddr == null || !rAddr.equals(address))) ||
				!this.remoteStateStack.get().getRemoteHeight().equals(height)) {
			throw new IllegalArgumentException("call to resetRemote must be 'paired' with call to remoteFor or setRemote");
		}
		this.remoteStateStack.remove();
	}

	/**
	 * Gets state for remote.
	 *
	 * @return Remote state if account have one.
	 */
	public RemoteState getRemoteState() {
		return this.remoteStateStack.get();
	}

	public boolean hasRemoteState() {
		return this.remoteStateStack.size() != 0;
	}

	public boolean hasRemote() {
		return this.remoteStateStack.size() != 0 && this.getRemoteState().hasRemote();
	}

	/**
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		final PoiAccountState ret = new PoiAccountState(this.address, this.importance.copy(), this.weightedBalances.copy(), this.height);
		this.remoteStateStack.shallowCopyTo(ret.remoteStateStack);
		return ret;
	}
}