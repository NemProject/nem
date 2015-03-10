package org.nem.core.serialization.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;

import java.math.BigInteger;

public abstract class AbstractBigIntegerSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, BigInteger> {
	public AbstractBigIntegerSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Test
	public void canRoundtripPrefixedUnsignedBigInteger() {
		// Arrange:
		final BigInteger i = new BigInteger(1, new byte[] { (byte)0x90, 0x12 });

		// Assert:
		this.assertCanRoundtrip(i);

		// Sanity:
		Assert.assertThat(3, IsEqual.equalTo(i.toByteArray().length));
	}

	@Test
	public void canRoundtripNonPrefixedUnsignedBigInteger() throws Exception {
		// Arrange:
		final BigInteger i = new BigInteger(new byte[] { (byte)0x90, 0x12 });

		// Assert:
		this.assertCanRoundtrip(i, new BigInteger(1, new byte[] { (byte)0x90, 0x12 }));

		// Sanity:
		Assert.assertThat(2, IsEqual.equalTo(i.toByteArray().length));
	}

	@Override
	protected BigInteger getValue() {
		return new BigInteger("958A7561F014", 16);
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
		serializer.writeBigInteger(label, null);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final BigInteger value) {
		serializer.writeBigInteger(label, value);
	}

	@Override
	protected BigInteger readValue(final Deserializer deserializer, final String label) {
		return deserializer.readBigInteger(label);
	}

	@Override
	protected BigInteger readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalBigInteger(label);
	}
}
