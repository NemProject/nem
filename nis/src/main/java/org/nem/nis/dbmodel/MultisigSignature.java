package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;
import org.nem.core.crypto.Hash;

import javax.persistence.*;

// TODO 20141218 J-G: any reason this doesn't derive from AbstractTransfer?
@Entity
@Table(name = "multisigsignatures")
public class MultisigSignature {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long shortId;
	private byte[] transferHash;

	private Integer version;
	private Long fee;
	private Integer timeStamp;
	private Integer deadline;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "senderId")
	private Account sender;
	private byte[] senderProof;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "multisigTransactionId")
	private MultisigTransaction multisigTransaction;

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

	public void setTransferHash(final Hash transferHash) {
		this.shortId = transferHash.getShortId();
		this.transferHash = transferHash.getRaw();
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
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

	public MultisigTransaction getMultisigTransaction() {
		return this.multisigTransaction;
	}

	public void setMultisigTransaction(final MultisigTransaction multisigTransaction) {
		this.multisigTransaction = multisigTransaction;
	}
}
