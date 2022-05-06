package org.nem.nis.dbmodel;

import javax.persistence.*;

/**
 * Multisig Min Cosignatories Modification db entity. <br>
 * Holds information about single multisig min cosignatories modification.
 */
@Entity
@Table(name = "mincosignatoriesmodifications")
public class DbMultisigMinCosignatoriesModification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer relativeChange;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public Integer getRelativeChange() {
		return this.relativeChange;
	}

	public void setRelativeChange(final Integer relativeChange) {
		this.relativeChange = relativeChange;
	}
}
