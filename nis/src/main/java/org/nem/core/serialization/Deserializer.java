package org.nem.core.serialization;

import java.math.BigInteger;

/**
 * An interface for forward-only deserialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface Deserializer {
    public int readInt(final String label) throws Exception;
    public long readLong(final String label) throws Exception;
    public BigInteger readBigInteger(final String label) throws Exception;
    public byte[] readBytes(final String label) throws Exception;
    public String readString(final String label) throws Exception;
}