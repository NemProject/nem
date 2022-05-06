package org.nem.nis.cache;

/**
 * A cache that can be committed.
 */
public interface CommittableCache {
	/**
	 * Commits all changes to the "real" cache.
	 */
	void commit();
}
