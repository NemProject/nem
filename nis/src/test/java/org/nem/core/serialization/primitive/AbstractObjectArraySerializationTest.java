package org.nem.core.serialization.primitive;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockSerializableEntity;

import java.util.*;

public abstract class AbstractObjectArraySerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, List<MockSerializableEntity>> {
	public AbstractObjectArraySerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Test
	public void canRoundtripArrayContainingNullValue() {
		// Arrange:
		final List<MockSerializableEntity> objects = this.getValue();
		objects.set(1, null);

		// Assert:
		this.assertCanRoundtrip(objects);

		// Sanity:
		Assert.assertThat(objects.get(1), IsNull.nullValue());
	}

	@Test
	public void canRoundtripEmptyArray() {
		// Assert:
		this.assertCanRoundtrip(new ArrayList<>());
	}

	@Override
	protected List<MockSerializableEntity> getValue() {
		final List<MockSerializableEntity> objects = new ArrayList<>();
		objects.add(new MockSerializableEntity(17, "foo", 42));
		objects.add(new MockSerializableEntity(111, "bar", 22));
		objects.add(new MockSerializableEntity(1, "alpha", 34));
		return objects;
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeObjectArray(label, null);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final List<MockSerializableEntity> value) {
		serializer.writeObjectArray(label, value);
	}

	@Override
	protected List<MockSerializableEntity> readValue(final Deserializer deserializer, final String label) {
		return deserializer.readObjectArray(label, MockSerializableEntity::new);
	}

	@Override
	protected List<MockSerializableEntity> readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalObjectArray(label, MockSerializableEntity::new);
	}
}
