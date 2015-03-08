package org.nem.core.serialization.primitive;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;

public abstract class AbstractPrimitiveSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer, T> {
	private final SerializationPolicy<TSerializer, TDeserializer> policy;

	public AbstractPrimitiveSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		this.policy = policy;
	}

	@Test
	public void canRoundtripValue() {
		// Assert:
		this.assertCanRoundtrip(this.getValue());
	}

	protected void assertCanRoundtrip(final T originalValue) {
		// Assert:
		this.assertCanRoundtrip(originalValue, originalValue);
	}

	protected void assertCanRoundtrip(final T originalValue, final T expectedValue) {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val");

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(expectedValue));
	}

	@Test
	public void canReadOptionalNullValue() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		final Deserializer deserializer = this.createDeserializer(serializer);

		// Act:
		final T value = this.readOptionalValue(deserializer, "value");

		// Assert:
		Assert.assertThat(value, IsNull.nullValue());
	}

	@Test
	public void cannotReadRequiredNullValue() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		final Deserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		ExceptionAssert.assertThrowsMissingPropertyException(
				() -> this.readValue(deserializer, "value"),
				"value");
	}

	@Test
	public void canRoundtripOptionalNullValue() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		this.writeNullValue(serializer, "value");
		final Deserializer deserializer = this.createDeserializer(serializer);

		// Act:
		final T value = this.readOptionalValue(deserializer, "value");

		// Assert:
		Assert.assertThat(value, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullValue() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		this.writeNullValue(serializer, "value");
		final Deserializer deserializer = this.createDeserializer(serializer);

		// Act:
		ExceptionAssert.assertThrowsMissingPropertyException(
				() -> this.readValue(deserializer, "value"),
				"value");
	}

	protected abstract T getValue();

	protected abstract void writeNullValue(final Serializer serializer, final String label);

	protected abstract void writeValue(final Serializer serializer, final String label, final T value);

	protected abstract T readValue(final Deserializer deserializer, final String label);

	protected abstract T readOptionalValue(final Deserializer deserializer, final String label);

	protected TSerializer createSerializer() {
		return this.policy.createSerializer();
	}

	protected Deserializer createDeserializer(final TSerializer serializer) {
		return this.policy.createDeserializer(serializer);
	}
}
