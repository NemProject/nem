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
    public void writeInt(final String label, final int i) throws Exception {
        this.serializer.writeInt(label, i);
    }

    @Override
    public void writeLong(final String label, final long l) throws Exception {
        this.serializer.writeLong(label, l);
    }

    @Override
    public void writeBigInteger(final String label, final BigInteger i) throws Exception {
        this.serializer.writeBigInteger(label, i);
    }

    @Override
    public void writeBytes(final String label, final byte[] bytes) throws Exception {
        this.serializer.writeBytes(label, bytes);
    }

    @Override
    public void writeString(final String label, final String s) throws Exception {
        this.serializer.writeString(label, s);
    }

    @Override
    public void writeAccount(final String label, final Account account) throws Exception {
        this.writeString(label, account.getId());
    }

    @Override
    public void writeSignature(final String label, final Signature signature) throws Exception {
        this.writeBigInteger(label + "_r", signature.getR());
        this.writeBigInteger(label + "_s", signature.getS());
    }
}
