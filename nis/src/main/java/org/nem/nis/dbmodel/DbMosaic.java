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

	private String mosaicId;

	private String description;

	private String namespaceId;

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

	public String getMosaicId() {
		return this.mosaicId;
	}

	public void setMosaicId(final String mosaicId) {
		this.mosaicId = mosaicId;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getNamespaceId() {
		return this.namespaceId;
	}

	public void setNamespaceId(final String namespaceId) {
		this.namespaceId = namespaceId;
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
