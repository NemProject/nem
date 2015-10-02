package org.nem.core.test;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.serialization.MissingRequiredPropertyException;

import java.util.concurrent.CompletionException;
import java.util.function.*;

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
	 * @param message The custom assertion message.
	 */
	public static void assertThrows(final Consumer<Void> consumer, final Class<?> exceptionClass, final String message) {
		assertThrows(consumer, exceptionClass, ex -> { }, message);
	}

	/**
	 * Asserts that the execution of consumer throws an exception of the specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 * @param assertExceptionProperties Consumer that is passed the matching exception to run any additional validation.
	 */
	public static <T> void assertThrows(
			final Consumer<Void> consumer,
			final Class<T> exceptionClass,
			final Consumer<T> assertExceptionProperties) {
		assertThrows(consumer, exceptionClass, assertExceptionProperties, null);
	}

	/**
	 * Asserts that the execution of consumer throws an exception of the specific class.
	 *
	 * @param consumer The consumer.
	 * @param exceptionClass The expected exception class.
	 * @param assertExceptionProperties Consumer that is passed the matching exception to run any additional validation.
	 * @param message The custom assertion message.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void assertThrows(
			final Consumer<Void> consumer,
			final Class<T> exceptionClass,
			final Consumer<T> assertExceptionProperties,
			final String message) {
		final String normalizedMessage = null == message ? "" : String.format("[%s]: ", message);
		try {
			consumer.accept(null);
		} catch (final Exception ex) {
			if (exceptionClass.isAssignableFrom(ex.getClass())) {
				assertExceptionProperties.accept((T)ex);
				return;
			}

			Assert.fail(String.format("%sunexpected exception of type %s was thrown: '%s'", normalizedMessage, ex.getClass(), ex.getMessage()));
		}

		Assert.fail(String.format("%sexpected exception of type %s was not thrown", normalizedMessage, exceptionClass));
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

	/**
	 * Asserts that a missing property exception was thrown.
	 *
	 * @param consumer The consumer.
	 * @param propertyName The expected missing property name.
	 */
	public static void assertThrowsMissingPropertyException(final Supplier<Object> consumer, final String propertyName) {
		ExceptionAssert.assertThrows(
				v -> consumer.get(),
				MissingRequiredPropertyException.class,
				ex -> Assert.assertThat(ex.getPropertyName(), IsEqual.equalTo(propertyName)));
	}
}
