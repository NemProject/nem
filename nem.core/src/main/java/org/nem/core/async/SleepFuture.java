package org.nem.core.async;

import java.util.*;
import java.util.concurrent.*;

/**
 * Static class containing methods for creating a delayed future.
 */
public class SleepFuture {
	private static final Timer TIMER = new Timer(true);

	/**
	 * Creates a new future that fires at the specified time in the future.
	 *
	 * @param delay The delay (in milliseconds).
	 * @param <T> Any type.
	 * @return The future.
	 */
	public static <T> CompletableFuture<T> create(final int delay) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				future.complete(null);
			}
		}, delay);
		return future;
	}
}
