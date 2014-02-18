package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

import java.math.BigInteger;

public class DelegatingObjectSerializer implements ObjectSerializer {

    private final Serializer serializer;

    public DelegatingObjectSerializer(final Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void writeInt(final int i) throws Exception {
        this.serializer.writeInt(i);
    }

    @Override
    public void writeLong(final long l) throws Exception {
        this.serializer.writeLong(l);
    }

    @Override
    public void writeBigInteger(final BigInteger i) throws Exception {
        this.serializer.writeBigInteger(i);
    }

    @Override
    public void writeBytes(final byte[] bytes) throws Exception {
        this.serializer.writeBytes(bytes);
    }

    @Override
    public void writeString(final String s) throws Exception {
        this.serializer.writeString(s);
    }

    @Override
    public void writeAccount(final Account account) throws Exception {
        this.writeString(account.getId());
    }

    @Override
    public void writeSignature(final Signature signature) throws Exception {
        this.writeBigInteger(signature.getR());
        this.writeBigInteger(signature.getS());
    }
}
