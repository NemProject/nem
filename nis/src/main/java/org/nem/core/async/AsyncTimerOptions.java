package org.nem.core.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Async timer options.
 */
public interface AsyncTimerOptions {

	/**
	 * Gets the recurring future supplier.
	 *
	 * @return The recurring future supplier.
	 */
	Supplier<CompletableFuture<?>> getRecurringFutureSupplier();

	/**
	 * Gets the initial trigger.
	 *
	 * @return The initial trigger.
	 */
	CompletableFuture<?> getInitialTrigger();

	/**
	 * Gets the delay strategy.
	 *
	 * @return The delay strategy.
	 */
	AbstractDelayStrategy getDelayStrategy();

	/**
	 * Gets the timer visitor.
	 *
	 * @return The timer visitor.
	 */
	AsyncTimerVisitor getVisitor();
}
