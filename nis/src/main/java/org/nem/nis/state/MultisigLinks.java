package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

public class MultisigLinks implements ReadOnlyMultisigLinks {
	private final Map<Address, BlockHeight> cosignatories;
	private final Map<Address, BlockHeight> cosignatoryOf;

	public MultisigLinks() {
		this.cosignatories = new HashMap<>();
		this.cosignatoryOf = new HashMap<>();
	}

	public void addCosignatory(final Address cosignatory, final BlockHeight height) {
		this.cosignatories.put(cosignatory, height);
	}

	public void addMultisig(final Address multisigAddress, final BlockHeight height) {
		this.cosignatoryOf.put(multisigAddress, height);
	}

	// TODO: should this verify height?
	public void removeCosignatory(final Address cosignatory, final BlockHeight height) {
		this.cosignatories.remove(cosignatory);
	}

	// TODO: should this verify height?
	public void removeMultisig(final Address multisigAddress, final BlockHeight height) {
		this.cosignatoryOf.remove(multisigAddress);
	}

	public Set<Address> getCosignatories() {
		return this.cosignatories.keySet();
	}
	
	public boolean isMultisig() {
		return 0 != this.cosignatories.size();
	}

	public boolean isCosignatory() {
		return 0 != this.cosignatoryOf.size();
	}

	public boolean isCosignatoryOf(final Address signer) {
		return this.cosignatoryOf.keySet().stream().anyMatch(a -> a.equals(signer));
	}

	public MultisigLinks copy() {
		final MultisigLinks multisigLinks = new MultisigLinks();
		this.cosignatories.forEach((k, v) -> multisigLinks.addCosignatory(k, v));
		this.cosignatoryOf.forEach((k, v) -> multisigLinks.addMultisig(k, v));
		return multisigLinks;
	}
}
