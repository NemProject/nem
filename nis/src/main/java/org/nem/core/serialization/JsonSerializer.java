package org.nem.core.serialization;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

import java.math.BigInteger;

public class JsonSerializer implements Serializer {

    private final JSONObject object;
    private int counter;

    public JsonSerializer() throws Exception {
        this.counter = 1;
        this.object = new JSONObject();
    }

    @Override
    public void writeInt(final int i) throws Exception {
        this.object.put(this.getNextKey(), i);
    }

    @Override
    public void writeLong(final long l) throws Exception {
        this.object.put(this.getNextKey(), l);
    }

    @Override
    public void writeBigInteger(final BigInteger i) throws Exception {
        this.writeBytes(i.toByteArray());
    }

    @Override
    public void writeBytes(final byte[] bytes) throws Exception {
        final String s = new String(Base64.encode(bytes), "UTF-8");
        this.writeString(s);
    }

    @Override
    public void writeString(final String s) throws Exception {
        this.object.put(this.getNextKey(), s);
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

    public JSONObject getObject() {
        return this.object;
    }

    private String getNextKey() {
        final String key = String.format("%d", this.counter);
        ++this.counter;
        return key;
    }
}
