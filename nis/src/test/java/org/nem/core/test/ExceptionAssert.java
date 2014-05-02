package org.nem.core.test;

import org.junit.Assert;

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

			Assert.fail(String.format("unexpected exception of type %s was thrown", exceptionClass));
		}

		Assert.fail(String.format("expected exception of type %s was not thrown", exceptionClass));
	}
}
