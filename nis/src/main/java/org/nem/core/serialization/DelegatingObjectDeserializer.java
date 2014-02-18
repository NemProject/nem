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
    public int readInt() throws Exception {
        return this.deserializer.readInt();
    }

    @Override
    public long readLong() throws Exception {
        return this.deserializer.readLong();
    }

    @Override
    public BigInteger readBigInteger() throws Exception {
        return this.deserializer.readBigInteger();
    }

    @Override
    public byte[] readBytes() throws Exception {
        return this.deserializer.readBytes();
    }

    @Override
    public String readString() throws Exception {
        return this.deserializer.readString();
    }

    @Override
    public Account readAccount() throws Exception {
        String accountId = readString();
        return this.accountLookup.findById(accountId);
    }

    @Override
    public Signature readSignature() throws Exception {
        BigInteger r = this.readBigInteger();
        BigInteger s = this.readBigInteger();
        return new Signature(r, s);
    }
}
