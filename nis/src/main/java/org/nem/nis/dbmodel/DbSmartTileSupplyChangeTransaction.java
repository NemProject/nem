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

	private String namespaceId;

	private String name;

	private Integer supplyType;

	private Long quantity;

	public DbSmartTileSupplyChangeTransaction() {
		super(DbBlock::getBlockSmartTileSupplyChangeTransactions);
	}

	public String getNamespaceId() {
		return this.namespaceId;
	}

	public void setNamespaceId(final String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
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
