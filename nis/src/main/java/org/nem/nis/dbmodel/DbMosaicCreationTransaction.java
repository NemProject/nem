package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.*;

/**
 * Mosaic creation transaction db entity.
 * <br>
 * Holds information about Transactions having type TransactionTypes.MOSAIC_CREATION.
 */
@Entity
@Table(name = "mosaiccreationtransactions")
public class DbMosaicCreationTransaction extends AbstractBlockTransfer<DbMosaicCreationTransaction> {

	// TODO 20150702 J-B: shouldn't this be 1:1 ?
	// TODO 20150702 BR -> J: No, a mosaic can have children in stage 2, so we need to be prepared for it.
	// TODO 20150703 J-B: but shouldn't the many : many be on the mosaic instead of the transaction?
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "mosaicCreationTransaction", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<DbMosaic> mosaics;

	public DbMosaicCreationTransaction() {
		super(DbBlock::getBlockMosaicCreationTransactions);
	}

	public List<DbMosaic> getMosaics() {
		return this.mosaics;
	}

	public void setMosaics(final List<DbMosaic> mosaics) {
		this.mosaics = mosaics;
	}
}
