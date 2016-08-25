package org.nem.core.async;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Timer that executes a future given on an interval.
 */
public class AsyncTimer implements Closeable {
	private static final Logger LOGGER = Logger.getLogger(NemAsyncTimerVisitor.class.getName());

	private final Supplier<CompletableFuture<?>> recurringFutureSupplier;
	private final AbstractDelayStrategy delay;
	private CompletableFuture<?> future;
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

	private void chain(final int delay) {
		this.visitor.notifyDelay(delay);
		CompletableFuture<?> oldFuture = this.future;
		this.future = SleepFuture.create(delay).thenCompose(v -> this.getNextChainLink());
		oldFuture.complete(null);
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

		try {
			this.visitor.notifyOperationStart();
			return this.recurringFutureSupplier.get()
					.thenAccept(v -> this.visitor.notifyOperationComplete())
					.exceptionally(e -> {
						this.visitor.notifyOperationCompleteExceptionally(e);
						return null;
					})
					.thenAccept(v -> {
						this.firstRecurrenceFuture.complete(null);
						this.chain(this.delay.next());
					});
		} catch (Exception e) {
			LOGGER.warning(String.format("Timer %s raised exception during start: %s",
					this.visitor.getTimerName(),
					e.toString()));
			this.chain(this.delay.next());
			return CompletableFuture.completedFuture(null);
		}
	}
}
