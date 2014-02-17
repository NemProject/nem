package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

import java.math.BigInteger;

public interface Serializer {
    public void writeInt(final int i) throws Exception;
    public void writeLong(final long l) throws Exception;
    public void writeBigInteger(final BigInteger i) throws Exception;
    public void writeBytes(final byte[] bytes) throws Exception;
    public void writeString(final String s) throws Exception;
    public void writeAccount(final Account account) throws Exception;
    public void writeSignature(final Signature signature) throws Exception;
}