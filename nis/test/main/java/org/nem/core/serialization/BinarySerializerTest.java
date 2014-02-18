package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;

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

    //endregion

    @Test
    public void canRoundtripMultipleValues() throws Exception {
        // Arrange:
        try (BinarySerializer serializer = new BinarySerializer()) {
            // Act:
            serializer.writeInt("alpha", 0x09513510);
            serializer.writeLong("zeta", 0xF239A033CE951350L);
            serializer.writeBytes("beta", new byte[]{2, 4, 6});
            serializer.writeInt("gamma", 7);
            serializer.writeString("epsilon", "FooBar");
            serializer.writeLong("sigma", 8);

            try (BinaryDeserializer deserializer = createBinaryDeserializer(serializer.getBytes())) {
                // Assert:
                Assert.assertThat(deserializer.readInt("alpha"), IsEqual.equalTo(0x09513510));
                Assert.assertThat(deserializer.readLong("zeta"), IsEqual.equalTo(0xF239A033CE951350L));
                Assert.assertThat(deserializer.readBytes("beta"), IsEqual.equalTo(new byte[] { 2, 4, 6 }));
                Assert.assertThat(deserializer.readInt("gamma"), IsEqual.equalTo(7));
                Assert.assertThat(deserializer.readString("epsilon"), IsEqual.equalTo("FooBar"));
                Assert.assertThat(deserializer.readLong("sigma"), IsEqual.equalTo(8L));
            }
        }
    }

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

    @Test(expected=SerializationException.class)
    public void readOfPrimitiveTypeFailsIfStreamIsTooSmall() throws Exception {
        byte[] bytes = new byte[] { 1, 2, 4 };
        try (BinaryDeserializer deserializer = createBinaryDeserializer(bytes)) {
            // Assert:
            deserializer.readInt("int");
        }
    }

    @Test(expected=SerializationException.class)
    public void readOfVariableSizedTypeFailsIfStreamIsTooSmall() throws Exception {
        byte[] bytes = new byte[] { 0x02, 0x00, 0x00, 0x00, 0x01 };
        try (BinaryDeserializer deserializer = createBinaryDeserializer(bytes)) {
            // Assert:
            deserializer.readBytes("bytes");
        }
    }

    private BinaryDeserializer createBinaryDeserializer(final byte[] bytes) throws Exception {
        return new BinaryDeserializer(bytes);
    }
}
