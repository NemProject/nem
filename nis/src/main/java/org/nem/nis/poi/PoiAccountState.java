package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.secret.*;

/**
 * Class containing extrinsic NIS-account information that is used to calculate POI.
 * TODO-CR 20140808 J->ALL i think the naming is confusing between PoiAccountState and PoiAccountInfo ... should try to think of better names
 * G->J, I think we can safely call it AccountState, can't we?
 */
public class PoiAccountState {
	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private BlockHeight height;

	private RemoteState remoteState;
	private RemoteState previousState;

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
	 *
	 * @param address Address of an owner account.
	 * @param height Height where association was created.
	 */
	public void remoteFor(final Address address, final BlockHeight height) {
		this.previousState = this.remoteState;
		this.remoteState = new RemoteState(address, height, true);
	}

	/**
	 * Creates association between "current" (owner) account and "remote" account.
	 *
	 * @param address Address of remote account.
	 * @param height Height where association was created.
	 */
	public void setRemote(final Address address, final BlockHeight height) {
		this.previousState = this.remoteState;
		this.remoteState = new RemoteState(address,height, false);
	}

	/**
	 * Removes association between "owner" and "remote".
	 */
	public void resetRemote() {
		// We can do it this way, as there must be 1440 blocks between change of state.
		this.remoteState = this.previousState;
		this.previousState = null;
	}

	/**
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		final PoiAccountState ret = new PoiAccountState(this.address, this.importance.copy(), this.weightedBalances.copy(), this.height);
		// no need to copy
		ret.previousState = this.previousState;
		ret.remoteState = this.remoteState;
		return ret;
	}
}