package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Namespace db entity
 * <br>
 * Holds information about a single namespace.
 * TODO 20150619 J-B: just a question (don't change this), why did you decide to have a separate Namespaces table instead of storing everything in the ProvisionNamespaceTransaction table?
 * > isn't the mapping between the two always going to be 1:1?
 */
@Entity
@Table(name = "namespaces")
public class DbNamespace {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String fullName;

	@ManyToOne
	@Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
	@JoinColumn(name = "ownerId")
	private DbAccount owner;

	private Long expiryHeight;

	private Integer level;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getFullName() {
		return this.fullName;
	}

	public void setFullName(final String fullName) {
		this.fullName = fullName;
	}

	public DbAccount getOwner() {
		return this.owner;
	}

	public void setOwner(final DbAccount owner) {
		this.owner = owner;
	}

	public Long getExpiryHeight() {
		return this.expiryHeight;
	}

	public void setExpiryHeight(final Long expiryHeight) {
		this.expiryHeight = expiryHeight;
	}

	public Integer getLevel() {
		return this.level;
	}

	public void setLevel(final Integer level) {
		this.level = level;
	}
}
