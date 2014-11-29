package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Multisig Signer Modification db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.MULTISIG_SIGNER_MODIFY
 * <p>
 */
@Entity
@Table(name = "multisigsignermodifications")
public class MultisigSignerModification extends AbstractTransfer<MultisigSignerModification> {
	@OneToOne(fetch = FetchType.EAGER, optional = true, mappedBy = "multisigSignerModification")
	private MultisigTransaction multisigTransactionMultisigSignerModification;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "cosignatoryId")
	private Account cosignatory;

	private Integer modificationType;

	public MultisigSignerModification() {
		super(b -> b.getBlockMultisigSignerModifications());
	}

	public Account getCosignatory() {
		return this.cosignatory;
	}

	public void setCosignatory(final Account cosignatory) {
		this.cosignatory = cosignatory;
	}

	public Integer getModificationType() {
		return this.modificationType;
	}

	public void setModificationType(final Integer modificationType) {
		this.modificationType = modificationType;
	}

	/* == */
	public MultisigTransaction getMultisigTransactionMultisigSignerModification() {
		return multisigTransactionMultisigSignerModification;
	}

	public void setMultisigTransactionMultisigSignerModification(MultisigTransaction multisigTransactionMultisigSignerModification) {
		this.multisigTransactionMultisigSignerModification = multisigTransactionMultisigSignerModification;
	}
}
