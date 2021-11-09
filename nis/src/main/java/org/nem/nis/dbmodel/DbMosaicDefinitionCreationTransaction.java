package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Mosaic definition creation transaction db entity. <br>
 * Holds information about Transactions having type TransactionTypes.MOSAIC_DEFINITION_CREATION.
 */
@Entity
@Table(name = "mosaicdefinitioncreationtransactions")
public class DbMosaicDefinitionCreationTransaction extends AbstractBlockTransfer<DbMosaicDefinitionCreationTransaction> {

	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "mosaicDefinitionId")
	private DbMosaicDefinition mosaicDefinition;

	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "creationFeeSinkId")
	private DbAccount creationFeeSink;

	private Long creationFee;

	public DbMosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}

	public void setMosaicDefinition(final DbMosaicDefinition mosaicDefinition) {
		this.mosaicDefinition = mosaicDefinition;
	}

	public DbAccount getCreationFeeSink() {
		return this.creationFeeSink;
	}

	public void setCreationFeeSink(final DbAccount creationFeeSink) {
		this.creationFeeSink = creationFeeSink;
	}

	public Long getCreationFee() {
		return this.creationFee;
	}

	public void setCreationFee(final Long creationFee) {
		this.creationFee = creationFee;
	}

	@Override
	public void setSender(final DbAccount dbSender) {
		super.setSender(dbSender);

		// mosaicDefinition must be set before the sender, otherwise there's no way to share the sender account
		if (null == this.mosaicDefinition) {
			throw new IllegalStateException("cannot set sender before mosaicDefinition");
		}

		this.mosaicDefinition.setCreator(dbSender);
		final DbAccount feeRecipient = this.mosaicDefinition.getFeeRecipient();
		if (null != feeRecipient && dbSender.getId().equals(feeRecipient.getId())) {
			this.mosaicDefinition.setFeeRecipient(dbSender);
		}
	}
}
