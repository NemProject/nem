package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.*;

/**
 * Mosaic creation transaction db entity
 * <br>
 * Holds information about Transactions having type TransactionTypes.MOSAIC_CREATION
 */
@Entity
@Table(name = "mosaiccreationtransactions")
public class DbMosaicCreationTransaction extends AbstractBlockTransfer<DbMosaicCreationTransaction> {

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "mosaicCreationTransaction", orphanRemoval = true)
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
