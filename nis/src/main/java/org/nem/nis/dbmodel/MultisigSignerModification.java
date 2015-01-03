package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.Set;

/**
 * Multisig Signer Modification db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.MULTISIG_SIGNER_MODIFY
 * <p>
 */
@Entity
@Table(name = "multisigsignermodifications")
public class MultisigSignerModification extends AbstractBlockTransfer<MultisigSignerModification> {
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigSignerModification", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<MultisigModification> multisigModifications;

	public MultisigSignerModification() {
		super(b -> b.getBlockMultisigSignerModifications());
	}

	public Set<MultisigModification> getMultisigModifications() {
		return this.multisigModifications;
	}

	public void setMultisigModifications(final Set<MultisigModification> multisigModifications) {
		this.multisigModifications = multisigModifications;
	}
}
