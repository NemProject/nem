package org.nem.core.async;

import java.util.concurrent.*;

/**
 * Static class containing methods for creating a delayed future.
 */
public class SleepFuture {

	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

	/**
	 * Creates a new future that fires at the specified time in the future.
	 *
	 * @param delay The delay (in milliseconds).
	 * @param <T> Any type.
	 * @return The future.
	 */
	public static <T> CompletableFuture<T> create(final int delay) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		SCHEDULER.schedule(() -> future.complete(null), delay, TimeUnit.MILLISECONDS);
		return future;
	}
}
