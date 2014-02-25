package org.nem.core.crypto;

/**
 * Exception that is used when a cryptographic operation fails.
 */
public class CryptoException extends RuntimeException {

    /**
     * Creates a new crypto exception.
     *
     * @param message The exception message.
     */
    public CryptoException(final String message) {
        super(message);
    }

    /**
     * Creates a new crypto exception.
     *
     * @param cause The exception message.
     */
    public CryptoException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new crypto exception.
     *
     * @param message The exception message.
     * @param cause The original exception.
     */
    public CryptoException(final String message, Throwable cause) {
        super(message, cause);
    }
}
