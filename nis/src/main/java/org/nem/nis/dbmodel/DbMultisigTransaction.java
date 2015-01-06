package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.Set;

/**
 * It is unfortunate that we couldn't use "table per subclass", which would allow this class to have a single
 * transferId field that could be correctly joined across multiple tables.
 * <br />
 * The reason is that db performance could be bad because of the way it is implemented. Specifically,
 * "it retrieves the union of all properties in the entire hierarchy into the result set"
 * - http://chriswongdevblog.blogspot.fr/2009/10/polymorphic-one-to-many-relationships.html
 */

@Entity
@Table(name = "multisigtransactions")
public class DbMultisigTransaction extends AbstractBlockTransfer<DbMultisigTransaction> {
	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "transferId")
	private DbTransferTransaction transferTransaction;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "importanceTransferId")
	private ImportanceTransfer importanceTransfer;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "multisigSignerModificationId")
	private DbMultisigAggregateModificationTransaction multisigAggregateModificationTransaction;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigTransaction", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<DbMultisigSignatureTransaction> multisigSignatureTransactions;

	public DbMultisigTransaction() {
		super(b -> b.getBlockMultisigTransactions());
	}

	public DbTransferTransaction getTransferTransaction() {
		return this.transferTransaction;
	}

	public void setTransferTransaction(final DbTransferTransaction transferTransaction) {
		this.transferTransaction = transferTransaction;
	}

	public ImportanceTransfer getImportanceTransfer() {
		return this.importanceTransfer;
	}

	public void setImportanceTransfer(final ImportanceTransfer importanceTransfer) {
		this.importanceTransfer = importanceTransfer;
	}

	public DbMultisigAggregateModificationTransaction getMultisigAggregateModificationTransaction() {
		return this.multisigAggregateModificationTransaction;
	}

	public void setMultisigAggregateModificationTransaction(final DbMultisigAggregateModificationTransaction multisigAggregateModificationTransaction) {
		this.multisigAggregateModificationTransaction = multisigAggregateModificationTransaction;
	}

	public Set<DbMultisigSignatureTransaction> getMultisigSignatureTransactions() {
		return this.multisigSignatureTransactions;
	}

	public void setMultisigSignatureTransactions(final Set<DbMultisigSignatureTransaction> multisigSignatureTransactions) {
		this.multisigSignatureTransactions = multisigSignatureTransactions;
	}
}
