package org.nem.core.utils;

/**
 * Predicate function that returns a boolean value given an object.
 *
 * @param <T> The type of object.
 */
public interface Predicate<T> {

	/**
	 * Evaluates the predicate against obj.
	 *
	 * @param obj The object to evaluate.
	 * @return The predicate result.
	 */
	public boolean evaluate(T obj);
}
