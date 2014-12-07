package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * Multisig Signer Modification db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.MULTISIG_SIGNER_MODIFY
 * <p>
 */
@Entity
@Table(name = "multisigsignermodifications")
public class MultisigSignerModification extends AbstractTransfer<MultisigSignerModification> {
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigSignerModification", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<MultisigModification> multisigModifications;

	public MultisigSignerModification() {
		super(b -> b.getBlockMultisigSignerModifications());
	}

	public Set<MultisigModification> getMultisigModifications() {
		return multisigModifications;
	}

	public void setMultisigModifications(final Set<MultisigModification> multisigModifications) {
		this.multisigModifications = multisigModifications;
	}
}
