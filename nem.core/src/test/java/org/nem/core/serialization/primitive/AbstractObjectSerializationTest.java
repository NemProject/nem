package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;

public abstract class AbstractObjectSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, MockSerializableEntity> {
	public AbstractObjectSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected MockSerializableEntity getValue() {
		return new MockSerializableEntity(17, "foo", 42);
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeObject(label, null);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final MockSerializableEntity value) {
		serializer.writeObject(label, value);
	}

	@Override
	protected MockSerializableEntity readValue(final Deserializer deserializer, final String label) {
		return deserializer.readObject(label, MockSerializableEntity::new);
	}

	@Override
	protected MockSerializableEntity readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalObject(label, MockSerializableEntity::new);
	}
}
