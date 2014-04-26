package org.nem.core.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Static class that contains helper functions for dealing with exceptions.
 */
public class ExceptionUtils {

	private ExceptionUtils() {
	}

	/**
	 * Wraps a checked InterruptedException in an unchecked exception.
	 *
	 * @param e The checked exception.
	 *
	 * @return An unchecked exception.
	 */
	public static RuntimeException toUnchecked(final InterruptedException e) {
		Thread.currentThread().interrupt();
		return new IllegalStateException(e);
	}

	/**
	 * Propagates checked exceptions as a runtime exception.
	 *
	 * @param callable The function.
	 * @param <T> The function return type.
	 * @return The function result.
	 */
	public static <T> T propagate(final Callable<T> callable) {
		return propagate(callable, RuntimeException::new);
	}

	/**
	 * Propagates checked exceptions as a specific runtime exception.
	 *
	 * @param callable The function.
	 * @param wrap A function that wraps an exception in a runtime exception.
	 * @param <T> The function return type.
	 * @param <E> The specific exception type.
	 * @return The function result.
	 */
	public static <T, E extends RuntimeException> T propagate(final Callable<T> callable, final Function<Exception, E> wrap) {
		try {
			return callable.call();
		} catch (ExecutionException e) {
			if (RuntimeException.class.isAssignableFrom(e.getCause().getClass()))
				throw (RuntimeException)e.getCause();
			throw wrap.apply(e);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw wrap.apply(e);
		}
	}
}
