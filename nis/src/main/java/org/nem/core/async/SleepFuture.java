package org.nem.core.async;

import java.util.concurrent.*;

/**
 * Static class containing methods for creating a delayed future.
 */
public class SleepFuture {

	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	/**
	 * Creates a new future that fires at the specified time in the future.
	 *
	 * @param delay The delay (in milliseconds).
	 */
	public static CompletableFuture<Void> create(final int delay) {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		scheduler.schedule(() -> future.complete(null), delay, TimeUnit.MILLISECONDS);
		return future;
	}
}
