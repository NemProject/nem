package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * NemProperty db entity for mosaic definition properties. <br>
 * Holds information about a single mosaic definition property.
 */
@Entity
@Table(name = "mosaicproperties")
public class DbMosaicProperty {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mosaicDefinitionId")
	private DbMosaicDefinition mosaicDefinition;

	private String name;
	private String value;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public DbMosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}

	public void setMosaicDefinition(final DbMosaicDefinition mosaicDefinition) {
		this.mosaicDefinition = mosaicDefinition;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(final String value) {
		this.value = value;
	}
}
