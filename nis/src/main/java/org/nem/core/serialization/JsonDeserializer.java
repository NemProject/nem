package org.nem.core.serialization;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;

import java.math.BigInteger;

public class JsonDeserializer implements Deserializer {

    private final JSONObject object;

    public JsonDeserializer(final JSONObject object) throws Exception {
        this.object = object;
    }

    @Override
    public int readInt(final String label) throws Exception {
        return this.object.getInt(label);
    }

    @Override
    public long readLong(final String label) throws Exception {
        return this.object.getLong(label);
    }

    @Override
    public BigInteger readBigInteger(final String label) throws Exception {
        final byte[] bytes = this.readBytes(label);
        return new BigInteger(bytes);
    }

    @Override
    public byte[] readBytes(final String label) throws Exception {
        final String s = this.readString(label);
        return Base64.decode(s.getBytes("UTF-8"));
    }

    @Override
    public String readString(final String label) throws Exception {
        return this.object.getString(label);
    }
}
