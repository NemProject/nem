package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A read-only view of an account's state.
 */
@SuppressWarnings("unused")
public interface ReadOnlyAccountState {

	/**
	 * Gets the account address.
	 *
	 * @return The account address.
	 */
	Address getAddress();

	/**
	 * Gets the weighted balances.
	 *
	 * @return The weighted balances.
	 */
	ReadOnlyWeightedBalances getWeightedBalances();

	/**
	 * Gets the historical importances.
	 *
	 * @return The historical importances.
	 */
	ReadOnlyHistoricalImportances getHistoricalImportances();

	/**
	 * Gets the importance information.
	 *
	 * @return The importance information.
	 */
	ReadOnlyAccountImportance getImportanceInfo();

	/**
	 * Gets the remote link information.
	 *
	 * @return The remote link information.
	 */
	ReadOnlyRemoteLinks getRemoteLinks();

	/**
	 * Gets multisig link information.
	 *
	 * @return The multisig link information.
	 */
	ReadOnlyMultisigLinks getMultisigLinks();

	/**
	 * Gets the account info.
	 *
	 * @return The account info.
	 */
	ReadOnlyAccountInfo getAccountInfo();

	/**
	 * Returns height of an account.
	 *
	 * @return The height of an account - when the account has been created.
	 */
	BlockHeight getHeight();
}
