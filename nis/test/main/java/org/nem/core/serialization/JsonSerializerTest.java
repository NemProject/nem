package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.json.*;
import org.junit.*;

import java.math.BigInteger;

public class JsonSerializerTest {

    //region Write

    @Test
    public void canWriteInt() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeInt("int", 0x09513510);

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getInt("int"), IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canWriteLong() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeLong("long", 0xF239A033CE951350L);

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getLong("long"), IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canWriteBigInteger() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeBigInteger("BigInteger", new BigInteger("958A7561F014", 16));

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("BigInteger"), IsEqual.equalTo("AJWKdWHwFA=="));
    }

    @Test
    public void canWriteBytes() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21, 0x5A };
        serializer.writeBytes("bytes", bytes);

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("bytes"), IsEqual.equalTo("UP8AfCFa"));
    }

    @Test
    public void canWriteString() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeString("String", "BEta");

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("String"), IsEqual.equalTo("BEta"));
    }

    //endregion

    //region Roundtrip

    @Test
    public void canRoundtripInt() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeInt("int", 0x09513510);

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
        final int i = deserializer.readInt("int");

        // Assert:
        Assert.assertThat(i, IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canRoundtripLong() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeLong("long", 0xF239A033CE951350L);

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
        final long l = deserializer.readLong("long");

        // Assert:
        Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canRoundtripBigInteger() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        final BigInteger i = new BigInteger("958A7561F014", 16);
        serializer.writeBigInteger("BigInteger", i);

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
        final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

        // Assert:
        Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
    }

    @Test
    public void canRoundtripBytes() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
        serializer.writeBytes("bytes", bytes);

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
        final byte[] readBytes = deserializer.readBytes("bytes");

        // Assert:
        Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
    }

    @Test
    public void canRoundtripString() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeString("String", "BEta GaMMa");

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
        final String s = deserializer.readString("String");

        // Assert:
        Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
    }

    //endregion

    @Test
    public void canRoundtripMultipleValues() throws Exception {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        serializer.writeInt("alpha", 0x09513510);
        serializer.writeLong("zeta", 0xF239A033CE951350L);
        serializer.writeBytes("beta", new byte[]{2, 4, 6});
        serializer.writeInt("gamma", 7);
        serializer.writeString("epsilon", "FooBar");
        serializer.writeLong("sigma", 8);

        JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());

        // Assert:
        Assert.assertThat(deserializer.readInt("alpha"), IsEqual.equalTo(0x09513510));
        Assert.assertThat(deserializer.readLong("zeta"), IsEqual.equalTo(0xF239A033CE951350L));
        Assert.assertThat(deserializer.readBytes("beta"), IsEqual.equalTo(new byte[] { 2, 4, 6 }));
        Assert.assertThat(deserializer.readInt("gamma"), IsEqual.equalTo(7));
        Assert.assertThat(deserializer.readString("epsilon"), IsEqual.equalTo("FooBar"));
        Assert.assertThat(deserializer.readLong("sigma"), IsEqual.equalTo(8L));
    }

    private JsonDeserializer createJsonDeserializer(final JSONObject object) throws Exception {
        return new JsonDeserializer(object);
    }
}
