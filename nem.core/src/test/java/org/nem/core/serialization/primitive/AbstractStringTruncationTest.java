package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public abstract class AbstractStringTruncationTest<TSerializer extends Serializer, TDeserializer extends Deserializer> extends AbstractPrimitiveTruncationTest<TSerializer, TDeserializer, String> {

	public AbstractStringTruncationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		super(policy);
	}

	@Override
	protected String getValue(final int length) {
		final byte[] bytes = Utils.generateRandomBytes(length);
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			final char c = (char)('a' + (Math.abs(bytes[i] % 26)));
			builder.append(c);
		}

		return builder.toString();
	}

	@Override
	protected String getTruncatedValue(final String value, final int limit) {
		return value.substring(0, limit);
	}

	@Override
	protected int getSize(final String value) {
		return value.length();
	}

	@Override
	protected int getDefaultLimit() {
		return 128;
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final String value, final int limit) {
		serializer.writeString(label, value, limit);
	}

	@Override
	protected String readValue(final Deserializer deserializer, final String label, final int limit) {
		return deserializer.readString(label, limit);
	}

	@Override
	protected void writeValue(final Serializer serializer, final String label, final String value) {
		serializer.writeString(label, value);
	}

	@Override
	protected String readValue(final Deserializer deserializer, final String label) {
		return deserializer.readString(label);
	}
}
