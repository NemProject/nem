package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public class BinarySerializationPolicy extends SerializationPolicy<BinarySerializer, BinaryDeserializer> {

	@Override
	public BinarySerializer createSerializer(final SerializationContext context) {
		return new BinarySerializer(context);
	}

	@Override
	public BinaryDeserializer createDeserializer(final BinarySerializer serializer, final DeserializationContext context) {
		return new BinaryDeserializer(serializer.getBytes(), context);
	}
}
