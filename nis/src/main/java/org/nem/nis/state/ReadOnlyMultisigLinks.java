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
	public Collection<Address> getCosignatories();

	/**
	 * Gets the addresses of all accounts of which this account is a cosignatory.
	 *
	 * @return The addresses of all accounts that this account can cosign.
	 */
	public Collection<Address> getCosignatoryOf();

	/**
	 * Checks if account is a multisig account.
	 *
	 * @return true if account is multisig account, false otherwise.
	 */
	public boolean isMultisig();

	/**
	 * Checks if account is cosignatory of any account.
	 *
	 * @return true if account is cosignatory, false otherwise.
	 */
	public boolean isCosignatory();

	/**
	 * Checks if account is cosignatory of given account.
	 *
	 * @param multisig address of other account.
	 * @return true in account is cosignatory of multisig, false otherwise.
	 */
	public boolean isCosignatoryOf(final Address multisig);
}
