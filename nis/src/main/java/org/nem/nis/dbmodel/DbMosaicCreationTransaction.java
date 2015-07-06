package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Mosaic creation transaction db entity.
 * <br>
 * Holds information about Transactions having type TransactionTypes.MOSAIC_CREATION.
 */
@Entity
@Table(name = "mosaiccreationtransactions")
public class DbMosaicCreationTransaction extends AbstractBlockTransfer<DbMosaicCreationTransaction> {

	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "mosaicId")
	private DbMosaic mosaic;

	public DbMosaicCreationTransaction() {
		super(DbBlock::getBlockMosaicCreationTransactions);
	}

	public DbMosaic getMosaic() {
		return this.mosaic;
	}

	public void setMosaic(final DbMosaic mosaic) {
		this.mosaic = mosaic;
	}
}
