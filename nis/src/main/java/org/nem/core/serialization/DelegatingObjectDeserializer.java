package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

import java.math.BigInteger;

public class DelegatingObjectDeserializer implements ObjectDeserializer {

    private final Deserializer deserializer;
    private final AccountLookup accountLookup;

    public DelegatingObjectDeserializer(final Deserializer deserializer, final AccountLookup accountLookup) {
        this.deserializer = deserializer;
        this.accountLookup = accountLookup;
    }

    @Override
    public int readInt(final String label) throws Exception {
        return this.deserializer.readInt(label);
    }

    @Override
    public long readLong(final String label) throws Exception {
        return this.deserializer.readLong(label);
    }

    @Override
    public BigInteger readBigInteger(final String label) throws Exception {
        return this.deserializer.readBigInteger(label);
    }

    @Override
    public byte[] readBytes(final String label) throws Exception {
        return this.deserializer.readBytes(label);
    }

    @Override
    public String readString(final String label) throws Exception {
        return this.deserializer.readString(label);
    }

    @Override
    public Account readAccount(final String label) throws Exception {
        String accountId = readString(label);
        return this.accountLookup.findById(accountId);
    }

    @Override
    public Signature readSignature(final String label) throws Exception {
        byte[] bytes = this.readBytes(label);
        try (BinaryDeserializer deserializer = new BinaryDeserializer(bytes)) {
            BigInteger r = deserializer.readBigInteger(null);
            BigInteger s = deserializer.readBigInteger(null);
            return new Signature(r, s);
        }
    }
}
