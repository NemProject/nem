package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Provision namespace db entity <br>
 * Holds information about a single provision namespace transaction.
 */
@Entity
@Table(name = "namespaceprovisions")
public class DbProvisionNamespaceTransaction extends AbstractBlockTransfer<DbProvisionNamespaceTransaction> {
	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "rentalFeeSinkId")
	private DbAccount rentalFeeSink;

	private Long rentalFee;

	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "namespaceId")
	private DbNamespace namespace;

	public DbAccount getRentalFeeSink() {
		return this.rentalFeeSink;
	}

	public void setRentalFeeSink(final DbAccount rentalFeeSink) {
		this.rentalFeeSink = rentalFeeSink;
	}

	public Long getRentalFee() {
		return this.rentalFee;
	}

	public void setRentalFee(final Long rentalFee) {
		this.rentalFee = rentalFee;
	}

	public DbNamespace getNamespace() {
		return this.namespace;
	}

	public void setNamespace(final DbNamespace namespace) {
		if (null != this.namespace) {
			throw new IllegalStateException("cannot reset namespace");
		}

		this.namespace = namespace;
	}

	@Override
	public void setSender(final DbAccount dbSender) {
		super.setSender(dbSender);

		// namespace must be set before the sender, otherwise there's no way to share the sender account
		if (null == this.namespace) {
			throw new IllegalStateException("cannot set sender before namespace");
		}

		this.namespace.setOwner(dbSender);
	}

	@Override
	public void setBlock(final DbBlock dbBlock) {
		super.setBlock(dbBlock);

		// for safety, require the namespace to be set before the block
		if (null == this.namespace) {
			throw new IllegalStateException("cannot set block before namespace");
		}

		// while loading blocks from db setBlock is called without dbBlock being fully initialized (height is null).
		// the height of the namespace is already set in this case, don't overwrite it.
		if (null != dbBlock.getHeight()) {
			this.namespace.setHeight(dbBlock.getHeight());
		}
	}
}
