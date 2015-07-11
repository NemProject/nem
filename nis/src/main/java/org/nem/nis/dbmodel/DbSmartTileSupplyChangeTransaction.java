package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Smart tile supply change db entity
 * <br>
 * Holds information about a single smart tile supply change transaction.
 */
@Entity
@Table(name = "smarttilesupplychanges")
public class DbSmartTileSupplyChangeTransaction extends AbstractBlockTransfer<DbSmartTileSupplyChangeTransaction> {

	private Long dbMosaicId;

	private Integer supplyType;

	private Long quantity;

	public DbSmartTileSupplyChangeTransaction() {
		super(DbBlock::getBlockSmartTileSupplyChangeTransactions);
	}

	public Long getDbMosaicId() {
		return this.dbMosaicId;
	}

	public void setDbMosaicId(final Long id) {
		this.dbMosaicId = id;
	}

	public Integer getSupplyType() {
		return this.supplyType;
	}

	public void setSupplyType(final Integer supplyType) {
		this.supplyType = supplyType;
	}

	public Long getQuantity() {
		return this.quantity;
	}

	public void setQuantity(final Long quantity) {
		this.quantity = quantity;
	}

}
