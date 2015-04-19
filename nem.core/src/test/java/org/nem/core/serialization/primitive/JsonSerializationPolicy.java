package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public class JsonSerializationPolicy extends SerializationPolicy<JsonSerializer, JsonDeserializer> {

	@Override
	public JsonSerializer createSerializer(final SerializationContext context) {
		return new JsonSerializer(context);
	}

	@Override
	public JsonDeserializer createDeserializer(final JsonSerializer serializer, final DeserializationContext context) {
		return new JsonDeserializer(serializer.getObject(), context);
	}
}
