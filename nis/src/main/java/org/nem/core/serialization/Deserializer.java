package org.nem.core.serialization;

import java.math.BigInteger;

/**
 * An interface for forward-only deserialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface Deserializer {

    /**
     * Reads a 32-bit integer value.
     *
     * @param label The optional name of the value.
     * @return The read value.
     */
    public int readInt(final String label);

    /**
     * Reads a 64-bit long value.
     *
     * @param label The optional name of the value.
     * @return The read value.
     */
    public long readLong(final String label);

    /**
     * Reads a BigInteger value.
     *
     * @param label The optional name of the value.
     * @return The read value.
     */
    public BigInteger readBigInteger(final String label);

    /**
     * Reads a byte array value
     *
     * @param label The optional name of the value.
     * @return The read value.
     */
    public byte[] readBytes(final String label);

    /**
     * Reads a String value.
     *
     * @param label The optional name of the value.
     * @return The read value.
     */
    public String readString(final String label);
}