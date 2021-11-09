package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;
import org.nem.core.crypto.Hash;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DbBlock entity. <br>
 * Holds all the important information related to block data. <br>
 * Associated harvester and transactions are obtained automatically (by BlockDao) thanks to @Cascade annotations.
 */
@Entity
@Table(name = "blocks")
public class DbBlock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer version;
	private byte[] prevBlockHash;
	private byte[] blockHash;
	private byte[] generationHash;
	private Integer timeStamp;

	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "harvesterId")
	private DbAccount harvester;
	private byte[] harvesterProof;

	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "harvestedInName")
	private DbAccount lessor;

	private Long height;
	private Long totalFee;
	private Long difficulty;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbTransferTransaction> blockTransferTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbImportanceTransferTransaction> blockImportanceTransferTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbMultisigAggregateModificationTransaction> blockMultisigAggregateModificationTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbMultisigTransaction> blockMultisigTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbProvisionNamespaceTransaction> blockProvisionNamespaceTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbMosaicDefinitionCreationTransaction> blockMosaicDefinitionCreationTransactions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "block", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.TRUE)
	private List<DbMosaicSupplyChangeTransaction> blockMosaicSupplyChangeTransactions = new ArrayList<>();

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
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
		return this.filter(this.blockTransferTransactions);
	}

	public void setBlockTransferTransactions(final List<DbTransferTransaction> blockTransferTransactions) {
		this.blockTransferTransactions = blockTransferTransactions;
	}

	public List<DbImportanceTransferTransaction> getBlockImportanceTransferTransactions() {
		return this.filter(this.blockImportanceTransferTransactions);
	}

	public void setBlockImportanceTransferTransactions(final List<DbImportanceTransferTransaction> blockImportanceTransferTransactions) {
		this.blockImportanceTransferTransactions = blockImportanceTransferTransactions;
	}

	public List<DbMultisigAggregateModificationTransaction> getBlockMultisigAggregateModificationTransactions() {
		return this.filter(this.blockMultisigAggregateModificationTransactions);
	}

	public void setBlockMultisigAggregateModificationTransactions(
			final List<DbMultisigAggregateModificationTransaction> blockMultisigAggregateModificationTransactions) {
		this.blockMultisigAggregateModificationTransactions = blockMultisigAggregateModificationTransactions;
	}

	public List<DbMultisigTransaction> getBlockMultisigTransactions() {
		return this.filter(this.blockMultisigTransactions);
	}

	public void setBlockMultisigTransactions(final List<DbMultisigTransaction> blockMultisigTransactions) {
		this.blockMultisigTransactions = blockMultisigTransactions;
	}

	public List<DbProvisionNamespaceTransaction> getBlockProvisionNamespaceTransactions() {
		return this.filter(this.blockProvisionNamespaceTransactions);
	}

	public void setBlockProvisionNamespaceTransactions(final List<DbProvisionNamespaceTransaction> blockProvisionNamespaceTransactions) {
		this.blockProvisionNamespaceTransactions = blockProvisionNamespaceTransactions;
	}

	public List<DbMosaicDefinitionCreationTransaction> getBlockMosaicDefinitionCreationTransactions() {
		return this.filter(this.blockMosaicDefinitionCreationTransactions);
	}

	public void setBlockMosaicDefinitionCreationTransactions(
			final List<DbMosaicDefinitionCreationTransaction> blockMosaicDefinitionCreationTransactions) {
		this.blockMosaicDefinitionCreationTransactions = blockMosaicDefinitionCreationTransactions;
	}

	public List<DbMosaicSupplyChangeTransaction> getBlockMosaicSupplyChangeTransactions() {
		return this.filter(this.blockMosaicSupplyChangeTransactions);
	}

	public void setBlockMosaicSupplyChangeTransactions(final List<DbMosaicSupplyChangeTransaction> blockMosaicSupplyChangeTransactions) {
		this.blockMosaicSupplyChangeTransactions = blockMosaicSupplyChangeTransactions;
	}

	@SuppressWarnings("rawtypes")
	private <T extends AbstractBlockTransfer> List<T> filter(final List<T> transactions) {
		return transactions.stream().filter(t -> null != t.getSenderProof()).collect(Collectors.toList());
	}

	public void addTransferTransaction(final DbTransferTransaction transaction) {
		this.blockTransferTransactions.add(transaction);
	}

	public void addImportanceTransferTransaction(final DbImportanceTransferTransaction transaction) {
		this.blockImportanceTransferTransactions.add(transaction);
	}

	public void addMultisigAggregateModificationTransaction(final DbMultisigAggregateModificationTransaction transaction) {
		this.blockMultisigAggregateModificationTransactions.add(transaction);
	}

	public void addMultisigTransaction(final DbMultisigTransaction transaction) {
		this.blockMultisigTransactions.add(transaction);
	}

	public void addProvisionNamespaceTransaction(final DbProvisionNamespaceTransaction transaction) {
		this.blockProvisionNamespaceTransactions.add(transaction);
	}

	public void addMosaicDefinitionCreationTransaction(final DbMosaicDefinitionCreationTransaction transaction) {
		this.blockMosaicDefinitionCreationTransactions.add(transaction);
	}

	public void addMosaicSupplyChangeTransaction(final DbMosaicSupplyChangeTransaction transaction) {
		this.blockMosaicSupplyChangeTransactions.add(transaction);
	}
}
