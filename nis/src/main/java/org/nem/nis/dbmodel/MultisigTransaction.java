package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.Set;

/**
 * TODO 20141129 G - J, BR : @J I was (again) thinking for quite a long, to map
 * our model inheritance in DB (probably using "table per subclass", I went through some blogs
 * this probably is the closest one:
 * http://chriswongdevblog.blogspot.fr/2009/10/polymorphic-one-to-many-relationships.html
 * basing on above and some other reading, it doesn't really make sense :/
 * (That's also why I can't have single field that would be correctly joined :/)
 * I hoped I'll have members
 * TODO 20141201 J-G: what issues did you have with "table per subclass"
 * TODO 20141202 G-J: I actually haven't tried it,
 * a) it would require serious changes (but I was actually ready to do them), but
 * b) take a look at the link above... especially this sentence:
 * " it retrieves the union of all properties in the entire hierarchy into the result set. "
 * TODO 20150103 J-G: i guess clean up this comments and remove the todos.
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
