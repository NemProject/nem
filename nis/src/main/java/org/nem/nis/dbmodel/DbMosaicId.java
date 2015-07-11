package org.nem.nis.dbmodel;

import org.nem.core.model.mosaic.MosaicId;
/**
 * Holds information about the mosaic database id
 */
public class DbMosaicId {
	private final Long id;

	public DbMosaicId(final Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof DbMosaicId)) {
			return false;
		}

		final DbMosaicId rhs = (DbMosaicId)obj;
		return this.id.equals(rhs.id);
	}
}
