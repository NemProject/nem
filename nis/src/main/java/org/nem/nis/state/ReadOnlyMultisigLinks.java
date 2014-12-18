package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.Set;

// TODO 20141218 J-G: needs comments

public interface ReadOnlyMultisigLinks {
	public Set<Address> getCosignatories();

	public boolean isMultisig();

	public boolean isCosignatory();

	public boolean isCosignatoryOf(final Address signer);
}
