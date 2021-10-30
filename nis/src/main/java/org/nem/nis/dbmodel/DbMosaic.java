package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Mosaic db entity <br>
 * Holds information about a single mosaic.
 */
@Entity
@Table(name = "transferredmosaics")
public class DbMosaic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transferId")
	private DbTransferTransaction transferTransaction;

	private Long dbMosaicId;

	private Long quantity;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public DbTransferTransaction getTransferTransaction() {
		return this.transferTransaction;
	}

	public void setTransferTransaction(final DbTransferTransaction transferTransaction) {
		this.transferTransaction = transferTransaction;
	}

	public Long getDbMosaicId() {
		return this.dbMosaicId;
	}

	public void setDbMosaicId(final Long id) {
		this.dbMosaicId = id;
	}

	public Long getQuantity() {
		return this.quantity;
	}

	public void setQuantity(final Long quantity) {
		this.quantity = quantity;
	}
}
