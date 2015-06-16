package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Provision namespace db entity
 * <br>
 * Holds information about a single provision namespace transaction.
 */
@Entity
@Table(name = "namespaceprovisions")
public class DbProvisionNamespaceTransaction extends AbstractBlockTransfer<DbProvisionNamespaceTransaction> {
	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "namespaceId")
	private DbNamespace namespace;

	public DbProvisionNamespaceTransaction() {
		super(DbBlock::getBlockProvisionNamespaceTransactions);
	}

	public DbNamespace getNamespace() {
		return this.namespace;
	}

	public void setNamespace(final DbNamespace namespace) {
		this.namespace = namespace;
	}
}
