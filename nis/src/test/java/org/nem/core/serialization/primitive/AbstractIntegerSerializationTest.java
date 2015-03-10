package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public abstract class AbstractIntegerSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, Integer> {
	public AbstractIntegerSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected Integer getValue() {
		return 0x09513510;
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final Integer value) {
		serializer.writeInt(label, value);
	}

	@Override
	protected Integer readValue(final Deserializer deserializer, final String label) {
		return deserializer.readInt(label);
	}

	@Override
	protected Integer readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalInt(label);
	}
}
