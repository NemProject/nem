package org.nem.nis.cache;

/**
 * A cache that can be copied.
 */
@SuppressWarnings("rawtypes")
public interface CopyableCache<TDerived extends CopyableCache> {

	/**
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
