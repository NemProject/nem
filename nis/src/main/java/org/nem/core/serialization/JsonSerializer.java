package org.nem.core.serialization;

import net.minidev.json.*;
import org.nem.core.utils.*;

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A json serializer that supports forward-only serialization.
 */
public class JsonSerializer extends Serializer {

	public static final String PROPERTY_ORDER_ARRAY_NAME = "_propertyOrderArray";

	private final JSONArray propertyOrderArray;
	private final JSONObject object;

	/**
	 * Creates a default json serializer that does not enforce forward-only reads.
	 */
	public JsonSerializer() {
		this(null);
	}

	/**
	 * Creates a default json serializer that does not enforce forward-only reads.
	 *
	 * @param context The serialization context to use.
	 */
	public JsonSerializer(final SerializationContext context) {
		this(context, false);
	}

	/**
	 * Creates a default json serializer that can conditionally enforce forward-only reads.
	 * For performance reasons, this feature should not be used in production code.
	 *
	 * @param enforceReadWriteOrder true if forward-only reads should be enforced.
	 */
	public JsonSerializer(final boolean enforceReadWriteOrder) {
		this(null, enforceReadWriteOrder);
	}

	private JsonSerializer(final SerializationContext context, final boolean enforceReadWriteOrder) {
		super(context);
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
	public void writeDouble(final String label, final double d) {
		this.pushLabel(label);
		this.object.put(label, d);
	}

	@Override
	public void writeBigInteger(final String label, final BigInteger i) {
		this.writeBytes(label, null == i ? null : i.toByteArray());
	}

	@Override
	protected void writeBytesImpl(final String label, final byte[] bytes) {
		final String s = null == bytes ? null : HexEncoder.getString(bytes);
		this.writeStringUnchecked(label, s);
	}

	@Override
	protected void writeStringImpl(final String label, final String s) {
		this.writeStringUnchecked(label, s);
	}

	private void writeStringUnchecked(final String label, final String s) {
		this.pushLabel(label);
		this.object.put(label, s);
	}

	@Override
	public void writeObject(final String label, final SerializableEntity object) {
		this.pushLabel(label);
		this.object.put(label, this.serializeObject(object));
	}

	@Override
	public void writeObjectArray(final String label, final Collection<? extends SerializableEntity> objects) {
		this.pushLabel(label);
		if (null == objects) {
			return;
		}

		final JSONArray jsonObjects = objects.stream()
				.map(this::serializeObject)
				.collect(Collectors.toCollection(JSONArray::new));

		this.object.put(label, jsonObjects);
	}

	private JSONObject serializeObject(final SerializableEntity object) {
		if (null == object) {
			return new JSONObject();
		}

		final JsonSerializer serializer = new JsonSerializer(this.getContext(), null != this.propertyOrderArray);
		object.serialize(serializer);
		return serializer.getObject();
	}

	/**
	 * Gets the underlying JSON object.
	 *
	 * @return The underlying JSON object.
	 */
	public JSONObject getObject() {
		if (null != this.propertyOrderArray) {
			this.object.put(PROPERTY_ORDER_ARRAY_NAME, this.propertyOrderArray);
		}

		return this.object;
	}

	private void pushLabel(final String label) {
		if (null == this.propertyOrderArray) {
			return;
		}

		this.propertyOrderArray.add(label);
	}

	/**
	 * Helper function that serializes a SerializableEntity to a JSON object.
	 *
	 * @param entity The entity to serialize.
	 * @return The resulting JSON object.
	 */
	public static JSONObject serializeToJson(final SerializableEntity entity) {
		final JsonSerializer serializer = new JsonSerializer();
		entity.serialize(serializer);
		return serializer.getObject();
	}

	/**
	 * Helper function that serializes a SerializableEntity to a byte array.
	 *
	 * @param entity The entity to serialize.
	 * @return The resulting byte array.
	 */
	public static byte[] serializeToBytes(final SerializableEntity entity) {
		return StringEncoder.getBytes(serializeToJson(entity).toJSONString());
	}
}
