package org.nem.peer;

import java.util.Collection;
import java.util.concurrent.*;

public class ParallelScheduler<T> {

    final ThreadPoolExecutor executor;
    final Action<T> action;

    /**
     * Creates a new parallel scheduler.
     *
     * @param numThreads The number of threads to use.
     * @param action The action to apply to all elements.
     */
    public ParallelScheduler(final int numThreads, final Action<T> action) {
        this.executor = new ThreadPoolExecutor(
            numThreads,
            numThreads,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
        this.action = action;
    }

    public void push(final Collection<T> elements) {
        for (final T element : elements)
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    action.execute(element);
                }
            });
    }

    public void block() {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Interface for executing an action given an element.
     */
    public static interface Action<T> {

        /**
         * Executes the action.
         *
         * @param element The element.
         */
        void execute(final T element);
    }
}
