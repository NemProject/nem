package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * NemProperty db entity for mosaic properties
 * <br>
 * Holds information about a single mosaic property.
 */
@Entity
@Table(name = "mosaicproperties")
public class DbMosaicProperty {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mosaicId")
	private DbMosaic mosaic;

	private String name;
	private String value;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public DbMosaic getMosaic() {
		return this.mosaic;
	}

	public void setMosaic(final DbMosaic mosaic) {
		this.mosaic = mosaic;
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
