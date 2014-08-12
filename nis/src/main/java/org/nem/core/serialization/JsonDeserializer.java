package org.nem.core.serialization;

import net.minidev.json.*;
import org.nem.core.utils.HexEncoder;

import java.math.*;
import java.util.*;

/**
 * A json deserializer that supports label-based lookup in addition to forward-only deserialization.
 */
public class JsonDeserializer extends Deserializer {

	private final JSONObject object;
	private final JSONArray propertyOrderArray;
	private int propertyOrderArrayIndex;

	/**
	 * Creates a new json deserializer.
	 *
	 * @param object The json object from which to read.
	 * @param context The deserialization context.
	 */
	public JsonDeserializer(final JSONObject object, final DeserializationContext context) {
		super(context);
		this.object = object;
		this.propertyOrderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
		this.propertyOrderArrayIndex = 0;
	}

	@Override
	public Integer readOptionalInt(final String label) {
		this.checkLabel(label);
		return (Integer)this.object.get(label);
	}

	@Override
	public Long readOptionalLong(final String label) {
		this.checkLabel(label);

		// the alternative to this ugly piece, is setting up JSONParser.DEFAULT_PERMISSIVE_MODE
		// in CommonStarter, but I was trying, but I don't know, how to set it up before tests :/
		//
		// additionally, readInt above, would have to be changed to:
		// ((Long)this.object.get(label)).intValue();
		final Object object = this.object.get(label);
		if (null == object) {
			return null;
		}

		if (object instanceof Integer) {
			return ((Integer)object).longValue();
		}

		return (Long)object;
	}

	@Override
	public Double readOptionalDouble(final String label) {
		this.checkLabel(label);

		final Object object = this.object.get(label);
		if (null == object) {
			return null;
		}

		if (object instanceof BigDecimal) {
			return ((BigDecimal)object).doubleValue();
		}

		return (Double)object;
	}

	@Override
	public BigInteger readOptionalBigInteger(final String label) {
		final byte[] bytes = this.readOptionalBytes(label);
		return null == bytes ? null : new BigInteger(1, bytes);
	}

	@Override
	public byte[] readOptionalBytes(final String label) {
		final String s = this.readOptionalString(label);
		if (null == s) {
			return null;
		}

		return s.isEmpty() ? new byte[] { } : HexEncoder.getBytes(s);
	}

	@Override
	public String readOptionalString(final String label) {
		this.checkLabel(label);
		return (String)this.object.get(label);
	}

	@Override
	public <T> T readOptionalObject(final String label, final ObjectDeserializer<T> activator) {
		this.checkLabel(label);
		final JSONObject childObject = (JSONObject)this.object.get(label);
		if (null == childObject) {
			return null;
		}

		return this.deserializeObject(childObject, activator);
	}

	@Override
	public <T> List<T> readOptionalObjectArray(final String label, final ObjectDeserializer<T> activator) {
		this.checkLabel(label);
		final JSONArray jsonArray = (JSONArray)this.object.get(label);

		if (null == jsonArray) {
			return null;
		}

		final List<T> objects = new ArrayList<>();
		for (final Object jsonObject : jsonArray) {
			objects.add(this.deserializeObject((JSONObject)jsonObject, activator));
		}

		return objects;
	}

	public <T> T deserializeObject(final JSONObject object, final ObjectDeserializer<T> activator) {
		final JsonDeserializer deserializer = new JsonDeserializer(object, this.getContext());
		return object.isEmpty() ? null : activator.deserialize(deserializer);
	}

	private void checkLabel(final String label) {
		if (null == this.propertyOrderArray) {
			return;
		}

		final String expectedLabel = (String)this.propertyOrderArray.get(this.propertyOrderArrayIndex++);
		if (label.equals(expectedLabel)) {
			return;
		}

		final String message = String.format(
				"expected property '%s' but request was for property '%s'",
				expectedLabel,
				label);
		throw new IllegalArgumentException(message);
	}
}
