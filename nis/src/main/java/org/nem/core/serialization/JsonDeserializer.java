package org.nem.core.serialization;

import net.minidev.json.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * A json deserializer that supports label-based lookup in addition to forward-only deserialization.
 */
public class JsonDeserializer implements Deserializer {

    private final JSONObject object;
    private final DeserializationContext context;
    private final JSONArray propertyOrderArray;
    private int propertyOrderArrayIndex;

    /**
     * Creates a new json deserializer.
     *
     * @param object The json object from which to read.
     * @param context The deserialization context.
     */
    public JsonDeserializer(final JSONObject object, final DeserializationContext context) {
        this.object = object;
        this.context = context;
        this.propertyOrderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
        this.propertyOrderArrayIndex = 0;
    }

    @Override
    public Integer readInt(final String label) {
        this.checkLabel(label);
		return (Integer)this.object.get(label);
    }

    @Override
    public Long readLong(final String label) {
        this.checkLabel(label);

		// the alternative to this ugly piece, is setting up JSONParser.DEFAULT_PERMISSIVE_MODE
		// in CommonStarter, but I was trying, but I don't know, how to set it up before tests :/
		//
		// additionally, readInt above, would have to be changed to:
		// ((Long)this.object.get(label)).intValue();

		Object object = this.object.get(label);
		if (object instanceof Long) {
			return (Long)object;
		}
        return ((Integer)object).longValue();
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

    @Override
    public <T> T readObject(final String label, final ObjectDeserializer<T> activator) {
        this.checkLabel(label);
        JSONObject childObject = (JSONObject)this.object.get(label);
        return this.deserializeObject(childObject, activator);
    }

    @Override
    public <T> List<T> readObjectArray(final String label, final ObjectDeserializer<T> activator) {
        this.checkLabel(label);
        JSONArray jsonArray = (JSONArray)this.object.get(label);

        if (null == jsonArray)
            return new ArrayList<>();

        List<T> objects = new ArrayList<>();
        for (Object jsonObject : jsonArray)
            objects.add(this.deserializeObject((JSONObject) jsonObject, activator));

        return objects;
    }

    @Override
    public DeserializationContext getContext() {
        return this.context;
    }

    public <T> T deserializeObject(final JSONObject object, final ObjectDeserializer<T> activator) {
        JsonDeserializer deserializer = new JsonDeserializer(object, this.context);
        return 0 == object.size() ? null : activator.deserialize(deserializer);
    }

    private void checkLabel(final String label) {
        if (null == this.propertyOrderArray)
            return;

        final String expectedLabel = (String)this.propertyOrderArray.get(this.propertyOrderArrayIndex++);
        if (label.equals(expectedLabel))
            return;

        final String message = String.format(
            "expected property '%s' but request was for property '%s'",
            expectedLabel,
            label);
        throw new InvalidParameterException(message);
    }
}
