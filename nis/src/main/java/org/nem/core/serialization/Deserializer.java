package org.nem.core.serialization;

import java.math.BigInteger;

public interface Deserializer {
    public int readInt() throws Exception;
    public long readLong() throws Exception;
    public BigInteger readBigInteger() throws Exception;
    public byte[] readBytes() throws Exception;
    public String readString() throws Exception;
}