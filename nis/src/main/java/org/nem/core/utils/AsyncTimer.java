package org.nem.core.utils;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Timer that executes a future given on an interval.
 */
public class AsyncTimer implements Closeable {
	private CompletableFuture<?> recurringFuture;
	private final int delay;

	private int numExecutions;
	private final AtomicBoolean isStopped = new AtomicBoolean();
	private CompletableFuture<?> future;

	/**
	 * Creates a new async timer.
	 * @param recurringFuture The future that should be executed on an interval.
	 * @param initialDelay The time (in milliseconds) to delay the first execution.
	 * @param delay The delay (in milliseconds) between the termination of one execution and the
	 *              commencement of the next.
	 */
	public AsyncTimer(
			final CompletableFuture<?> recurringFuture,
			final int initialDelay,
			final int delay) {

		this.recurringFuture = recurringFuture;
		this.delay = delay;
		this.future = this.refresh(initialDelay);
	}

	/**
	 * Gets the number of times the user function has been executed.
	 *
	 * @return The number of times the user function has been executed.
	 */
	public int getNumExecutions() { return this.numExecutions; }

	/**
	 * Determines if this timer is stopped.
	 *
	 * @return true if this timer is stopped.
	 */
	public boolean isStopped() {
		return this.future.isDone();
	}

	private CompletableFuture<?> refresh(int delay) {
		return CompletableFuture.runAsync(() -> sleep(delay))
				.thenCompose(v -> getNextChainLink());
	}

	@Override
	public void close() {
		this.isStopped.set(true);
	}

	private CompletableFuture<?> getNextChainLink() {
		if (this.isStopped.get()) {
			this.log("stopped");
			final CompletableFuture<Void> terminatingFuture = new CompletableFuture<>();
			terminatingFuture.complete(null);
			return terminatingFuture;
		}

		this.log("recurring");
		++this.numExecutions;
		return this.recurringFuture
				.thenCompose(v -> this.refresh(this.delay));
	}

	private void sleep(int milliseconds) {
		ExceptionUtils.propagate(() -> {
			this.log("sleeping: " + delay);
			Thread.sleep(milliseconds);
			return null;
		});
	}

	// TODO: remove this logging when everything is working.
	private final static long initialTime = System.currentTimeMillis();

	private void log(final String s) {
		System.out.println(String.format(
				"z[%d - %d] %s (%d)",
				(System.currentTimeMillis() - initialTime),
				Thread.currentThread().getId(),
				s,
				this.numExecutions));
	}
}
