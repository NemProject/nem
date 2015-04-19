package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public abstract class AbstractLongSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, Long> {
	public AbstractLongSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected Long getValue() {
		return 0xF239A033CE951350L;
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final Long value) {
		serializer.writeLong(label, value);
	}

	@Override
	protected Long readValue(final Deserializer deserializer, final String label) {
		return deserializer.readLong(label);
	}

	@Override
	protected Long readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalLong(label);
	}
}
