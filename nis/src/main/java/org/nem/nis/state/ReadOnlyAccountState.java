package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A read-only view of an account's state.
 */
public interface ReadOnlyAccountState {

	/**
	 * Gets the account address.
	 *
	 * @return The account address.
	 */
	public Address getAddress();

	/**
	 * Gets the weighted balances.
	 *
	 * @return The weighted balances.
	 */
	public ReadOnlyWeightedBalances getWeightedBalances();

	/**
	 * Gets the importance information.
	 *
	 * @return The importance information.
	 */
	public ReadOnlyAccountImportance getImportanceInfo();

	/**
	 * Gets the remote link information.
	 *
	 * @return The remote link information.
	 */
	public ReadOnlyRemoteLinks getRemoteLinks();

	/**
	 * Gets multisig link information.
	 *
	 * @return The multisig link information.
	 */
	public ReadOnlyMultisigLinks getMultisigLinks();

	/**
	 * Gets the account info.
	 *
	 * @return The account info.
	 */
	public ReadOnlyAccountInfo getAccountInfo();

	/**
	 * Returns height of an account.
	 *
	 * @return The height of an account - when the account has been created.
	 */
	public BlockHeight getHeight();
}
