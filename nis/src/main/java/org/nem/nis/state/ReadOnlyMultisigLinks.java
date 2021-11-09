package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.Collection;

/**
 * Container of readonly multisig state information.
 */
public interface ReadOnlyMultisigLinks {

	/**
	 * Gets the addresses of all cosignatories.
	 *
	 * @return The addresses of cosignatories.
	 */
	Collection<Address> getCosignatories();

	/**
	 * Gets the addresses of all accounts for which this account is a cosignatory.
	 *
	 * @return The addresses of all accounts that this account can cosign.
	 */
	Collection<Address> getCosignatoriesOf();

	/**
	 * Checks if account is a multisig account.
	 *
	 * @return true if account is multisig account, false otherwise.
	 */
	boolean isMultisig();

	/**
	 * Checks if account is cosignatory of any account.
	 *
	 * @return true if account is cosignatory, false otherwise.
	 */
	boolean isCosignatory();

	/**
	 * Checks if account is cosignatory of given account.
	 *
	 * @param multisig address of other account.
	 * @return true in account is cosignatory of multisig, false otherwise.
	 */
	boolean isCosignatoryOf(final Address multisig);

	/**
	 * Gets the minimum number of cosignatories needed to complete a multisig transaction.
	 *
	 * @return The minimum number of cosignatories.
	 */
	int minCosignatories();
}
