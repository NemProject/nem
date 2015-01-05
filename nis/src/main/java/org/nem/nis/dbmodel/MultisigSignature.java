package org.nem.nis.dbmodel;

import javax.persistence.*;

@Entity
@Table(name = "multisigsignatures")
public class MultisigSignature extends AbstractTransfer {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "multisigTransactionId")
	private MultisigTransaction multisigTransaction;

	public MultisigTransaction getMultisigTransaction() {
		return this.multisigTransaction;
	}

	public void setMultisigTransaction(final MultisigTransaction multisigTransaction) {
		this.multisigTransaction = multisigTransaction;
	}
}
