package org.nem.core.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A builder for creating async timer options.
 */
public class AsyncTimerOptionsBuilder {
	private Supplier<CompletableFuture<?>> recurringFutureSupplier;
	private AbstractDelayStrategy delayStrategy;
	private CompletableFuture<?> trigger;
	private int initialDelay;
	private AsyncTimerVisitor visitor;

	/**
	 * Sets the recurring future supplier.
	 *
	 * @param recurringFutureSupplier The recurring future supplier.
	 * @return This builder.
	 */
	public AsyncTimerOptionsBuilder setRecurringFutureSupplier(final Supplier<CompletableFuture<?>> recurringFutureSupplier) {
		this.recurringFutureSupplier = recurringFutureSupplier;
		return this;
	}

	/**
	 * Sets the initial delay.
	 *
	 * @param initialDelay The initial delay.
	 * @return This builder.
	 */
	public AsyncTimerOptionsBuilder setInitialDelay(final int initialDelay) {
		this.initialDelay = initialDelay;
		return this;
	}

	/**
	 * Sets the trigger.
	 *
	 * @param trigger The trigger.
	 * @return This builder.
	 */
	public AsyncTimerOptionsBuilder setTrigger(final CompletableFuture<?> trigger) {
		this.trigger = trigger;
		return this;
	}

	/**
	 * Sets the delay strategy.
	 *
	 * @param delayStrategy The delay strategy.
	 * @return This builder.
	 */
	public AsyncTimerOptionsBuilder setDelayStrategy(final AbstractDelayStrategy delayStrategy) {
		this.delayStrategy = delayStrategy;
		return this;
	}

	/**
	 * Sets the visitor.
	 *
	 * @param visitor The visitor.
	 * @return This builder.
	 */
	public AsyncTimerOptionsBuilder setVisitor(final AsyncTimerVisitor visitor) {
		this.visitor = visitor;
		return this;
	}

	/**
	 * Creates a new async timer options.
	 *
	 * @return The async timer options
	 */
	public AsyncTimerOptions create() {
		return new AsyncTimerOptions();
	}

	private class AsyncTimerOptions implements org.nem.core.async.AsyncTimerOptions {
		private final CompletableFuture<?> trigger;
		private final AsyncTimerVisitor visitor;

		public AsyncTimerOptions() {
			final AsyncTimerVisitor visitor = AsyncTimerOptionsBuilder.this.visitor;
			this.visitor = null == visitor ? new DefaultAsyncTimerVisitor() : visitor;

			CompletableFuture<?> trigger = AsyncTimerOptionsBuilder.this.trigger;
			final int initialDelay = AsyncTimerOptionsBuilder.this.initialDelay;
			if (initialDelay > 0) {
				if (null == trigger) {
					trigger = this.delay(initialDelay);
				} else {
					trigger = trigger.thenCompose(v -> this.delay(initialDelay));
				}
			}

			if (null == trigger) {
				trigger = CompletableFuture.completedFuture(null);
			}

			this.trigger = trigger;
		}

		private CompletableFuture<?> delay(final int milliseconds) {
			this.visitor.notifyDelay(milliseconds);
			return SleepFuture.create(milliseconds);
		}

		@Override
		public Supplier<CompletableFuture<?>> getRecurringFutureSupplier() {
			return AsyncTimerOptionsBuilder.this.recurringFutureSupplier;
		}

		@Override
		public CompletableFuture<?> getInitialTrigger() {
			return this.trigger;
		}

		@Override
		public AbstractDelayStrategy getDelayStrategy() {
			return AsyncTimerOptionsBuilder.this.delayStrategy;
		}

		@Override
		public AsyncTimerVisitor getVisitor() {
			return this.visitor;
		}
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

		@Override
		public String getTimerName() {
			return "unknown timer";
		}
	}
}
