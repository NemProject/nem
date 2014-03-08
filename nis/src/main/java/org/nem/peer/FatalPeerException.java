package org.nem.peer;

/**
 * A fatal (non-recoverable) peer exception.
 */
public class FatalPeerException extends RuntimeException {

    /**
     * Creates a new crypto exception.
     *
     * @param message The exception message.
     */
    public FatalPeerException(final String message) {
        super(message);
    }

    /**
     * Creates a new crypto exception.
     *
     * @param cause The exception message.
     */
    public FatalPeerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new crypto exception.
     *
     * @param message The exception message.
     * @param cause The original exception.
     */
    public FatalPeerException(final String message, Throwable cause) {
        super(message, cause);
    }
}
