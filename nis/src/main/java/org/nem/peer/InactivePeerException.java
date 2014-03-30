package org.nem.peer;

/**
 * A exception that is thrown when attempting to communicate with an inactive peer.
 */
public class InactivePeerException extends RuntimeException {

    /**
     * Creates a new exception.
     *
     * @param message The exception message.
     */
    public InactivePeerException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception.
     *
     * @param cause The exception message.
     */
    public InactivePeerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception.
     *
     * @param message The exception message.
     * @param cause The original exception.
     */
    public InactivePeerException(final String message, Throwable cause) {
        super(message, cause);
    }
}
