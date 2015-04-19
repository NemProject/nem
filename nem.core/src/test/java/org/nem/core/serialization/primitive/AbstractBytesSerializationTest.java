package org.nem.core.serialization.primitive;

import org.junit.Test;
import org.nem.core.serialization.*;

public abstract class AbstractBytesSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, byte[]> {
	public AbstractBytesSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Test
	public void canRoundtripEmptyBytes() {
		// Assert:
		this.assertCanRoundtrip(new byte[] {});
	}

	@Override
	protected byte[] getValue() {
		return new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeBytes(label, null);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final byte[] value) {
		serializer.writeBytes(label, value);
	}

	@Override
	protected byte[] readValue(final Deserializer deserializer, final String label) {
		return deserializer.readBytes(label);
	}

	@Override
	protected byte[] readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalBytes(label);
	}
}
