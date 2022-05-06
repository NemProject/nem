package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Base class for all transfer db entities that are stored directly in blocks.
 *
 * @param <TDerived> The derived transfer type.
 */
@MappedSuperclass
public abstract class AbstractBlockTransfer<TDerived extends AbstractBlockTransfer<?>> extends AbstractTransfer {
	private Integer blkIndex; // index inside block

	private Long referencedTransaction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blockId")
	private DbBlock block;

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

	public DbBlock getBlock() {
		return this.block;
	}

	public void setBlock(final DbBlock dbBlock) {
		this.block = dbBlock;
	}
}
