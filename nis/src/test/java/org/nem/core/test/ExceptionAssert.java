package org.nem.core.test;

import org.junit.Assert;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Helper class that contains functions for asserting that specific exceptions
 * are thrown.
 */
public class ExceptionAssert {

	/**
	 * Asserts that the execution of consumer throws an exception of the specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 */
	public static void assertThrows(final Consumer<Void> consumer, final Class<?> exceptionClass) {
		assertThrows(consumer, exceptionClass, ex -> { });
	}

	/**
	 * Asserts that the execution of consumer throws an exception of the specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 * @param assertExceptionProperties Consumer that is passed the matching exception to run any additional validation.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void assertThrows(
			final Consumer<Void> consumer,
			final Class<T> exceptionClass,
			final Consumer<T> assertExceptionProperties) {
		try {
			consumer.accept(null);
		} catch (final Exception ex) {
			if (exceptionClass.isAssignableFrom(ex.getClass())) {
				assertExceptionProperties.accept((T)ex);
				return;
			}

			Assert.fail(String.format("unexpected exception of type %s was thrown: '%s'", ex.getClass(), ex.getMessage()));
		}

		Assert.fail(String.format("expected exception of type %s was not thrown", exceptionClass));
	}

	/**
	 * Asserts that the execution of consumer throws a completion exception wrapping an exception of the
	 * specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 */
	public static void assertThrowsCompletionException(final Consumer<Void> consumer, final Class<?> exceptionClass) {
		try {
			consumer.accept(null);
		} catch (final CompletionException completionEx) {
			final Throwable ex = completionEx.getCause();
			if (ex.getClass() == exceptionClass) {
				return;
			}

			Assert.fail(String.format("unexpected exception of type %s was thrown", ex.getClass()));
		}

		Assert.fail(String.format("expected exception of type %s was not thrown", exceptionClass));
	}
}
