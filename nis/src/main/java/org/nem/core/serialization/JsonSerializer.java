package org.nem.core.serialization;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;

import java.math.BigInteger;

public class JsonSerializer implements Serializer {

    private final JSONObject object;

    public JsonSerializer() throws Exception {
        this.object = new JSONObject();
    }

    @Override
    public void writeInt(final String label, final int i) throws Exception {
        this.object.put(label, i);
    }

    @Override
    public void writeLong(final String label, final long l) throws Exception {
        this.object.put(label, l);
    }

    @Override
    public void writeBigInteger(final String label, final BigInteger i) throws Exception {
        this.writeBytes(label, i.toByteArray());
    }

    @Override
    public void writeBytes(final String label, final byte[] bytes) throws Exception {
        final String s = new String(Base64.encode(bytes), "UTF-8");
        this.writeString(label, s);
    }

    @Override
    public void writeString(final String label, final String s) throws Exception {
        this.object.put(label, s);
    }

    public JSONObject getObject() {
        return this.object;
    }
}
