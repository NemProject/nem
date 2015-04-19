package org.nem.core.function;

/**
 * Represents a function that accepts five arguments and produces a result.
 *
 * @param <T> The type of the first argument.
 * @param <U> The type of the second argument.
 * @param <V> The type of the third argument.
 * @param <W> The type of the fourth argument.
 * @param <X> The type of the fifth argument.
 * @param <R> The type of the result.
 */
@FunctionalInterface
public interface PentaFunction<T, U, V, W, X, R> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t The first argument.
	 * @param u The second argument.
	 * @param v The third argument.
	 * @param w The fourth argument.
	 * @param x The fifth argument.
	 * @return The result.
	 */
	R apply(T t, U u, V v, W w, X x);
}
