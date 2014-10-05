package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;
import org.nem.core.crypto.Hash;

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
public class Transfer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long shortId;
	private byte[] transferHash;

	private Integer version;
	private Integer type;
	private Long fee;
	private Integer timeStamp;
	private Integer deadline;

	@ManyToOne
	@Cascade({ CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "senderId")
	private Account sender;
	private byte[] senderProof;

	@ManyToOne
	@Cascade({ CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "recipientId")
	private Account recipient;

	private Integer blkIndex; // blkIndex inside block

	private Long amount;
	private Long referencedTransaction;

	private Integer messageType;
	private byte[] messagePayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blockId")
	private Block block;

	public Transfer() {
	}

	public Transfer(
			final Hash hash,
			final Integer version,
			final Integer type,
			final Long fee,
			final Integer timeStamp,
			final Integer deadline,
			final Account sender,
			final byte[] senderProof,
			final Account recipient,
			final Integer blkIndex,
			final Long amount,
			final Long referencedTransaction
	) {
		this.shortId = hash.getShortId();
		this.transferHash = hash.getRaw();
		this.version = version;
		this.type = type;
		this.fee = fee;
		this.timeStamp = timeStamp;
		this.deadline = deadline;
		this.sender = sender;
		this.senderProof = senderProof;
		this.recipient = recipient;
		this.blkIndex = blkIndex;
		this.amount = amount;
		this.referencedTransaction = referencedTransaction;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public Long getShortId() {
		return this.shortId;
	}

	public void setShortId(final Long shortId) {
		this.shortId = shortId;
	}

	public Hash getTransferHash() {
		return new Hash(this.transferHash);
	}

	public void setTransferHash(final byte[] transferHash) {
		this.transferHash = transferHash;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
	}

	public Integer getType() {
		return this.type;
	}

	public void setType(final Integer type) {
		this.type = type;
	}

	public Long getFee() {
		return this.fee;
	}

	public void setFee(final Long fee) {
		this.fee = fee;
	}

	public Integer getTimeStamp() {
		return this.timeStamp;
	}

	public void setTimeStamp(final Integer timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Integer getDeadline() {
		return this.deadline;
	}

	public void setDeadline(final Integer deadline) {
		this.deadline = deadline;
	}

	public Account getSender() {
		return this.sender;
	}

	public void setSender(final Account sender) {
		this.sender = sender;
	}

	public byte[] getSenderProof() {
		return this.senderProof;
	}

	public void setSenderProof(final byte[] senderProof) {
		this.senderProof = senderProof;
	}

	public Account getRecipient() {
		return this.recipient;
	}

	public void setRecipient(final Account recipient) {
		this.recipient = recipient;
	}

	public Integer getBlkIndex() {
		return this.blkIndex;
	}

	public void setBlkIndex(final Integer blkIndex) {
		this.blkIndex = blkIndex;
	}

	public Long getAmount() {
		return this.amount;
	}

	public void setAmount(final Long amount) {
		this.amount = amount;
	}

	public Long getReferencedTransaction() {
		return this.referencedTransaction;
	}

	public void setReferencedTransaction(final Long referencedTransaction) {
		this.referencedTransaction = referencedTransaction;
	}

	public Block getBlock() {
		return this.block;
	}

	public void setBlock(final Block block) {
		this.block = block;
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
}
