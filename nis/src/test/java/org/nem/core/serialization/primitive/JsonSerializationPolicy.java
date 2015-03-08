package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public class JsonSerializationPolicy extends SerializationPolicy<JsonSerializer, JsonDeserializer> {

	@Override
	public JsonSerializer createSerializer() {
		return new JsonSerializer();
	}

	@Override
	public JsonDeserializer createDeserializer(final JsonSerializer serializer, final DeserializationContext context) {
		return new JsonDeserializer(serializer.getObject(), context);
	}
}
