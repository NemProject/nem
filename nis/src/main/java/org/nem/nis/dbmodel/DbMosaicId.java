package org.nem.nis.dbmodel;

/**
 * Holds information about the mosaic database id.
 */
public class DbMosaicId {
	private final Long id;

	/**
	 * Creates a new db mosaic id.
	 *
	 * @param id The id.
	 */
	public DbMosaicId(final Long id) {
		this.id = id;
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
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

		final DbMosaicId rhs = (DbMosaicId) obj;
		return this.id.equals(rhs.id);
	}
}
