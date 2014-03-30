package org.nem.core.dbmodel;

import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity  
@Table(name="transfers") 
public class Transfer {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	private Long shortId;
	private byte[] transferHash;
	
	private Integer version;
	private Integer type;
	private Long fee;
	private Integer timestamp;
	private Integer deadline;

	@ManyToOne
	@Cascade({CascadeType.SAVE_UPDATE})
	@JoinColumn(name="senderId")
	private Account sender;
	private byte[] senderProof;

	@ManyToOne
	@Cascade({CascadeType.SAVE_UPDATE})
	@JoinColumn(name="recipientId")
	private Account recipient;
	
	private Integer blkIndex; // blkIndex inside block
	
	private Long amount;
	private Long referencedTransaction;

	private Integer messageType;
	private byte[] messagePayload;
	
	@ManyToOne(fetch= FetchType.LAZY)
    @JoinTable(name="block_transfers",
        joinColumns = {@JoinColumn(name="transfer_id", referencedColumnName="id")},  
        inverseJoinColumns = {@JoinColumn(name="block_id", referencedColumnName="id")}  
    )
    private Block block;
	
	public Transfer() {
	}
	
	public Transfer(
			Long shortId,
			byte[] transferHash,
			Integer version,
			Integer type,
			Long fee,
			Integer timestamp,
			Integer deadline,
			Account sender,
			byte[] senderProof, 
			Account recipient,
			Integer blkIndex,
			Long amount,
			Long referencedTransaction
			)
	{
		this.shortId = shortId;
		this.transferHash = transferHash;
		this.version = version;
		this.type = type;
		this.fee = fee;
		this.timestamp = timestamp;
		this.deadline = deadline;
		this.sender = sender;
		this.senderProof = senderProof;
		this.recipient = recipient;
		this.blkIndex = blkIndex;
		this.amount = amount;
		this.referencedTransaction = referencedTransaction;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getShortId() {
		return shortId;
	}

	public void setShortId(Long shortId) {
		this.shortId = shortId;
	}

	public byte[] getTransferHash() {
		return transferHash;
	}

	public void setTransferHash(byte[] transferHash) {
		this.transferHash = transferHash;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Long getFee() {
		return fee;
	}

	public void setFee(Long fee) {
		this.fee = fee;
	}

	public Integer getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getDeadline() {
		return deadline;
	}

	public void setDeadline(Integer deadline) {
		this.deadline = deadline;
	}

	public Account getSender() {
		return sender;
	}

	public void setSender(Account sender) {
		this.sender = sender;
	}

	public byte[] getSenderProof() {
		return senderProof;
	}

	public void setSenderProof(byte[] senderProof) {
		this.senderProof = senderProof;
	}

	public Account getRecipient() {
		return recipient;
	}

	public void setRecipient(Account recipient) {
		this.recipient = recipient;
	}

	public Integer getBlkIndex() {
		return blkIndex;
	}

	public void setBlkIndex(Integer blkIndex) {
		this.blkIndex = blkIndex;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public Long getReferencedTransaction() {
		return referencedTransaction;
	}

	public void setReferencedTransaction(Long referencedTransaction) {
		this.referencedTransaction = referencedTransaction;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public Integer getMessageType() {
		return messageType;
	}

	public void setMessageType(Integer messageType) {
		this.messageType = messageType;
	}

	public byte[] getMessagePayload() {
		return messagePayload;
	}

	public void setMessagePayload(byte[] messagePayload) {
		this.messagePayload = messagePayload;
	}
}
