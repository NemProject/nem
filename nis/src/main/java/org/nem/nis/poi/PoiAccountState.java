package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.remote.RemoteLinks;
import org.nem.nis.secret.*;

/**
 * Class containing extrinsic in-memory mutable account information.
 * TODO 20141209 J-*: really, really need to rename this class to AccountState!!!
 */
public class PoiAccountState {
	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private final RemoteLinks remoteLinks;
	private final AccountInfo accountInfo;
	private BlockHeight height;

	/**
	 * Creates a new NIS account state.
	 */
	public PoiAccountState(final Address address) {
		this(address, new AccountImportance(), new WeightedBalances(), new RemoteLinks(), new AccountInfo(), null);
	}

	private PoiAccountState(
			final Address address,
			final AccountImportance importance,
			final WeightedBalances weightedBalances,
			final RemoteLinks remoteLinks,
			final AccountInfo accountInfo,
			final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.weightedBalances = weightedBalances;
		this.remoteLinks = remoteLinks;
		this.accountInfo = accountInfo;
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
	 * Gets the remote link information.
	 *
	 * @return The remote link information.
	 */
	public RemoteLinks getRemoteLinks() {
		return this.remoteLinks;
	}

	/**
	 * Gets the account info.
	 *
	 * @return The account info.
	 */
	public AccountInfo getAccountInfo() {
		return this.accountInfo;
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
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		return new PoiAccountState(
				this.address,
				this.importance.copy(),
				this.weightedBalances.copy(),
				this.remoteLinks.copy(),
				this.accountInfo.copy(),
				this.height);
	}
}