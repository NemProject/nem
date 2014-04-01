package org.nem.peer.scheduling;

import org.nem.core.utils.ExceptionUtils;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParallelSchedulerFactory<T> implements SchedulerFactory<T> {

	final int numThreads;

	/**
	 * Creates a new parallel scheduler factory.
	 *
	 * @param numThreads The number of threads that each scheduler should use.
	 */
	public ParallelSchedulerFactory(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public Scheduler<T> createScheduler(Action<T> action) {
		return new ParallelScheduler<>(this.numThreads, action);
	}

	//region ParallelScheduler

	private static class ParallelScheduler<T> implements Scheduler<T> {

		final ThreadPoolExecutor executor;
		final Action<T> action;
		final AtomicBoolean canPush;

		public ParallelScheduler(final int numThreads, final Action<T> action) {
			this.executor = new ThreadPoolExecutor(
					numThreads,
					numThreads,
					0L,
					TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());
			this.action = action;
			this.canPush = new AtomicBoolean(true);
		}

		@Override
		public void push(final Collection<T> elements) {
			if (!this.canPush.get())
				throw new IllegalStateException("Cannot call push after block");

			for (final T element : elements)
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						action.execute(element);
					}
				});
		}

		@Override
		public void block() {
			this.canPush.set(false);
			this.executor.shutdown();

			try {
				this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw ExceptionUtils.toUnchecked(e);
			}
		}
	}

	//endregion
}