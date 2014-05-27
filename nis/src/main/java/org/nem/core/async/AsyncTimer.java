package org.nem.core.async;

import java.io.Closeable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Timer that executes a future given on an interval.
 */
public class AsyncTimer implements Closeable {

	private static final Logger LOGGER = Logger.getLogger(AsyncTimer.class.getName());

	private final Supplier<CompletableFuture<?>> recurringFutureSupplier;
	private final AbstractDelayStrategy delay;
	private final CompletableFuture<?> future;

	private final AtomicBoolean isStopped = new AtomicBoolean();
	private final CompletableFuture<?> firstRecurrenceFuture = new CompletableFuture<>();
	private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

	private int numExecutions;
	private String name;

	/**
	 * Creates a new async timer.
	 *
	 * @param recurringFutureSupplier Supplier that provides a future that should be executed on an interval.
	 * @param initialDelay The time (in milliseconds) to delay the first execution.
	 * @param delay The delay strategy.
	 */
	public AsyncTimer(
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final int initialDelay,
			final AbstractDelayStrategy delay) {

		this.recurringFutureSupplier = recurringFutureSupplier;
		this.delay = delay;
		this.future = this.refresh(initialDelay);
	}

	private AsyncTimer(
			final CompletableFuture<?> trigger,
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final AbstractDelayStrategy delay) {

		this.recurringFutureSupplier = recurringFutureSupplier;
		this.delay = delay;
		this.future = this.createFuture(trigger);
	}

	private CompletableFuture<?> createFuture(final CompletableFuture<?> trigger) {
		return trigger.thenCompose(v -> this.getNextChainLink());
	}

	/**
	 * Creates a new AsyncTimer that will start executing when the specified trigger is triggered.
	 *
	 * @param triggerTimer The timer that will trigger the first execution when it has completed a single recurrence.
	 * @param recurringFutureSupplier Supplier that provides a future that should be executed on an interval.
	 * @param delay The delay strategy.
	 */
	public static AsyncTimer After(
			final AsyncTimer triggerTimer,
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final AbstractDelayStrategy delay) {

		return new AsyncTimer(triggerTimer.firstRecurrenceFuture, recurringFutureSupplier, delay);
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
		this.log("sleeping for " + delay + "ms");
		final SleepRunnable sleepRunnable = new SleepRunnable();
		this.scheduler.schedule(sleepRunnable, delay, TimeUnit.MILLISECONDS);
		return sleepRunnable.getFuture().thenCompose(v -> this.getNextChainLink());
	}

	@Override
	public void close() {
		this.isStopped.set(true);
	}

	private CompletableFuture<?> getNextChainLink() {
		if (this.isStopped.get() || this.delay.shouldStop()) {
			this.log("stopped");
			final CompletableFuture<Void> terminatingFuture = new CompletableFuture<>();
			terminatingFuture.complete(null);
			return terminatingFuture;
		}

		this.log("executing");
		++this.numExecutions;
		return this.recurringFutureSupplier.get()
				.exceptionally(e -> {
					LOGGER.warning(String.format("Timer %s raised exception: %s", this.getName(), e.getMessage()));
					return null;
				})
				.thenCompose(v -> {
					this.firstRecurrenceFuture.complete(null);
					return this.refresh(this.delay.next());
				});
	}

	private void log(final String message) {
//		LOGGER.info(String.format(
//				"[%d] Timer %s: %s (%d)",
//				Thread.currentThread().getId(),
//				this.getName(),
//				message,
//				this.numExecutions));
	}

	private static class SleepRunnable implements Runnable {
		private final CompletableFuture<Void> future;

		public SleepRunnable() {
			this.future = new CompletableFuture<>();
		}

		@Override
		public void run() {
			this.future.complete(null);
		}

		public CompletableFuture<Void> getFuture() {
			return this.future;
		}
	}
}
