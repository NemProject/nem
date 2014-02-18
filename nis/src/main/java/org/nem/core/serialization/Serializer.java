package org.nem.core.serialization;

import java.math.BigInteger;

/**
 * An interface for forward-only serialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface Serializer {
    public void writeInt(final String label, final int i) throws Exception;
    public void writeLong(final String label, final long l) throws Exception;
    public void writeBigInteger(final String label, final BigInteger i) throws Exception;
    public void writeBytes(final String label, final byte[] bytes) throws Exception;
    public void writeString(final String label, final String s) throws Exception;
}