package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

public abstract class AbstractTruncatedStringSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractStringSerializationTest<TSerializer, TDeserializer> {
	private static final int LIMIT = 20;

	public AbstractTruncatedStringSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeString(label, null, LIMIT);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final String value) {
		serializer.writeString(label, value, LIMIT);
	}

	@Override
	protected String readValue(final Deserializer deserializer, final String label) {
		return deserializer.readString(label, LIMIT);
	}

	@Override
	protected String readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalString(label, LIMIT);
	}
}
