package org.nem.nis.state;

import org.nem.core.model.Address;

import java.util.Set;

public interface ReadOnlyMultisigLinks {
	public Set<Address> getCosignatories();

	public boolean isMultisig();

	public boolean isCosignatory();

	public boolean isCosignatoryOf(final Address signer);
}
