package org.nem.core.async;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Timer that executes a future given on an interval.
 */
public class AsyncTimer implements Closeable {
	private final Supplier<CompletableFuture<?>> recurringFutureSupplier;
	private final AbstractDelayStrategy delay;
	private final CompletableFuture<?> future;
	private final AsyncTimerVisitor visitor;

	private final AtomicBoolean isStopped = new AtomicBoolean();
	private final CompletableFuture<?> firstRecurrenceFuture = new CompletableFuture<>();

	/**
	 * Creates a new async timer.
	 *
	 * @param recurringFutureSupplier Supplier that provides a future that should be executed on an interval.
	 * @param initialDelay The time (in milliseconds) to delay the first execution.
	 * @param delay The delay strategy.
	 * @param visitor The visitor.
	 */
	public AsyncTimer(
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final int initialDelay,
			final AbstractDelayStrategy delay,
			final AsyncTimerVisitor visitor) {

		this.recurringFutureSupplier = recurringFutureSupplier;
		this.delay = delay;
		this.visitor = getVisitorOrDefault(visitor);
		this.future = this.chain(initialDelay);
	}

	private AsyncTimer(
			final CompletableFuture<?> trigger,
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final AbstractDelayStrategy delay,
			final AsyncTimerVisitor visitor) {

		this.recurringFutureSupplier = recurringFutureSupplier;
		this.delay = delay;
		this.visitor = getVisitorOrDefault(visitor);
		this.future = trigger.thenCompose(v -> this.getNextChainLink());
	}

	private static AsyncTimerVisitor getVisitorOrDefault(final AsyncTimerVisitor visitor) {
		return null == visitor ? new DefaultAsyncTimerVisitor() : visitor;
	}

	/**
	 * Creates a new AsyncTimer that will start executing when the specified trigger is triggered.
	 *
	 * @param triggerTimer The timer that will trigger the first execution when it has completed a single recurrence.
	 * @param recurringFutureSupplier Supplier that provides a future that should be executed on an interval.
	 * @param delay The delay strategy.
	 * @param visitor The visitor.
	 */
	public static AsyncTimer After(
			final AsyncTimer triggerTimer,
			final Supplier<CompletableFuture<?>> recurringFutureSupplier,
			final AbstractDelayStrategy delay,
			final AsyncTimerVisitor visitor) {

		return new AsyncTimer(triggerTimer.firstRecurrenceFuture, recurringFutureSupplier, delay, visitor);
	}

	/**
	 * Determines if this timer is stopped.
	 *
	 * @return true if this timer is stopped.
	 */
	public boolean isStopped() {
		return this.future.isDone();
	}

	private CompletableFuture<?> chain(int delay) {
		this.visitor.notifyDelay(delay);
		return SleepFuture.create(delay).thenCompose(v -> this.getNextChainLink());
	}

	@Override
	public void close() {
		this.isStopped.set(true);
	}

	private CompletableFuture<?> getNextChainLink() {
		if (this.isStopped.get() || this.delay.shouldStop()) {
			this.visitor.notifyStop();
			final CompletableFuture<Void> terminatingFuture = new CompletableFuture<>();
			terminatingFuture.complete(null);
			return terminatingFuture;
		}

		this.visitor.notifyOperationStart();
		return this.recurringFutureSupplier.get()
				.thenAccept(v -> this.visitor.notifyOperationComplete())
				.exceptionally(e -> {
					this.visitor.notifyOperationCompleteExceptionally(e);
					return null;
				})
				.thenCompose(v -> {
					this.firstRecurrenceFuture.complete(null);
					return this.chain(this.delay.next());
				});
	}

	private static class DefaultAsyncTimerVisitor implements AsyncTimerVisitor {
		@Override
		public void notifyOperationStart() {
		}

		@Override
		public void notifyOperationComplete() {
		}

		@Override
		public void notifyOperationCompleteExceptionally(final Throwable e) {
		}

		@Override
		public void notifyDelay(final int delay) {
		}

		@Override
		public void notifyStop() {
		}
	}
}
