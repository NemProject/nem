package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.nem.core.crypto.Hash;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.List;

/**
 * DbBlock entity.
 * <p>
 * Holds all the important information related to block data.
 * <p>
 * Associated harvester and transactions are obtained automatically (by BlockDao)
 * thanks to @Cascade annotations.
 */
@Entity
@Table(name = "blocks")
public class DbBlock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long shortId;

	private Integer version;
	private byte[] prevBlockHash;
	private byte[] blockHash;
	private byte[] generationHash;
	private Integer timeStamp;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "harvesterId")
	private DbAccount harvester;
	private byte[] harvesterProof;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "harvestedInName")
	private DbAccount lessor;

	private Long height;
	private Long totalFee;
	private Long difficulty;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@OrderBy("orderId")
	@LazyCollection(LazyCollectionOption.TRUE)
	@OrderColumn(name = "orderId")
	private List<DbTransferTransaction> blockTransferTransactions;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@OrderBy("orderId")
	@LazyCollection(LazyCollectionOption.TRUE)
	@OrderColumn(name = "orderId")
	private List<DbImportanceTransferTransaction> blockImportanceTransferTransactions;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@OrderBy("orderId")
	@LazyCollection(LazyCollectionOption.TRUE)
	@OrderColumn(name = "orderId")
	private List<DbMultisigAggregateModificationTransaction> blockMultisigAggregateModificationTransactions;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@OrderBy("orderId")
	@LazyCollection(LazyCollectionOption.TRUE)
	@OrderColumn(name = "orderId")
	private List<DbMultisigTransaction> blockMultisigTransactions;

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

	public void setPrevBlockHash(final Hash prevBlockHash) {
		this.prevBlockHash = prevBlockHash.getRaw();
	}

	public Hash getBlockHash() {
		return new Hash(this.blockHash);
	}

	public void setBlockHash(final Hash blockHash) {
		this.blockHash = blockHash.getRaw();
		this.shortId = blockHash.getShortId();
	}

	public Hash getGenerationHash() {
		return new Hash(this.generationHash);
	}

	public void setGenerationHash(final Hash generationHash) {
		this.generationHash = generationHash.getRaw();
	}

	public Integer getTimeStamp() {
		return this.timeStamp;
	}

	public void setTimeStamp(final Integer timeStamp) {
		this.timeStamp = timeStamp;
	}

	public DbAccount getHarvester() {
		return this.harvester;
	}

	public void setHarvester(final DbAccount harvester) {
		this.harvester = harvester;
	}

	public byte[] getHarvesterProof() {
		return this.harvesterProof;
	}

	public void setHarvesterProof(final byte[] harvesterProof) {
		this.harvesterProof = harvesterProof;
	}

	public DbAccount getLessor() {
		return this.lessor;
	}

	public void setLessor(final DbAccount lessor) {
		this.lessor = lessor;
	}

	public Long getHeight() {
		return this.height;
	}

	public void setHeight(final Long height) {
		this.height = height;
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

	public List<DbTransferTransaction> getBlockTransferTransactions() {
		return this.blockTransferTransactions;
	}

	public void setBlockTransferTransactions(final List<DbTransferTransaction> blockTransferTransactions) {
		this.blockTransferTransactions = blockTransferTransactions;
	}

	public List<DbImportanceTransferTransaction> getBlockImportanceTransferTransactions() {
		return this.blockImportanceTransferTransactions;
	}

	public void setBlockImportanceTransferTransactions(final List<DbImportanceTransferTransaction> blockImportanceTransferTransactions) {
		this.blockImportanceTransferTransactions = blockImportanceTransferTransactions;
	}

	public List<DbMultisigAggregateModificationTransaction> getBlockMultisigAggregateModificationTransactions() {
		return this.blockMultisigAggregateModificationTransactions;
	}

	public void setBlockMultisigAggregateModificationTransactions(final List<DbMultisigAggregateModificationTransaction> blockMultisigAggregateModificationTransactions) {
		this.blockMultisigAggregateModificationTransactions = blockMultisigAggregateModificationTransactions;
	}

	public List<DbMultisigTransaction> getBlockMultisigTransactions() {
		return blockMultisigTransactions;
	}

	public void setBlockMultisigTransactions(final List<DbMultisigTransaction> blockMultisigTransactions) {
		this.blockMultisigTransactions = blockMultisigTransactions;
	}
}
