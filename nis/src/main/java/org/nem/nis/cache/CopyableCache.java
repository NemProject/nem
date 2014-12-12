package org.nem.nis.cache;

/**
 * A cache that can be copied.
 */
public interface CopyableCache<TDerived extends CopyableCache> {

	/**
	 * Copies this facade's states to another facade's map.
	 *
	 * @param rhs The other facade.
	 */
	public void shallowCopyTo(final TDerived rhs);

	/**
	 * Creates a copy of this repository.
	 *
	 * @return A copy of this repository.
	 */
	public TDerived copy();
}
