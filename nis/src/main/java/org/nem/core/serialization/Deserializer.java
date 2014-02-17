package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

import java.math.BigInteger;

public interface Deserializer {
    public int readInt() throws Exception;
    public long readLong() throws Exception;
    public BigInteger readBigInteger() throws Exception;
    public byte[] readBytes() throws Exception;
    public String readString() throws Exception;
    public Account readAccount() throws Exception;
    public Signature readSignature() throws Exception;
}