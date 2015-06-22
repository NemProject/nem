package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Provision namespace db entity
 * <br>
 * Holds information about a single provision namespace transaction.
 */
@Entity
@Table(name = "namespaceprovisions")
public class DbProvisionNamespaceTransaction extends AbstractBlockTransfer<DbProvisionNamespaceTransaction> {
	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "lessorId")
	private DbAccount lessor;

	private Long rentalFee;

	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "namespaceId")
	private DbNamespace namespace;

	public DbProvisionNamespaceTransaction() {
		super(DbBlock::getBlockProvisionNamespaceTransactions);
	}

	public DbAccount getLessor() {
		return this.lessor;
	}

	public void setLessor(final DbAccount lessor) {
		this.lessor = lessor;
	}

	public Long getRentalFee() {
		return this.rentalFee;
	}

	public void setRentalFee(final Long rentalFee) {
		this.rentalFee = rentalFee;
	}

	public DbNamespace getNamespace() {
		return this.namespace;
	}

	public void setNamespace(final DbNamespace namespace) {
		this.namespace = namespace;
	}

	public void setBlock(final DbBlock dbBlock) {
		super.setBlock(dbBlock);

		// don't require the namespace to be set before the block; in some cases, the namespace
		// will have full information (e.g. when mapping from raw)
		if (null != this.namespace) {
			this.namespace.setHeight(dbBlock.getHeight());
		}
	}
}
