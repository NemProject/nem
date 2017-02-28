package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.util.Arrays;

public abstract class AbstractBytesTruncationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveTruncationTest<TSerializer, TDeserializer, byte[]> {

	public AbstractBytesTruncationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected byte[] getValue(final int length) {
		return Utils.generateRandomBytes(length);
	}

	@Override
	protected byte[] getTruncatedValue(final byte[] value, final int limit) {
		return Arrays.copyOf(value, limit);
	}

	@Override
	protected int getSize(final byte[] value) {
		return value.length;
	}

	@Override
	protected int getDefaultLimit() {
		return 2048;
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final byte[] value, final int limit) {
		serializer.writeBytes(label, value, limit);
	}

	@Override
	protected byte[] readValue(final Deserializer deserializer, final String label, final int limit) {
		return deserializer.readBytes(label, limit);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final byte[] value) {
		serializer.writeBytes(label, value);
	}

	@Override
	protected byte[] readValue(final Deserializer deserializer, final String label) {
		return deserializer.readBytes(label);
	}
}
