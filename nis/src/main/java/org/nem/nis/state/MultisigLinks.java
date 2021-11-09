package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.*;

/**
 * A collection of multisig information associated with an account.
 */
public class MultisigLinks implements ReadOnlyMultisigLinks {
	private final Set<Address> cosignatories = new HashSet<>();
	private final Set<Address> cosignatoryOf = new HashSet<>();
	private int minCosignatories = 0;

	/**
	 * Adds a cosignatory link.
	 *
	 * @param cosignatory The address of the account that is a cosignatory of this (multisig) account.
	 */
	public void addCosignatory(final Address cosignatory) {
		if (this.isCosignatory()) {
			throw new IllegalArgumentException("cannot add cosignatory to cosigning account");
		}

		this.cosignatories.add(cosignatory);
	}

	/**
	 * Adds a multisig link.
	 *
	 * @param multisig The address of the multisig account for which this account is a cosignatory.
	 */
	public void addCosignatoryOf(final Address multisig) {
		if (this.isMultisig()) {
			throw new IllegalArgumentException("cannot add cosignatory-of to multisig account");
		}

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
	 * Sets the minimum number of cosignatories needed to complete a multisig transaction.
	 *
	 * @param value The value by which to increment the minimum number of cosignatories. Can be negative.
	 */
	public void incrementMinCosignatoriesBy(final int value) {
		final int minCosignatories = this.minCosignatories + value;
		if (minCosignatories < 0) {
			throw new IllegalArgumentException("min cosignatories out of range");
		}

		this.minCosignatories = minCosignatories;
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
	public Collection<Address> getCosignatoriesOf() {
		return Collections.unmodifiableSet(this.cosignatoryOf);
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

	@Override
	public int minCosignatories() {
		return this.minCosignatories;
	}

	/**
	 * Creates a deep copy of the multisig links.
	 *
	 * @return The deep copy.
	 */
	public MultisigLinks copy() {
		final MultisigLinks multisigLinks = new MultisigLinks();
		this.cosignatories.forEach(multisigLinks::addCosignatory);
		this.cosignatoryOf.forEach(multisigLinks::addCosignatoryOf);
		multisigLinks.minCosignatories = this.minCosignatories;
		return multisigLinks;
	}
}
