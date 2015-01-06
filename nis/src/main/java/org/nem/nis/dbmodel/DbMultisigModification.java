package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Multisig Modification db entity
 * <p>
 * Holds information about single multisig modification
 * <p>
 */
@Entity
@Table(name = "multisigmodifications")
public class DbMultisigModification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "cosignatoryId")
	private DbAccount cosignatory;

	private Integer modificationType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "multisigSignerModificationId")
	private DbMultisigAggregateModificationTransaction multisigAggregateModificationTransaction;

	public DbMultisigAggregateModificationTransaction getMultisigAggregateModificationTransaction() {
		return this.multisigAggregateModificationTransaction;
	}

	public void setMultisigAggregateModificationTransaction(final DbMultisigAggregateModificationTransaction multisigAggregateModificationTransaction) {
		this.multisigAggregateModificationTransaction = multisigAggregateModificationTransaction;
	}

	public DbAccount getCosignatory() {
		return this.cosignatory;
	}

	public void setCosignatory(final DbAccount cosignatory) {
		this.cosignatory = cosignatory;
	}

	public Integer getModificationType() {
		return this.modificationType;
	}

	public void setModificationType(final Integer modificationType) {
		this.modificationType = modificationType;
	}
}
