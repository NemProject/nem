package org.nem.nis.dbmodel;

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Namespace db entity <br>
 * Holds information about a single namespace.
 */
@Entity
@Table(name = "namespaces")
public class DbNamespace {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String fullName;

	@ManyToOne
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@JoinColumn(name = "ownerId")
	private DbAccount owner;

	private Long height;

	private Integer level;

	public DbNamespace() {
	}

	public DbNamespace(final DbNamespace original, final DbAccount owner, final Long height) {
		this.id = original.getId();
		this.fullName = original.getFullName();
		this.level = original.getLevel();
		this.owner = owner;
		this.height = height;
	}

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

	public Long getHeight() {
		return this.height;
	}

	public void setHeight(final Long height) {
		this.height = height;
	}

	public Integer getLevel() {
		return this.level;
	}

	public void setLevel(final Integer level) {
		this.level = level;
	}
}
