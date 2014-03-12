package org.nem.peer.scheduling;

import java.util.Collection;

/**
 * Interface that provides methods for scheduling and waiting for work.
 *
 * @param <T> The type of element that the scheduler is compatible with.
 */
public interface Scheduler<T> {

    /**
     * Adds each element in elements to the work queue.
     *
     * @param elements Elements that should be added to the work queue.
     */
    public void push(final Collection<T> elements);

    /**
     * Blocks until all pending work is complete.
     */
    public void block();
}
