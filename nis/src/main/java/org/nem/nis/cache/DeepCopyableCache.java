package org.nem.nis.cache;

/**
 * A cache that can be deep copied.
 */
@SuppressWarnings("rawtypes")
public interface DeepCopyableCache<TDerived extends DeepCopyableCache> extends CopyableCache<TDerived> {

	/**
	 * Creates a deep copy of this cache.
	 *
	 * @return A deep copy of this cache.
	 */
	TDerived deepCopy();
}
