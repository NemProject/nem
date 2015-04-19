package org.nem.core.serialization.primitive;

import org.junit.Test;
import org.nem.core.serialization.*;

public abstract class AbstractDoubleSerializationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveSerializationTest<TSerializer, TDeserializer, Double> {
	public AbstractDoubleSerializationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Test
	public void canRoundtripDoubleNaN() {
		// Assert:
		this.assertCanRoundtrip(Double.NaN);
	}

	@Override
	protected Double getValue() {
		return 0.999999999534338712692260742187500;
	}

	@Override
	protected void writeNullValue(final Serializer serializer, final String label) {
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final Double value) {
		serializer.writeDouble(label, value);
	}

	@Override
	protected Double readValue(final Deserializer deserializer, final String label) {
		return deserializer.readDouble(label);
	}

	@Override
	protected Double readOptionalValue(final Deserializer deserializer, final String label) {
		return deserializer.readOptionalDouble(label);
	}
}
