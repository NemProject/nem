package org.nem.core.serialization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.primitive.*;
import org.nem.core.test.MockSerializableEntity;

import java.math.BigInteger;
import java.nio.*;
import java.util.*;

public class BinarySerializerTest extends SerializerTest<BinarySerializer, BinaryDeserializer> {

	@Override
	protected SerializationPolicy<BinarySerializer, BinaryDeserializer> getPolicy() {
		return new BinarySerializationPolicy();
	}

	//region Write

	@Test
	public void canWriteInt() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("int", 0x09513510);

			// Assert:
			final byte[] expectedBytes = new byte[] { 0x10, 0x35, 0x51, 0x09 };
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteLong() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeLong("long", 0xF239A033CE951350L);

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x50, 0x13, (byte)0x95, (byte)0xCE,
					0x33, (byte)0xA0, 0x39, (byte)0xF2
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteDouble() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final Double d = 0.999999999534338712692260742187500;
			serializer.writeDouble("double", d);

			// Assert:
			final byte[] expectedBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(d).array();
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteBigInteger() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final BigInteger i = new BigInteger("958A7561F014", 16);
			serializer.writeBigInteger("BigInteger", i);

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x07, 0x00, 0x00, 0x00,
					0x00, (byte)0x95, (byte)0x8A, 0x75, 0x61, (byte)0xF0, 0x14
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteUnsignedBigInteger() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final BigInteger i = new BigInteger(1, new byte[] { (byte)0x90, 0x12 });
			serializer.writeBigInteger("BigInteger", i);

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x03, 0x00, 0x00, 0x00,
					0x00, (byte)0x90, 0x12
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteBytes() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
			serializer.writeBytes("bytes", bytes);

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x05, 0x00, 0x00, 0x00,
					0x50, (byte)0xFF, 0x00, 0x7C, 0x21
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteNullBytes() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeBytes("bytes", null);

			// Assert:
			final byte[] expectedBytes = new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteEmptyBytes() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final byte[] bytes = new byte[0];
			serializer.writeBytes("bytes", bytes);

			// Assert:
			final byte[] expectedBytes = new byte[] { 0x00, 0x00, 0x00, 0x00 };
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteString() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeString("String", "BEta");

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x04, 0x00, 0x00, 0x00,
					0x42, 0x45, 0x74, 0x61
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteObject() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x13, 0x00, 0x00, 0x00,
					0x11, 0x00, 0x00, 0x00,
					0x03, 0x00, 0x00, 0x00, 0x66, 0x6F, 0x6F,
					0x2A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	@Test
	public void canWriteObjectArray() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			final List<SerializableEntity> originalObjects = new ArrayList<>();
			originalObjects.add(new MockSerializableEntity(17, "foo", 42));
			originalObjects.add(new MockSerializableEntity(111, "bar", 22));
			originalObjects.add(new MockSerializableEntity(1, "alpha", 34));
			serializer.writeObjectArray("SerializableEntity", originalObjects);

			// Assert:
			final byte[] expectedBytes = new byte[] {
					0x03, 0x00, 0x00, 0x00,
					0x13, 0x00, 0x00, 0x00,
					0x11, 0x00, 0x00, 0x00,
					0x03, 0x00, 0x00, 0x00, 0x66, 0x6F, 0x6F,
					0x2A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x13, 0x00, 0x00, 0x00,
					0x6F, 0x00, 0x00, 0x00,
					0x03, 0x00, 0x00, 0x00, 0x62, 0x61, 0x72,
					0x16, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x15, 0x00, 0x00, 0x00,
					0x01, 0x00, 0x00, 0x00,
					0x05, 0x00, 0x00, 0x00, 0x61, 0x6C, 0x70, 0x68, 0x61,
					0x22, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			};
			Assert.assertThat(serializer.getBytes(), IsEqual.equalTo(expectedBytes));
		}
	}

	//endregion

	//region HasMoreData

	@Test
	public void deserializerInitiallyHasMoreData() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("int", 0x09513510);

			try (BinaryDeserializer deserializer = this.createBinaryDeserializer(serializer.getBytes())) {
				// Assert:
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(true));
			}
		}
	}

	//endregion

	//region availableBytes

	@Test
	public void availableBytesReturnsNumberOfAvailableBytes() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("int", 0x09513510);
			serializer.writeLong("long", 0x0951351001234567L);

			try (BinaryDeserializer deserializer = this.createBinaryDeserializer(serializer.getBytes())) {
				// Assert:
				Assert.assertThat(deserializer.availableBytes(), IsEqual.equalTo(12));
				deserializer.readInt("int");
				Assert.assertThat(deserializer.availableBytes(), IsEqual.equalTo(8));
				deserializer.readLong("long");
				Assert.assertThat(deserializer.availableBytes(), IsEqual.equalTo(0));
			}
		}
	}

	//endregion

	//region Corrupt Data Handling

	@Test(expected = SerializationException.class)
	public void readOfPrimitiveTypeFailsIfStreamIsTooSmall() throws Exception {
		final byte[] bytes = new byte[] { 1, 2, 4 };
		try (BinaryDeserializer deserializer = this.createBinaryDeserializer(bytes)) {
			// Assert:
			deserializer.readInt("int");
		}
	}

	@Test(expected = SerializationException.class)
	public void readOfVariableSizedTypeFailsIfStreamIsTooSmall() throws Exception {
		final byte[] bytes = new byte[] { 0x02, 0x00, 0x00, 0x00, 0x01 };
		try (BinaryDeserializer deserializer = this.createBinaryDeserializer(bytes)) {
			// Assert:
			deserializer.readBytes("bytes");
		}
	}

	private BinaryDeserializer createBinaryDeserializer(final byte[] bytes) {
		return new BinaryDeserializer(bytes, null);
	}

	//endregion

	//region serializeToBytes

	@Test
	public void serializeToBytesProducesSameBytesAsEntitySerialize() throws Exception {
		// Arrange:
		try (final BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			final SerializableEntity entity = new MockSerializableEntity(17, "foo", 42);
			entity.serialize(serializer);
			final byte[] writeObjectBytes = serializer.getBytes();

			// Assert:
			Assert.assertThat(BinarySerializer.serializeToBytes(entity), IsEqual.equalTo(writeObjectBytes));
		}
	}

	//endregion
}
