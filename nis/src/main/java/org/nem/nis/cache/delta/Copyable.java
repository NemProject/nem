package org.nem.nis.cache.delta;

/**
 * An object that supports creating a deep copy of itself.
 * TODO 20151013 J-J: is this interface really needed?
 */
public interface Copyable<T> {
	/**
	 * Creates a copy of this object.
	 *
	 * @return A copy of this object.
	 */
	T copy();
}
