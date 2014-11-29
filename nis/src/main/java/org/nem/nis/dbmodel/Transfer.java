package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;

/**
 * Transfer Db entity.
 * <p>
 * Holds information about Transactions having type TransactionTypes.TRANSFER_TYPE
 * <p>
 * Associated sender and recipient are obtained automatically (by TransferDao)
 * thanks to @Cascade annotations.
 */
@Entity
@Table(name = "transfers")
public class Transfer extends AbstractTransfer<Transfer> {
	@OneToOne(fetch = FetchType.EAGER, optional = true, mappedBy = "transfer")
	private MultisigTransaction multisigTransactionTransfer;

	@ManyToOne
	@Cascade({ CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "recipientId")
	private Account recipient;

	private Long amount;

	private Integer messageType;
	private byte[] messagePayload;

	public Transfer() {
		super(b -> b.getBlockTransfers());
	}

	public Account getRecipient() {
		return this.recipient;
	}

	public void setRecipient(final Account recipient) {
		this.recipient = recipient;
	}

	public Long getAmount() {
		return this.amount;
	}

	public void setAmount(final Long amount) {
		this.amount = amount;
	}

	public Integer getMessageType() {
		return this.messageType;
	}

	public void setMessageType(final Integer messageType) {
		this.messageType = messageType;
	}

	public byte[] getMessagePayload() {
		return this.messagePayload;
	}

	public void setMessagePayload(final byte[] messagePayload) {
		this.messagePayload = messagePayload;
	}

	/* == */
	public MultisigTransaction getMultisigTransactionTransfer() {
		return multisigTransactionTransfer;
	}

	public void setMultisigTransactionTransfer(MultisigTransaction multisigTransactionTransfer) {
		this.multisigTransactionTransfer = multisigTransactionTransfer;
	}
}
