package org.nem.core.serialization;

import org.nem.core.utils.StringEncoder;

import java.io.*;
import java.math.BigInteger;
import java.util.Collection;

/**
 * A binary serializer that supports forward-only serialization.
 */
public class BinarySerializer extends Serializer implements AutoCloseable {

	/**
	 * Sentinel value used to indicate that a serialized byte array should be deserialized as null.
	 */
	public static final int NULL_BYTES_SENTINEL_VALUE = 0xFFFFFFFF;

	private final ByteArrayOutputStream stream;

	/**
	 * Creates a new binary serializer.
	 */
	public BinarySerializer() {
		this(null);
	}

	/**
	 * Creates a new binary serializer.
	 *
	 * @param context The serialization context to use.
	 */
	public BinarySerializer(final SerializationContext context) {
		super(context);
		this.stream = new ByteArrayOutputStream();
	}

	@Override
	public void writeInt(final String label, final int i) {
		final byte[] bytes = {
				(byte)(i & 0xFF),
				(byte)((i >> 8) & 0xFF),
				(byte)((i >> 16) & 0xFF),
				(byte)((i >> 24) & 0xFF),
		};
		this.writeBytesInternal(bytes);
	}

	@Override
	public void writeLong(final String label, final long l) {
		this.writeInt(label, (int)l);
		this.writeInt(label, (int)(l >> 32));
	}

	@Override
	public void writeDouble(final String label, final double d) {
		this.writeLong(label, Double.doubleToLongBits(d));
	}

	@Override
	public void writeBigInteger(final String label, final BigInteger i) {
		this.writeBytes(label, null == i ? null : i.toByteArray());
	}

	@Override
	protected void writeBytesImpl(final String label, final byte[] bytes) {
		this.writeBytesUnchecked(label, bytes);
	}

	private void writeBytesUnchecked(final String label, final byte[] bytes) {
		if (null == bytes) {
			this.writeInt(label, NULL_BYTES_SENTINEL_VALUE);
		} else {
			this.writeInt(label, bytes.length);
			this.writeBytesInternal(bytes);
		}
	}

	@Override
	protected void writeStringImpl(final String label, final String s) {
		this.writeBytes(label, null == s ? null : StringEncoder.getBytes(s));
	}

	@Override
	public void writeObject(final String label, final SerializableEntity object) {
		this.writeBytesUnchecked(label, this.serializeObject(object));
	}

	@Override
	public void writeObjectArray(final String label, final Collection<? extends SerializableEntity> objects) {
		if (null == objects) {
			this.writeInt(label, NULL_BYTES_SENTINEL_VALUE);
			return;
		}

		this.writeInt(label, objects.size());
		for (final SerializableEntity object : objects) {
			this.writeBytesUnchecked(label, this.serializeObject(object));
		}
	}

	@Override
	public void close() throws IOException {
		this.stream.close();
	}

	private byte[] serializeObject(final SerializableEntity object) {
		if (null == object) {
			return new byte[0];
		}

		try {
			try (BinarySerializer serializer = new BinarySerializer(this.getContext())) {
				object.serialize(serializer);
				return serializer.getBytes();
			}
		} catch (final Exception ex) {
			throw new SerializationException(ex);
		}
	}

	/**
	 * Gets the underlying byte buffer.
	 *
	 * @return The underlying byte buffer.
	 */
	public byte[] getBytes() {
		return this.stream.toByteArray();
	}

	private void writeBytesInternal(final byte[] bytes) {
		this.stream.write(bytes, 0, bytes.length);
	}

	/**
	 * Helper function that serializes a SerializableEntity to a byte array.
	 *
	 * @param entity The entity to serialize.
	 * @return The resulting byte array.
	 */
	public static byte[] serializeToBytes(final SerializableEntity entity) {
		try {
			try (BinarySerializer binarySerializer = new BinarySerializer()) {
				entity.serialize(binarySerializer);
				return binarySerializer.getBytes();
			}
		} catch (final Exception e) {
			throw new SerializationException(e);
		}
	}
}
