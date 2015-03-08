package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public class BinarySerializationPolicy extends SerializationPolicy<BinarySerializer, BinaryDeserializer> {

	@Override
	protected BinarySerializer createSerializer() {
		return new BinarySerializer();
	}

	@Override
	protected BinaryDeserializer createDeserializer(final BinarySerializer serializer, final DeserializationContext context) {
		return new BinaryDeserializer(serializer.getBytes(), context);
	}
}
