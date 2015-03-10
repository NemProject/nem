package org.nem.core.serialization.primitive;

import org.junit.Test;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;

public abstract class AbstractStringSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, String> {
	public AbstractStringSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Test
	public void canRoundtripUtf8String() {
		// Assert:
		this.assertCanRoundtrip("zuação danada");
	}

	@Test
	public void cannotRoundtripRequiredEmptyString() {
		// Act:
		this.assertStringCannotBeRoundTripped("");
	}

	@Test
	public void cannotRoundtripRequiredWhitespaceString() {
		// Act:
		this.assertStringCannotBeRoundTripped("  \t \t");
	}

	private void assertStringCannotBeRoundTripped(final String s) {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", s);

		final Deserializer deserializer = this.createDeserializer(serializer);
		ExceptionAssert.assertThrowsMissingPropertyException(
				() -> deserializer.readString("String"),
				"String");
	}

	@Override
	protected String getValue() {
		return "BEta GaMMa";
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeString(label, null);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final String value) {
		serializer.writeString(label, value);
	}

	@Override
	protected String readValue(final Deserializer deserializer, final String label) {
		return deserializer.readString(label);
	}

	@Override
	protected String readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalString(label);
	}
}
