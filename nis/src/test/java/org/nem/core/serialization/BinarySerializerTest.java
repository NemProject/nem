package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.math.BigInteger;
import java.util.*;

public class BinarySerializerTest {

	//region Write

	@Test
	public void canWriteInt() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
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
		try (BinarySerializer serializer = new BinarySerializer()) {
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
	public void canWriteBigInteger() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
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
	public void canWriteBytes() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
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
		try (BinarySerializer serializer = new BinarySerializer()) {
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
		try (BinarySerializer serializer = new BinarySerializer()) {
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
		try (BinarySerializer serializer = new BinarySerializer()) {
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
		try (BinarySerializer serializer = new BinarySerializer()) {

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
		try (BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			List<SerializableEntity> originalObjects = new ArrayList<>();
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

	//region Roundtrip

	@Test
	public void canRoundtripInt() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("int", 0x09513510);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final int i = deserializer.readInt("int");

				// Assert:
				Assert.assertThat(i, IsEqual.equalTo(0x09513510));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripLong() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeLong("long", 0xF239A033CE951350L);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final long l = deserializer.readLong("long");

				// Assert:
				Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripBigInteger() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final BigInteger i = new BigInteger("958A7561F014", 16);
			serializer.writeBigInteger("BigInteger", i);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

				// Assert:
				Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripBytes() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
			serializer.writeBytes("bytes", bytes);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final byte[] readBytes = deserializer.readBytes("bytes");

				// Assert:
				Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripNullBytes() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeBytes("bytes", null);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final byte[] readBytes = deserializer.readBytes("bytes");

				// Assert:
				Assert.assertThat(readBytes, IsEqual.equalTo(null));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripEmptyBytes() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			final byte[] bytes = new byte[0];
			serializer.writeBytes("bytes", bytes);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final byte[] readBytes = deserializer.readBytes("bytes");

				// Assert:
				Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripString() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeString("String", "BEta GaMMa");

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final String s = deserializer.readString("String");

				// Assert:
				Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(false));
			}
		}
	}

	@Test
	public void canRoundtripObject() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

				// Assert:
				Assert.assertThat(object, IsEqual.equalTo(new MockSerializableEntity(17, "foo", 42)));
			}
		}
	}

	@Test
	public void canRoundtripNullObject() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			serializer.writeObject("SerializableEntity", null);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

				// Assert:
				Assert.assertThat(object, IsEqual.equalTo(null));
			}
		}
	}

	@Test
	public void canRoundtripObjectArray() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			List<SerializableEntity> originalObjects = new ArrayList<>();
			originalObjects.add(new MockSerializableEntity(17, "foo", 42));
			originalObjects.add(new MockSerializableEntity(111, "bar", 22));
			originalObjects.add(new MockSerializableEntity(1, "alpha", 34));

			// Act:
			serializer.writeObjectArray("SerializableArray", originalObjects);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

				// Assert:
				Assert.assertThat(objects.size(), IsEqual.equalTo(3));
				for (int i = 0; i < objects.size(); ++i)
					Assert.assertThat(objects.get(i), IsEqual.equalTo(originalObjects.get(i)));
			}
		}
	}

	@Test
	public void canRoundtripArrayContainingNullValue() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			List<SerializableEntity> originalObjects = new ArrayList<>();
			originalObjects.add(null);

			// Act:
			serializer.writeObjectArray("SerializableArray", originalObjects);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

				// Assert:
				Assert.assertThat(objects.size(), IsEqual.equalTo(1));
				Assert.assertThat(objects.get(0), IsEqual.equalTo(null));
			}
		}
	}

	//endregion

	//region Roundtrip Multiple

	@Test
	public void canRoundtripMultipleValues() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("alpha", 0x09513510);
			serializer.writeLong("zeta", 0xF239A033CE951350L);
			serializer.writeBytes("beta", new byte[] { 2, 4, 6 });
			serializer.writeObject("object", new MockSerializableEntity(7, "foo", 5));
			serializer.writeInt("gamma", 7);
			serializer.writeString("epsilon", "FooBar");
			serializer.writeObjectArray("entities", Arrays.asList(
					new MockSerializableEntity(5, "ooo", 62),
					new MockSerializableEntity(8, "ala", 15)
			));
			serializer.writeBigInteger("bi", new BigInteger("14"));
			serializer.writeLong("sigma", 8);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				// Assert:
				Assert.assertThat(deserializer.readInt("alpha"), IsEqual.equalTo(0x09513510));
				Assert.assertThat(deserializer.readLong("zeta"), IsEqual.equalTo(0xF239A033CE951350L));
				Assert.assertThat(deserializer.readBytes("beta"), IsEqual.equalTo(new byte[] { 2, 4, 6 }));

				final MockSerializableEntity entity = deserializer.readObject(
						"object",
						new MockSerializableEntity.Activator());
				Assert.assertThat(entity, IsEqual.equalTo(new MockSerializableEntity(7, "foo", 5)));

				Assert.assertThat(deserializer.readInt("gamma"), IsEqual.equalTo(7));
				Assert.assertThat(deserializer.readString("epsilon"), IsEqual.equalTo("FooBar"));

				final List<MockSerializableEntity> entities = deserializer.readObjectArray(
						"entities",
						new MockSerializableEntity.Activator());
				Assert.assertThat(entities.get(0), IsEqual.equalTo(new MockSerializableEntity(5, "ooo", 62)));
				Assert.assertThat(entities.get(1), IsEqual.equalTo(new MockSerializableEntity(8, "ala", 15)));

				Assert.assertThat(deserializer.readBigInteger("bi"), IsEqual.equalTo(new BigInteger("14")));
				Assert.assertThat(deserializer.readLong("sigma"), IsEqual.equalTo(8L));
			}
		}
	}

	//endregion

	//region Context

	@Test
	public void contextPassedToDeserializerConstructorIsUsed() throws Exception {
		// Arrange:
		DeserializationContext context = new DeserializationContext(new MockAccountLookup());

		// Act:
		try (BinaryDeserializer deserializer = new BinaryDeserializer(new byte[] { }, context)) {
			// Assert:
			Assert.assertThat(deserializer.getContext(), IsEqual.equalTo(context));
		}
	}

	@Test
	public void contextPassedToDeserializerConstructorIsPassedToChildDeserializer() throws Exception {
		// Arrange:
		DeserializationContext context = new DeserializationContext(new MockAccountLookup());
		try (BinarySerializer serializer = new BinarySerializer()) {
			serializer.writeObject("test", new MockSerializableEntity(7, "a", 12));

			// Act:
			try (BinaryDeserializer deserializer = new BinaryDeserializer(serializer.getBytes(), context)) {
				MockSerializableEntity.Activator objectDeserializer = new MockSerializableEntity.Activator();
				deserializer.readObject("test", objectDeserializer);

				// Assert:
				Assert.assertThat(objectDeserializer.getLastContext(), IsEqual.equalTo(context));
			}
		}
	}

	//endregion

	//region HasMoreData

	@Test
	public void deserializerInitiallyHasMoreData() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {
			// Act:
			serializer.writeInt("int", 0x09513510);

			try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
				// Assert:
				Assert.assertThat(deserializer.hasMoreData(), IsEqual.equalTo(true));
			}
		}
	}

	//endregion

	//region Corrupt Data Handling

	@Test(expected = SerializationException.class)
	public void readOfPrimitiveTypeFailsIfStreamIsTooSmall() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 4 };
		try (BinaryDeserializer deserializer = createBinaryDeserializer(bytes)) {
			// Assert:
			deserializer.readInt("int");
		}
	}

	@Test(expected = SerializationException.class)
	public void readOfVariableSizedTypeFailsIfStreamIsTooSmall() throws Exception {
		byte[] bytes = new byte[] { 0x02, 0x00, 0x00, 0x00, 0x01 };
		try (BinaryDeserializer deserializer = createBinaryDeserializer(bytes)) {
			// Assert:
			deserializer.readBytes("bytes");
		}
	}

	//endregion

	//region serializeToBytes

	@Test
	public void serializeToBytesProducesSameBytesAsEntitySerialize() throws Exception {
		// Arrange:
		try (BinarySerializer serializer = new BinarySerializer()) {

			// Act:
			SerializableEntity entity = new MockSerializableEntity(17, "foo", 42);
			entity.serialize(serializer);
			byte[] writeObjectBytes = serializer.getBytes();

			// Assert:
			Assert.assertThat(BinarySerializer.serializeToBytes(entity), IsEqual.equalTo(writeObjectBytes));
		}
	}

	//endregion

	private BinaryDeserializer createBinaryDeserializer(final byte[] bytes) throws Exception {
		return new BinaryDeserializer(bytes, null);
	}
}
