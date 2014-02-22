package org.nem.core.serialization;

import java.math.BigInteger;

/**
 * An interface for forward-only serialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface Serializer {

    /**
     * Writes a 32-bit integer value.
     *
     * @param label The optional name of the value.
     * @param i The value.
     */
    public void writeInt(final String label, final int i);

    /**
     * Writes a 64-bit long value.
     *
     * @param label The optional name of the value.
     * @param l The value.
     */
    public void writeLong(final String label, final long l);

    /**
     * Writes a BigInteger value.
     *
     * @param label The optional name of the value.
     * @param i The value.
     */
    public void writeBigInteger(final String label, final BigInteger i);

    /**
     * Writes a byte array value.
     *
     * @param label The optional name of the value.
     * @param bytes The value.
     */
    public void writeBytes(final String label, final byte[] bytes);

    /**
     * Writes a String value.
     *
     * @param label The optional name of the value.
     * @param s The value.
     */
    public void writeString(final String label, final String s);
}