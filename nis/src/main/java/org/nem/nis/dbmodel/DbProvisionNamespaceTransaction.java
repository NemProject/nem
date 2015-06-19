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

	public Long getRentalFee() {
		return this.rentalFee;
	}

	public DbNamespace getNamespace() {
		return this.namespace;
	}

	public void setNamespace(final DbNamespace namespace) {
		this.namespace = namespace;
	}
}
