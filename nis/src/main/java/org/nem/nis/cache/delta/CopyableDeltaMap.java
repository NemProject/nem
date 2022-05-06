package org.nem.nis.cache.delta;

/**
 * A delta map that can be copied.
 */
@SuppressWarnings({
		"unused", "rawtypes"
})
public interface CopyableDeltaMap<TDerived extends CopyableDeltaMap> {

	/**
	 * Commits all changes to the underlying map.
	 */
	void commit();

	/**
	 * Shallow copies this map to another map.
	 *
	 * @param rhs The other map.
	 */
	void shallowCopyTo(final TDerived rhs);

	/**
	 * Creates a rebased copy of this map that is a copy of the current map without any pending changes.
	 *
	 * @return The new map.
	 */
	TDerived rebase();

	/**
	 * Creates a deep copy of this map.
	 *
	 * @return The new map.
	 */
	TDerived deepCopy();
}
