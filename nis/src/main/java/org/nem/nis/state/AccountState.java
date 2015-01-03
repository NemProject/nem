package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

// TODO 20150103 J-G: please update the account state tests

/**
 * Class containing extrinsic in-memory mutable account information.
 */
public class AccountState implements ReadOnlyAccountState {
	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private final RemoteLinks remoteLinks;
	private final MultisigLinks multisigLinks;
	private final AccountInfo accountInfo;
	private BlockHeight height;

	/**
	 * Creates a new NIS account state.
	 */
	public AccountState(final Address address) {
		this(address, new AccountImportance(), new WeightedBalances(), new RemoteLinks(), new MultisigLinks(), new AccountInfo(), null);
	}

	private AccountState(
			final Address address,
			final AccountImportance importance,
			final WeightedBalances weightedBalances,
			final RemoteLinks remoteLinks,
			final MultisigLinks multisigLinks,
			final AccountInfo accountInfo,
			final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.weightedBalances = weightedBalances;
		this.remoteLinks = remoteLinks;
		this.multisigLinks = multisigLinks;
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
	 * Gets multisig link information.
	 *
	 * @return The multisig link information.
	 */
	public MultisigLinks getMultisigLinks() {
		return this.multisigLinks;
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
	public AccountState copy() {
		return new AccountState(
				this.address,
				this.importance.copy(),
				this.weightedBalances.copy(),
				this.remoteLinks.copy(),
				this.multisigLinks.copy(),
				this.accountInfo.copy(),
				this.height);
	}
}