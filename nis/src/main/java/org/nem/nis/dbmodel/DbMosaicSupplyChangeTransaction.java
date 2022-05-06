package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Mosaic supply change db entity <br>
 * Holds information about a single mosaic supply change transaction.
 */
@Entity
@Table(name = "mosaicsupplychanges")
public class DbMosaicSupplyChangeTransaction extends AbstractBlockTransfer<DbMosaicSupplyChangeTransaction> {

	private Long dbMosaicId;

	private Integer supplyType;

	private Long quantity;

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
