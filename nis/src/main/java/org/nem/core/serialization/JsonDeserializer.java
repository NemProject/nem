package org.nem.core.serialization;

import net.minidev.json.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;
import java.security.InvalidParameterException;

/**
 * A json deserializer that supports label-based lookup in addition to forward-only deserialization.
 */
public class JsonDeserializer implements Deserializer {

    private final JSONObject object;
    private final JSONArray propertyOrderArray;
    private int propertyOrderArrayIndex;

    /**
     * Creates a new json deserializer.
     *
     * @param object The json object from which to read.
     */
    public JsonDeserializer(final JSONObject object) {
        this.object = object;
        this.propertyOrderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
        this.propertyOrderArrayIndex = 0;
    }

    @Override
    public int readInt(final String label) {
        this.checkLabel(label);
        return (Integer)this.object.get(label);
    }

    @Override
    public long readLong(final String label) {
        this.checkLabel(label);
        return (Long)this.object.get(label);
    }

    @Override
    public BigInteger readBigInteger(final String label) {
        final byte[] bytes = this.readBytes(label);
        return new BigInteger(bytes);
    }

    @Override
    public byte[] readBytes(final String label) {
        final String s = this.readString(label);
        return s.isEmpty() ? new byte[] { } : Base64Encoder.getBytes(s);
    }

    @Override
    public String readString(final String label) {
        this.checkLabel(label);
        return (String)this.object.get(label);
    }

    private void checkLabel(final String label) {
        if (null == this.propertyOrderArray)
            return;

        final String expectedLabel = (String)this.propertyOrderArray.get(this.propertyOrderArrayIndex++);
        if (label.equals(expectedLabel))
            return;

        final String message = String.format("expected property '%s' but request was for property '%s'", expectedLabel, label);
        throw new InvalidParameterException(message);
    }
}
