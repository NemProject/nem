package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.nem.core.crypto.Hash;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;

/**
 * Importance transfer db entity
 * <p>
 * Holds information about Transactions having type TransactionTypes.IMPORTANCE_TYPE
 * <p>
 */
@Entity
@Table(name = "importancetransfers")
public class ImportanceTransfer {
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
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "senderId")
	private Account sender;
	private byte[] senderProof;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "remoteId")
	private Account remote;

	private Integer direction;

	private Integer blkIndex; // blkIndex inside block

	private Long referencedTransaction;


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name = "block_importancetransfers",
			joinColumns = { @JoinColumn(name = "importancetransfer_id", referencedColumnName = "id") },
			inverseJoinColumns = { @JoinColumn(name = "block_id", referencedColumnName = "id") }
	)
	private Block block;

	public ImportanceTransfer() {
	}

	public ImportanceTransfer(
			final Hash hash,
			final Integer version,
			final Integer type,
			final Long fee,
			final Integer timeStamp,
			final Integer deadline,
			final Account sender,
			final byte[] senderProof,
			final Account remote,
			final Integer direction,
			final Integer blkIndex,
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
		this.remote = remote;
		this.direction = direction;
		this.blkIndex = blkIndex;
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

	public Account getRemote() {
		return this.remote;
	}

	public void setRemote(final Account remote) {
		this.remote = remote;
	}

	public Integer getDirection() {
		return direction;
	}

	public void setDirection(Integer direction) {
		this.direction = direction;
	}

	public Integer getBlkIndex() {
		return this.blkIndex;
	}

	public void setBlkIndex(final Integer blkIndex) {
		this.blkIndex = blkIndex;
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
}
