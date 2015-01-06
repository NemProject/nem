package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.Set;

/**
 * Container of readonly multisig state information.
 */
public interface ReadOnlyMultisigLinks {
	/**
	 * Gets the addresses of all cosignatories.
	 * TODO 20150103 J-G: can we return collection; actually shouldn't return that either since someone could add to them
	 * > unless the impl makes a copy of the addresses, probably the better solution
	 *
	 * @return The addresses of cosignatories.
	 */
	public Set<Address> getCosignatories();

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
