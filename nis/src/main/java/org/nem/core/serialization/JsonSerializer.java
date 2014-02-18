package org.nem.core.serialization;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;

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

    public JSONObject getObject() {
        return this.object;
    }

    private String getNextKey() {
        final String key = String.format("%d", this.counter);
        ++this.counter;
        return key;
    }
}
