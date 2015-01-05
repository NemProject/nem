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
public class MultisigTransaction extends AbstractBlockTransfer<MultisigTransaction> {
	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "transferId")
	private Transfer transfer;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "importanceTransferId")
	private ImportanceTransfer importanceTransfer;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "multisigSignerModificationId")
	private MultisigSignerModification multisigSignerModification;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "multisigTransaction", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<MultisigSignature> multisigSignatures;

	public MultisigTransaction() {
		super(b -> b.getBlockMultisigTransactions());
	}

	public Transfer getTransfer() {
		return this.transfer;
	}

	public void setTransfer(final Transfer transfer) {
		this.transfer = transfer;
	}

	public ImportanceTransfer getImportanceTransfer() {
		return this.importanceTransfer;
	}

	public void setImportanceTransfer(final ImportanceTransfer importanceTransfer) {
		this.importanceTransfer = importanceTransfer;
	}

	public MultisigSignerModification getMultisigSignerModification() {
		return this.multisigSignerModification;
	}

	public void setMultisigSignerModification(final MultisigSignerModification multisigSignerModification) {
		this.multisigSignerModification = multisigSignerModification;
	}

	public Set<MultisigSignature> getMultisigSignatures() {
		return this.multisigSignatures;
	}

	public void setMultisigSignatures(final Set<MultisigSignature> multisigSignatures) {
		this.multisigSignatures = multisigSignatures;
	}
}
