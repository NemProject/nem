package org.nem.nis.cache;

/**
 * A cache that can be copied.
 */
public interface CopyableCache<TDerived extends CopyableCache> {

	/**
	 * TODO 20151013 J-J: can we remove this?
	 * Shallow copies this cache to another cache.
	 *
	 * @param rhs The other cache.
	 */
	void shallowCopyTo(final TDerived rhs);

	/**
	 * Creates a copy of this cache.
	 *
	 * @return A copy of this cache.
	 */
	TDerived copy();
}
