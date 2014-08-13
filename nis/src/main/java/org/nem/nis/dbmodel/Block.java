package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;
import org.nem.core.crypto.Hash;

import javax.persistence.*;
import java.util.List;

/**
 * Db Block entity.
 *
 * Holds all the important information related to block data.
 *
 * Associated forger and transactions are obtained automatically (by BlockDao)
 * thanks to @Cascade annotations.
 */
@Entity
@Table(name = "blocks")
public class Block {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long shortId;

	private Integer version;
	private byte[] prevBlockHash;
	private byte[] blockHash;
	private byte[] generationHash;
	private Integer timestamp;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "forgerId")
	private Account forger;
	private byte[] forgerProof;

	private Long height;

	private Long totalAmount;
	private Long totalFee;

	private Long difficulty;

	private Long nextBlockId;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@OrderBy("blkIndex")
	private List<Transfer> blockTransfers;

	public Block() {
	}

	public Block(
			final Hash hash,
			final Integer version,
			final Hash generationHash,
			final Hash prevBlockHash,
			final Integer timestamp,
			final Account forger,
			final byte[] forgerProof,
			final Long height,
			final Long totalAmount,
			final Long totalFee,
			final Long difficulty) {

		this.shortId = hash.getShortId();
		this.version = version;
		this.generationHash = generationHash.getRaw();
		this.prevBlockHash = prevBlockHash.getRaw();
		this.blockHash = hash.getRaw();
		this.timestamp = timestamp;
		this.forger = forger;
		this.forgerProof = forgerProof;
		this.height = height;
		this.totalAmount = totalAmount;
		this.totalFee = totalFee;
		this.difficulty = difficulty;
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

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
	}

	public Hash getPrevBlockHash() {
		return new Hash(this.prevBlockHash);
	}

	public void setPrevBlockHash(final byte[] prevBlockHash) {
		this.prevBlockHash = prevBlockHash;
	}

	public Hash getBlockHash() {
		return new Hash(this.blockHash);
	}

	public void setBlockHash(final byte[] blockHash) {
		this.blockHash = blockHash;
	}

	public Hash getGenerationHash() {
		return new Hash(this.generationHash);
	}

	public void setGenerationHash(final byte[] generationHash) {
		this.generationHash = generationHash;
	}

	public Integer getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(final Integer timestamp) {
		this.timestamp = timestamp;
	}

	public Account getForger() {
		return this.forger;
	}

	public void setForgerId(final Account forger) {
		this.forger = forger;
	}

	public byte[] getForgerProof() {
		return this.forgerProof;
	}

	public void setForgerProof(final byte[] forgerProof) {
		this.forgerProof = forgerProof;
	}

	public Long getHeight() {
		return this.height;
	}

	public void setHeight(final Long height) {
		this.height = height;
	}

	public Long getTotalAmount() {
		return this.totalAmount;
	}

	public void setTotalAmount(final Long totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Long getTotalFee() {
		return this.totalFee;
	}

	public void setTotalFee(final Long totalFee) {
		this.totalFee = totalFee;
	}

	public Long getDifficulty() {
		return this.difficulty;
	}

	public void setDifficulty(final Long difficulty) {
		this.difficulty = difficulty;
	}

	public Long getNextBlockId() {
		return this.nextBlockId;
	}

	public void setNextBlockId(final Long nextBlockId) {
		this.nextBlockId = nextBlockId;
	}

	public List<Transfer> getBlockTransfers() {
		return this.blockTransfers;
	}

	public void setBlockTransfers(final List<Transfer> blockTransfers) {
		this.blockTransfers = blockTransfers;
	}
}
