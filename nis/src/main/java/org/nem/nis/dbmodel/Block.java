package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;

/**
 * Db Block entity.
 * <p/>
 * Holds all the important information related to block data.
 * <p/>
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
	private Integer timestamp;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "forgerId")
	private Account forger;
	private byte[] forgerProof;

	private Long height;

	private Long totalAmount;
	private Long totalFee;

	private Long nextBlockId;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "block", orphanRemoval=true)
	@OrderBy("blkIndex")
	private List<Transfer> blockTransfers;

	public Block() {
	}

	public Block(
			Long shortId,
			Integer version,
			byte[] prevBlockHash,
			byte[] blockHash,
			Integer timestamp,
			Account forger,
			byte[] forgerProof,
			Long height,
			Long totalAmount,
			Long totalFee) {

		this.shortId = shortId;
		this.version = version;
		this.prevBlockHash = prevBlockHash;
		this.blockHash = blockHash;
		this.timestamp = timestamp;
		this.forger = forger;
		this.forgerProof = forgerProof;
		this.height = height;
		this.totalAmount = totalAmount;
		this.totalFee = totalFee;
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

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public byte[] getPrevBlockHash() {
		return prevBlockHash;
	}

	public void setPrevBlockHash(byte[] prevBlockHash) {
		this.prevBlockHash = prevBlockHash;
	}

	public byte[] getBlockHash() {
		return blockHash;
	}

	public void setBlockHash(byte[] blockHash) {
		this.blockHash = blockHash;
	}

	public Integer getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}

	public Account getForger() {
		return forger;
	}

	public void setForgerId(Account forger) {
		this.forger = forger;
	}

	public byte[] getForgerProof() {
		return forgerProof;
	}

	public void setForgerProof(byte[] forgerProof) {
		this.forgerProof = forgerProof;
	}

	public Long getHeight() {
		return height;
	}

	public void setHeight(Long height) {
		this.height = height;
	}

	public Long getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Long totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Long getTotalFee() {
		return totalFee;
	}

	public void setTotalFee(Long totalFee) {
		this.totalFee = totalFee;
	}

	public Long getNextBlockId() {
		return nextBlockId;
	}

	public void setNextBlockId(Long nextBlockId) {
		this.nextBlockId = nextBlockId;
	}

	public List<Transfer> getBlockTransfers() {
		return blockTransfers;
	}

	public void setBlockTransfers(List<Transfer> blockTransfers) {
		this.blockTransfers = blockTransfers;
	}


}