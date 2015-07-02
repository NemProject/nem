package org.nem.nis.dbmodel;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.Set;

/**
 * Mosaic db entity.
 * <br>
 * Holds information about a single mosaic.
 */
@Entity
@Table(name = "mosaics")
public class DbMosaic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO 20150702 J-B: why do you need this FK reference?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mosaicCreationTransactionId")
	private DbMosaicCreationTransaction mosaicCreationTransaction;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "mosaic", orphanRemoval = true)
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<DbMosaicProperty> properties;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "creatorId")
	private DbAccount creator;

	private Long amount;

	private Integer position;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public DbMosaicCreationTransaction getMosaicCreationTransaction() {
		return this.mosaicCreationTransaction;
	}

	public void setMosaicCreationTransaction(final DbMosaicCreationTransaction mosaicCreationTransaction) {
		this.mosaicCreationTransaction = mosaicCreationTransaction;
	}

	public Set<DbMosaicProperty> getProperties() {
		return this.properties;
	}

	public void setProperties(final Set<DbMosaicProperty> properties) {
		this.properties = properties;
	}

	public DbAccount getCreator() {
		return this.creator;
	}

	public void setCreator(final DbAccount creator) {
		this.creator = creator;
	}

	public Long getAmount() {
		return this.amount;
	}

	public void setAmount(final Long amount) {
		this.amount = amount;
	}

	public Integer getPosition() {
		return this.position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}
}
