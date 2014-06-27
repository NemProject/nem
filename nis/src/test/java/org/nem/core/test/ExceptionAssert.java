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
		try {
			consumer.accept(null);
		}
		catch (Exception ex) {
			if (ex.getClass() == exceptionClass)
				return;

			Assert.fail(String.format("unexpected exception of type %s was thrown", ex.getClass()));
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
		}
		catch (CompletionException completionEx) {
			final Throwable ex = completionEx.getCause();
			if (ex.getClass() == exceptionClass)
				return;

			Assert.fail(String.format("unexpected exception of type %s was thrown", ex.getClass()));
		}

		Assert.fail(String.format("expected exception of type %s was not thrown", exceptionClass));
	}

	/**
	 * Asserts that the execution of consumer throws an exception of the specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 */
	public static void assertDoesNotThrow(final Consumer<Void> consumer) {
		try {
			consumer.accept(null);
		}
		catch (Exception ex) {
			Assert.fail(String.format("unexpected exception of type %s was thrown", ex.getClass()));
		}
	}
}
