package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.Address;

import java.math.BigInteger;

/**
 * An ObjectDeserializer implementation that delegates primitive deserialization
 * to a wrapped Deserializer object.
 */
public class DelegatingObjectDeserializer implements ObjectDeserializer {

    private final Deserializer deserializer;
    private final AccountLookup accountLookup;

    /**
     * Creates a new delegating object deserializer.
     *
     * @param deserializer The Deserializer that should be used for primitive deserialization.
     * @param accountLookup The interface that should be used for looking up accounts.
     */
    public DelegatingObjectDeserializer(final Deserializer deserializer, final AccountLookup accountLookup) {
        this.deserializer = deserializer;
        this.accountLookup = accountLookup;
    }

    @Override
    public int readInt(final String label) {
        return this.deserializer.readInt(label);
    }

    @Override
    public long readLong(final String label) {
        return this.deserializer.readLong(label);
    }

    @Override
    public BigInteger readBigInteger(final String label) {
        return this.deserializer.readBigInteger(label);
    }

    @Override
    public byte[] readBytes(final String label) {
        return this.deserializer.readBytes(label);
    }

    @Override
    public String readString(final String label) {
        return this.deserializer.readString(label);
    }

    @Override
    public Address readAddress(final String label) {
        String encodedAddress = readString(label);
        return Address.fromEncoded(encodedAddress);
    }

    @Override
    public Account readAccount(final String label) {
        Address address = readAddress(label);
        return this.accountLookup.findByAddress(address);
    }

    @Override
    public Signature readSignature(final String label) {
        byte[] bytes = this.readBytes(label);
        try {
            try (BinaryDeserializer deserializer = new BinaryDeserializer(bytes)) {
                BigInteger r = deserializer.readBigInteger(null);
                BigInteger s = deserializer.readBigInteger(null);
                return new Signature(r, s);
            }
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
