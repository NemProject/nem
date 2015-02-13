package org.nem.core.function;

/**
 * Represents a function that accepts four arguments and produces a result.
 *
 * @param <T> The type of the first argument.
 * @param <U> The type of the second argument.
 * @param <V> The type of the third argument.
 * @param <W> The type of the fourth argument.
 * @param <R> The type of the result.
 */
@FunctionalInterface
public interface QuadFunction<T, U, V, W, R> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t The first argument.
	 * @param u The second argument.
	 * @param v The third argument.
	 * @param w The fourth argument.
	 * @return The result.
	 */
	R apply(T t, U u, V v, W w);
}
