package org.nem.core.utils;

/**
 * Parameter-less function that returns an object.
 *
 * @param <T> The type of object.
 */
public interface Func<T> {

	/**
	 * Evaluates the function.
	 *
	 * @return The evaluation result.
	 */
	public T evaluate();
}