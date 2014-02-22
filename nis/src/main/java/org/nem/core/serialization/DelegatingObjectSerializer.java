package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.Address;

import java.math.BigInteger;

/**
 * An ObjectSerializer implementation that delegates primitive serialization
 * to a wrapped Serializer object.
 */
public class DelegatingObjectSerializer implements ObjectSerializer {

    private final Serializer serializer;

    /**
     * Creates a new delegating object serializer.
     *
     * @param serializer The Serializer that should be used for primitive serializer.
     */
    public DelegatingObjectSerializer(final Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void writeInt(final String label, final int i) {
        this.serializer.writeInt(label, i);
    }

    @Override
    public void writeLong(final String label, final long l) {
        this.serializer.writeLong(label, l);
    }

    @Override
    public void writeBigInteger(final String label, final BigInteger i) {
        this.serializer.writeBigInteger(label, i);
    }

    @Override
    public void writeBytes(final String label, final byte[] bytes) {
        this.serializer.writeBytes(label, bytes);
    }

    @Override
    public void writeString(final String label, final String s) {
        this.serializer.writeString(label, s);
    }

    @Override
    public void writeAddress(final String label, final Address address) {
        this.writeString(label, address.getEncoded());
    }

    @Override
    public void writeAccount(final String label, final Account account) {
        this.writeAddress(label, account.getAddress());
    }

    @Override
    public void writeSignature(final String label, final Signature signature) {
        try {
            try (BinarySerializer serializer = new BinarySerializer()) {
                serializer.writeBigInteger(null, signature.getR());
                serializer.writeBigInteger(null, signature.getS());
                this.writeBytes(label, serializer.getBytes());
            }
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
