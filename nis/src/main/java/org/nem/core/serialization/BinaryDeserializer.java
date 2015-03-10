package org.nem.core.serialization;

import org.nem.core.utils.StringEncoder;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

/**
 * A binary deserializer that supports forward-only deserialization.
 */
public class BinaryDeserializer extends Deserializer implements AutoCloseable {

	private final ByteArrayInputStream stream;

	/**
	 * Creates a new binary deserializer.
	 *
	 * @param bytes The byte array from which to read.
	 * @param context The deserialization context.
	 */
	public BinaryDeserializer(final byte[] bytes, final DeserializationContext context) {
		super(context);
		this.stream = new ByteArrayInputStream(bytes);
	}

	@Override
	public Integer readOptionalInt(final String label) {
		return this.readIfNotEmpty(() -> {
			final byte[] bytes = this.readBytes(4);
			return bytes[0] & 0x000000FF
					| (bytes[1] << 8) & 0x0000FF00
					| (bytes[2] << 16) & 0x00FF0000
					| (bytes[3] << 24) & 0xFF000000;
		});
	}

	@Override
	public Long readOptionalLong(final String label) {
		return this.readIfNotEmpty(() -> {
			final long lowPart = this.readInt(label);
			final long highPart = this.readInt(label);
			return lowPart & 0x00000000FFFFFFFFL
					| (highPart << 32) & 0xFFFFFFFF00000000L;
		});
	}

	@Override
	public Double readOptionalDouble(final String label) {
		return this.readIfNotEmpty(() -> Double.longBitsToDouble(this.readLong(label)));
	}

	@Override
	public BigInteger readOptionalBigInteger(final String label) {
		return this.readIfNotEmpty(() -> {
			final byte[] bytes = this.readOptionalBytes(label);
			return null == bytes ? null : new BigInteger(1, bytes);
		});
	}

	@Override
	protected byte[] readOptionalBytesImpl(final String label) {
		return this.readOptionalBytesUnchecked(label);
	}

	private byte[] readOptionalBytesUnchecked(final String label) {
		return this.readIfNotEmpty(() -> {
			final int numBytes = this.readInt(label);
			return BinarySerializer.NULL_BYTES_SENTINEL_VALUE == numBytes ? null : this.readBytes(numBytes);
		});
	}

	@Override
	protected String readOptionalStringImpl(final String label) {
		return this.readIfNotEmpty(() -> {
			final byte[] bytes = this.readOptionalBytes(label);
			return null == bytes ? null : StringEncoder.getString(bytes);
		});
	}

	@Override
	public <T> T readOptionalObject(final String label, final ObjectDeserializer<T> activator) {
		return this.readIfNotEmpty(() -> this.deserializeObject(label, activator));
	}

	@Override
	public <T> List<T> readOptionalObjectArray(final String label, final ObjectDeserializer<T> activator) {
		return this.readIfNotEmpty(() -> {
			final int numObjects = this.readInt(label);
			if (BinarySerializer.NULL_BYTES_SENTINEL_VALUE == numObjects) {
				return null;
			}

			final List<T> objects = new ArrayList<>();
			for (int i = 0; i < numObjects; ++i) {
				objects.add(this.deserializeObject(label, activator));
			}

			return objects;
		});
	}

	@Override
	public void close() throws Exception {
		this.stream.close();
	}

	private <T> T deserializeObject(final String label, final ObjectDeserializer<T> activator) {
		try {
			final byte[] bytes = this.readOptionalBytesUnchecked(label);
			if (0 == bytes.length) {
				return null;
			}

			try (BinaryDeserializer deserializer = new BinaryDeserializer(bytes, this.getContext())) {
				return activator.deserialize(deserializer);
			}
		} catch (final Exception ex) {
			throw new SerializationException(ex);
		}
	}

	/**
	 * Determines if there is more data left to read.
	 *
	 * @return true if there is more data left to read.
	 */
	public boolean hasMoreData() {
		return 0 != this.stream.available();
	}

	/**
	 * Gets the number of unread bytes in the buffer.
	 *
	 * @return The number of unread bytes.
	 */
	public int availableBytes() {
		return this.stream.available();
	}

	private <T> T readIfNotEmpty(final Supplier<T> supplier) {
		return this.hasMoreData() ? supplier.get() : null;
	}

	private byte[] readBytes(final int numBytes) {
		if (this.stream.available() < numBytes) {
			throw new SerializationException("unexpected end of stream reached");
		}

		if (0 == numBytes) {
			return new byte[0];
		}

		try {
			final byte[] bytes = new byte[numBytes];
			final int numBytesRead = this.stream.read(bytes);
			if (numBytesRead != numBytes) {
				throw new SerializationException("unexpected end of stream reached");
			}

			return bytes;
		} catch (final IOException e) {
			throw new SerializationException(e);
		}
	}
}
