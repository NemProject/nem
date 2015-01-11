package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.*;

/**
 * A collection of multisig information associated with an account.
 */
public class MultisigLinks implements ReadOnlyMultisigLinks {
	private final Set<Address> cosignatories = new HashSet<>();
	private final Set<Address> cosignatoryOf = new HashSet<>();

	/**
	 * Adds a cosignatory link.
	 *
	 * @param cosignatory The address of the account that is a cosignatory of this (multisig) account.
	 */
	public void addCosignatory(final Address cosignatory) {
		this.cosignatories.add(cosignatory);
	}

	/**
	 * Adds a multisig link.
	 *
	 * @param multisig The address of the multisig account for which this account is a cosignatory.
	 */
	public void addCosignatoryOf(final Address multisig) {
		this.cosignatoryOf.add(multisig);
	}

	/**
	 * Removes a cosignatory link.
	 *
	 * @param cosignatory The cosignatory to remove.
	 */
	public void removeCosignatory(final Address cosignatory) {
		this.cosignatories.remove(cosignatory);
	}

	/**
	 * Removes a multisig link.
	 *
	 * @param multisig The multisig to remove.
	 */
	public void removeCosignatoryOf(final Address multisig) {
		this.cosignatoryOf.remove(multisig);
	}

	@Override
	public Collection<Address> getCosignatories() {
		return Collections.unmodifiableSet(this.cosignatories);
	}

	@Override
	public Collection<Address> getCosignatoryOf() {
		return Collections.unmodifiableSet(cosignatoryOf);
	}

	@Override
	public boolean isMultisig() {
		return !this.cosignatories.isEmpty();
	}

	@Override
	public boolean isCosignatory() {
		return !this.cosignatoryOf.isEmpty();
	}

	@Override
	public boolean isCosignatoryOf(final Address multisig) {
		return this.cosignatoryOf.stream().anyMatch(a -> a.equals(multisig));
	}

	/**
	 * Creates a deep copy of the multisig links.
	 *
	 * @return The deep copy.
	 */
	public MultisigLinks copy() {
		final MultisigLinks multisigLinks = new MultisigLinks();
		this.cosignatories.forEach(v -> multisigLinks.addCosignatory(v));
		this.cosignatoryOf.forEach(v -> multisigLinks.addCosignatoryOf(v));
		return multisigLinks;
	}
}
