package org.nem.peer.scheduling;

/**
 * Interface for creating schedulers.
 *
 * @param <T> The type of element that the scheduler is compatible with.
 */
public interface SchedulerFactory<T> {

	/**
	 * Creates a scheduler.
	 *
	 * @param action The type of action that the scheduler should run on each element.
	 *
	 * @return The scheduler.
	 */
	Scheduler<T> createScheduler(final Action<T> action);
}
