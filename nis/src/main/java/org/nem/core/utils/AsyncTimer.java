package org.nem.core.utils;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Timer that executes a future given on an interval.
 */
public class AsyncTimer implements Closeable {

	private static final Logger LOGGER = Logger.getLogger(AsyncTimer.class.getName());

	private CompletableFuture<?> recurringFuture;
	private final int delay;

	private int numExecutions;
	private final AtomicBoolean isStopped = new AtomicBoolean();
	private CompletableFuture<?> future;

	private String name;

	/**
	 * Creates a new async timer.
	 *
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

	private AsyncTimer(
			final CompletableFuture<?> trigger,
			final CompletableFuture<?> recurringFuture,
			final int delay) {

		this.recurringFuture = recurringFuture;
		this.delay = delay;
		this.future = trigger.thenCompose(v -> this.getNextChainLink());
	}

	/**
	 * Creates a new AsyncTimer that will start executing when the specified trigger is triggered.
	 *
	 * @param trigger The future that will trigger the first execution when fired.
	 * @param recurringFuture The future that should be executed on an interval.
	 * @param delay The delay (in milliseconds) between the termination of one execution and the
	 *              commencement of the next.
	 */
	public static AsyncTimer After(
			final CompletableFuture<?> trigger,
			final CompletableFuture<?> recurringFuture,
			final int delay) {

		return new AsyncTimer(trigger, recurringFuture, delay);
	}

	/**
	 * Gets the number of times the user function has been executed.
	 *
	 * @return The number of times the user function has been executed.
	 */
	public int getNumExecutions() { return this.numExecutions; }

	/**
	 * Gets the name of the timer.
	 *
	 * @return The name of the timer.
	 */
	public String getName() { return this.name; }

	/**
	 * Sets the name of the timer.
	 *
	 * @param name The name of the timer.
	 */
	public void setName(final String name) { this.name = name; }

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

		this.log("executing");
		++this.numExecutions;
		return this.recurringFuture
				.thenCompose(v -> this.refresh(this.delay));
	}

	private void sleep(int milliseconds) {
		ExceptionUtils.propagate(() -> {
			this.log("sleeping for " + delay + "ms");
			Thread.sleep(milliseconds);
			return null;
		});
	}

	private void log(final String message) {
		LOGGER.info(String.format(
				"[%d] Timer %s: %s (%d)",
				Thread.currentThread().getId(),
				this.getName(),
				message,
				this.numExecutions));
	}
}
