package org.nem.nis.dbmodel;

import javax.persistence.*;

@Entity
@Table(name = "multisigsignatures")
public class DbMultisigSignatureTransaction extends AbstractTransfer {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "multisigTransactionId")
	private DbMultisigTransaction multisigTransaction;

	public DbMultisigTransaction getMultisigTransaction() {
		return this.multisigTransaction;
	}

	public void setMultisigTransaction(final DbMultisigTransaction multisigTransaction) {
		this.multisigTransaction = multisigTransaction;
	}
}
