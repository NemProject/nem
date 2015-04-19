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
	 * @param options The async timer options.
	 */
	public AsyncTimer(final AsyncTimerOptions options) {
		this.recurringFutureSupplier = options.getRecurringFutureSupplier();
		this.delay = options.getDelayStrategy();
		this.visitor = options.getVisitor();
		this.future = options.getInitialTrigger().thenCompose(v -> this.getNextChainLink());
	}

	/**
	 * Determines if this timer is stopped.
	 *
	 * @return true if this timer is stopped.
	 */
	public boolean isStopped() {
		return this.future.isDone();
	}

	/**
	 * Gets a future that is triggered after the recurring future is completed at least once.
	 *
	 * @return The future.
	 */
	public CompletableFuture<?> getFirstFireFuture() {
		return this.firstRecurrenceFuture;
	}

	private CompletableFuture<?> chain(final int delay) {
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
}
