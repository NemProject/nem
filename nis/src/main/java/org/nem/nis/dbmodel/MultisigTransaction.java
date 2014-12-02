package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * TODO 20141129 G - J, BR : @J I was (again) thinking for quite a long, to map
 * our model inheritance in DB (probably using "table per subclass", I went through some blogs
 * this probably is the closest one:
 * http://chriswongdevblog.blogspot.fr/2009/10/polymorphic-one-to-many-relationships.html
 *
 * basing on above and some other reading, it doesn't really make sense :/
 *
 * (That's also why I can't have single field that would be correctly joined :/)
 * I hoped I'll have members
 *
 * TODO 20141201 J-G: what issues did you have with "table per subclass"
 */

@Entity
@Table(name = "multisigtransactions")
public class MultisigTransaction  extends AbstractTransfer<MultisigTransaction> {
	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "transferId")
	private Transfer transfer;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "importanceTransferId")
	private ImportanceTransfer importanceTransfer;

	@OneToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "multisigSignerModificationId")
	private MultisigSignerModification multisigSignerModification;

	public MultisigTransaction() {
		super(b -> b.getBlockMultisigTransactions());
	}


	public Transfer getTransfer() {
		return transfer;
	}

	public void setTransfer(final Transfer transfer) {
		this.transfer = transfer;
	}

	public ImportanceTransfer getImportanceTransfer() {
		return importanceTransfer;
	}

	public void setImportanceTransfer(final ImportanceTransfer importanceTransfer) {
		this.importanceTransfer = importanceTransfer;
	}

	public MultisigSignerModification getMultisigSignerModification() {
		return multisigSignerModification;
	}

	public void setMultisigSignerModification(final MultisigSignerModification multisigSignerModification) {
		this.multisigSignerModification = multisigSignerModification;
	}
}
