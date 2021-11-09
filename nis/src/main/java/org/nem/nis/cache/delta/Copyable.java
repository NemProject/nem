package org.nem.nis.cache.delta;

/**
 * An object that supports creating a deep copy of itself.
 */
public interface Copyable<T> {
	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	T copy();
}
