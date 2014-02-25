package org.nem.core.serialization;

import net.minidev.json.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;

/**
 * A json serializer that supports forward-only serialization.
 */
public class JsonSerializer implements Serializer {

    public static final String PROPERTY_ORDER_ARRAY_NAME = "_propertyOrderArray";

    private final JSONArray propertyOrderArray;
    private final JSONObject object;

    /**
     * Creates a default json serializer that does not enforce forward-only reads.
     */
    public JsonSerializer() {
        this(false);
    }

    /**
     * Creates a default json serializer
     *
     * @param enforceReadWriteOrder true if forward-only reads should be enforced.
     */
    public JsonSerializer(boolean enforceReadWriteOrder) {
        this.object = new JSONObject();
        this.propertyOrderArray = enforceReadWriteOrder ? new JSONArray() : null;
    }

    @Override
    public void writeInt(final String label, final int i) {
        this.pushLabel(label);
        this.object.put(label, i);
    }

    @Override
    public void writeLong(final String label, final long l) {
        this.pushLabel(label);
        this.object.put(label, l);
    }

    @Override
    public void writeBigInteger(final String label, final BigInteger i) {
        this.writeBytes(label, i.toByteArray());
    }

    @Override
    public void writeBytes(final String label, final byte[] bytes) {
        final String s = Base64Encoder.getString(bytes);
        this.writeString(label, s);
    }

    @Override
    public void writeString(final String label, final String s) {
        this.pushLabel(label);
        this.object.put(label, s);
    }

    /**
     * Gets the underlying JSON object.
     *
     * @return The underlying JSON object.
     */
    public JSONObject getObject() {
        if (null != this.propertyOrderArray)
            this.object.put(PROPERTY_ORDER_ARRAY_NAME, this.propertyOrderArray);

        return this.object;
    }

    private void pushLabel(final String label) {
        if (null == this.propertyOrderArray)
            return;

        this.propertyOrderArray.add(label);
    }
}
