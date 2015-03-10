package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public abstract class AbstractTruncatedBytesSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractBytesSerializationTest<TSerializer, TDeserializer> {
	private static final int LIMIT = 20;

	public AbstractTruncatedBytesSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeBytes(label, null, LIMIT);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final byte[] value) {
		serializer.writeBytes(label, value, LIMIT);
	}

	@Override
	protected byte[] readValue(final Deserializer deserializer, final String label) {
		return deserializer.readBytes(label, LIMIT);
	}

	@Override
	protected byte[] readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalBytes(label, LIMIT);
	}
}