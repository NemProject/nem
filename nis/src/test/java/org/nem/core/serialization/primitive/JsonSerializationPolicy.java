package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public class JsonSerializationPolicy extends SerializationPolicy<JsonSerializer, JsonDeserializer> {

	@Override
	protected JsonSerializer createSerializer() {
		return new JsonSerializer();
	}

	@Override
	protected JsonDeserializer createDeserializer(final JsonSerializer serializer, final DeserializationContext context) {
		return new JsonDeserializer(serializer.getObject(), context);
	}
}
