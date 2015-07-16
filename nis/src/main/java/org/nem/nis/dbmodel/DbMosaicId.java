package org.nem.nis.dbmodel;

/**
 * Holds information about the mosaic database id.
 * TODO 20150715 J-B: comment publics
 * TODO 20150715 J-B: add simple tests for hashcode / equality
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
