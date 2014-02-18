package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.json.JSONObject;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.test.*;

import java.math.BigInteger;

public class DelegatingObjectSerializerTest {

    //region Write

    @Test
    public void canWriteInt() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeInt(0x09513510);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getInt("1"), IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canWriteLong() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeLong(0xF239A033CE951350L);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getLong("1"), IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canWriteBigInteger() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeBigInteger(new BigInteger("958A7561F014", 16));

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("1"), IsEqual.equalTo("AJWKdWHwFA=="));
    }

    @Test
    public void canWriteBytes() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21, 0x5A };
        serializer.writeBytes(bytes);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("1"), IsEqual.equalTo("UP8AfCFa"));
    }

    @Test
    public void canWriteString() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeString("BEta");

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("1"), IsEqual.equalTo("BEta"));
    }

    @Test
    public void canWriteAccount() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeAccount(new MockAccount("MockAcc"));

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("1"), IsEqual.equalTo("MockAcc"));
    }

    @Test
    public void canWriteSignature() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final Signature signature = new Signature(new BigInteger("23", 16), new BigInteger("A4", 16));
        serializer.writeSignature(signature);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(2));
        Assert.assertThat(object.getString("1"), IsEqual.equalTo("Iw=="));
        Assert.assertThat(object.getString("2"), IsEqual.equalTo("AKQ="));
    }

    //endregion

    //region Roundtrip

    @Test
    public void canRoundtripInt() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeInt(0x09513510);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final int i = deserializer.readInt();

        // Assert:
        Assert.assertThat(i, IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canRoundtripLong() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeLong(0xF239A033CE951350L);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final long l = deserializer.readLong();

        // Assert:
        Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canRoundtripBigInteger() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final BigInteger i = new BigInteger("958A7561F014", 16);
        serializer.writeBigInteger(i);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final BigInteger readBigInteger = deserializer.readBigInteger();

        // Assert:
        Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
    }

    @Test
    public void canRoundtripBytes() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
        serializer.writeBytes(bytes);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final byte[] readBytes = deserializer.readBytes();

        // Assert:
        Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
    }

    @Test
    public void canRoundtripString() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeString("BEta GaMMa");

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final String s = deserializer.readString();

        // Assert:
        Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
    }

    @Test
    public void canRoundtripAccount() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        MockAccountLookup accountLookup = new MockAccountLookup();

        // Act:
        serializer.writeAccount(new MockAccount("MockAcc"));

        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            accountLookup);
        final Account account = deserializer.readAccount();

        // Assert:
        Assert.assertThat(account.getId(), IsEqual.equalTo("MockAcc"));
        Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
    }

    @Test
    public void canRoundtripSignature() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final Signature signature = new Signature(new BigInteger("23", 16), new BigInteger("A4", 16));
        serializer.writeSignature(signature);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final Signature readSignature = deserializer.readSignature();

        // Assert:
        Assert.assertThat(readSignature, IsEqual.equalTo(signature));
    }

    //endregion

    @Test
    public void canRoundtripMultipleValues() throws Exception {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeInt(0x09513510);
        serializer.writeLong(0xF239A033CE951350L);
        serializer.writeAccount(new MockAccount("Beta"));
        serializer.writeBytes(new byte[]{2, 4, 6});
        serializer.writeInt(7);
        serializer.writeString("FooBar");
        serializer.writeLong(8);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());

        // Assert:
        Assert.assertThat(deserializer.readInt(), IsEqual.equalTo(0x09513510));
        Assert.assertThat(deserializer.readLong(), IsEqual.equalTo(0xF239A033CE951350L));
        Assert.assertThat(deserializer.readAccount().getId(), IsEqual.equalTo("Beta"));
        Assert.assertThat(deserializer.readBytes(), IsEqual.equalTo(new byte[] { 2, 4, 6 }));
        Assert.assertThat(deserializer.readInt(), IsEqual.equalTo(7));
        Assert.assertThat(deserializer.readString(), IsEqual.equalTo("FooBar"));
        Assert.assertThat(deserializer.readLong(), IsEqual.equalTo(8L));
    }

    private ObjectDeserializer createObjectDeserializer(final JSONObject object) throws Exception {
        return new DelegatingObjectDeserializer(new JsonDeserializer(object), new MockAccountLookup());
    }
}
