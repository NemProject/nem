package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.delta.Copyable;

/**
 * Class containing extrinsic in-memory mutable account information.
 */
public class AccountState implements ReadOnlyAccountState, Copyable<AccountState> {
	private final Address address;
	private final AccountImportance importance;
	private final HistoricalImportances historicalImportances;
	private final WeightedBalances weightedBalances;
	private final RemoteLinks remoteLinks;
	private final MultisigLinks multisigLinks;
	private final AccountInfo accountInfo;
	private BlockHeight height;

	/**
	 * Creates a new NIS account state.
	 *
	 * @param address The address of an account.
	 */
	public AccountState(final Address address) {
		this(address, new AccountImportance(), new HistoricalImportances(), NemStateGlobals.createWeightedBalances(), new RemoteLinks(),
				new MultisigLinks(), new AccountInfo(), null);
	}

	private AccountState(final Address address, final AccountImportance importance, final HistoricalImportances historicalImportances,
			final WeightedBalances weightedBalances, final RemoteLinks remoteLinks, final MultisigLinks multisigLinks,
			final AccountInfo accountInfo, final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.historicalImportances = historicalImportances;
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
	 * Gets the historical importances.
	 *
	 * @return The historical importances.
	 */
	public HistoricalImportances getHistoricalImportances() {
		return this.historicalImportances;
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

	@Override
	public AccountState copy() {
		return new AccountState(this.address, this.importance.copy(), this.historicalImportances.copy(), this.weightedBalances.copy(),
				this.remoteLinks.copy(), this.multisigLinks.copy(), this.accountInfo.copy(), this.height);
	}
}
