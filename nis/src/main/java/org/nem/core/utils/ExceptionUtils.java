package org.nem.core.utils;

/**
 * Static class that contains helper functions for dealing with exceptions.
 */
public class ExceptionUtils {

    /**
     * Wraps a checked InterruptedException in an unchecked exception.
     *
     * @param e The checked exception.
     * @return An unchecked exception.
     */
    public static RuntimeException toUnchecked(final InterruptedException e) {
        Thread.currentThread().interrupt();
        return new IllegalStateException(e);
    }
}
