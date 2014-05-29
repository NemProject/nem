package org.nem.core.serialization;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.math.BigInteger;
import java.util.*;

public class JsonSerializerTest {

	//region Write

	@Test
	public void canWriteInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("int", 0x09513510);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("int"), IsEqual.equalTo(0x09513510));
	}

	@Test
	public void canWriteLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeLong("long", 0xF239A033CE951350L);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("long"), IsEqual.equalTo(0xF239A033CE951350L));
	}

	@Test
	public void canWriteBigInteger() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeBigInteger("BigInteger", new BigInteger("958A7561F014", 16));

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("BigInteger"), IsEqual.equalTo("AJWKdWHwFA=="));
	}

	@Test
	public void canWriteBytes() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21, 0x5A };
		serializer.writeBytes("bytes", bytes);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("bytes"), IsEqual.equalTo("UP8AfCFa"));
	}

	@Test
	public void canWriteNullBytes() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeBytes("bytes", null);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("bytes"), IsNull.nullValue());
	}

	@Test
	public void canWriteString() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeString("String", "BEta");

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("String"), IsEqual.equalTo("BEta"));
	}

	@Test
	public void canWriteObject() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		assertMockSerializableJsonObject((JSONObject)object.get("SerializableEntity"), 17, "foo", 42);
	}

	@Test
	public void canWriteObjectArray() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(new MockSerializableEntity(17, "foo", 42));
		originalObjects.add(new MockSerializableEntity(111, "bar", 22));
		originalObjects.add(new MockSerializableEntity(1, "alpha", 34));

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));

		final JSONArray serializableArray = (JSONArray)object.get("SerializableArray");
		Assert.assertThat(serializableArray.size(), IsEqual.equalTo(3));
		assertMockSerializableJsonObject((JSONObject)serializableArray.get(0), 17, "foo", 42);
		assertMockSerializableJsonObject((JSONObject)serializableArray.get(1), 111, "bar", 22);
		assertMockSerializableJsonObject((JSONObject)serializableArray.get(2), 1, "alpha", 34);
	}

	private static void assertMockSerializableJsonObject(
			JSONObject object,
			int expectedIntValue,
			String expectedStringValue,
			long expectedLongValue) {
		// Assert:
		Assert.assertThat(object.get("int"), IsEqual.equalTo(expectedIntValue));
		Assert.assertThat(object.get("s"), IsEqual.equalTo(expectedStringValue));
		Assert.assertThat(object.get("long"), IsEqual.equalTo(expectedLongValue));
	}

	//endregion

	//region Roundtrip

	@Test
	public void canRoundtripInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("int", 0x09513510);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final int i = deserializer.readInt("int");

		// Assert:
		Assert.assertThat(i, IsEqual.equalTo(0x09513510));
	}

	@Test
	public void canReadNullInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final Integer i = deserializer.readInt("int");

		// Assert:
		Assert.assertThat(i, IsNull.nullValue());
	}

	@Test
	public void canRoundtripLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeLong("long", 0xF239A033CE951350L);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final long l = deserializer.readLong("long");

		// Assert:
		Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
	}

	@Test
	public void canReadIntAsLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("long", 447182);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final long l = deserializer.readLong("long");

		// Assert:
		Assert.assertThat(l, IsEqual.equalTo(447182L));
	}

	@Test
	public void canReadNullLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final Long l = deserializer.readLong("long");

		// Assert:
		Assert.assertThat(l, IsNull.nullValue());
	}

	@Test
	public void canRoundtripBigInteger() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final BigInteger i = new BigInteger("958A7561F014", 16);
		serializer.writeBigInteger("BigInteger", i);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
	}

	@Test
	public void canReadNullBigInteger() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final BigInteger i = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(i, IsNull.nullValue());
	}

	@Test
	public void canRoundtripBytes() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
		serializer.writeBytes("bytes", bytes);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
	}

	@Test
	public void canRoundtripNullBytes() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeBytes("bytes", null);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsNull.nullValue());
	}

	@Test
	public void canRoundtripEmptyBytes() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final byte[] bytes = new byte[] { };
		serializer.writeBytes("bytes", bytes);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
	}

	@Test
	public void canRoundtripString() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeString("String", "BEta GaMMa");

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final String s = deserializer.readString("String");

		// Assert:
		Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
	}

	@Test
	public void canRoundtripObject() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsEqual.equalTo(new MockSerializableEntity(17, "foo", 42)));
	}

	@Test
	public void canRoundtripNullObject() {

		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", null);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsNull.nullValue());
	}

	@Test
	public void canReadNullObject() {

		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsNull.nullValue());
	}

	@Test
	public void canRoundtripObjectArray() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(new MockSerializableEntity(17, "foo", 42));
		originalObjects.add(new MockSerializableEntity(111, "bar", 22));
		originalObjects.add(new MockSerializableEntity(1, "alpha", 34));

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(3));
		for (int i = 0; i < objects.size(); ++i)
			Assert.assertThat(objects.get(i), IsEqual.equalTo(originalObjects.get(i)));
	}

	@Test
	public void canRoundtripArrayContainingNullValue() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(null);

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(1));
		Assert.assertThat(objects.get(0), IsNull.nullValue());
	}

	@Test
	public void canRoundtripNullArray() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeObjectArray("oa", null);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("oa", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(0));
	}

	//endregion

	//region Roundtrip Multiple

	@Test
	public void canRoundtripMultipleValues() {
		// Assert:
		assertRoundtripMultipleValues(new JsonSerializer());
	}

	@Test
	public void canRoundtripMultipleValuesWithOrderingChecksEnabled() {
		// Assert:
		assertRoundtripMultipleValues(new JsonSerializer(true));
	}

	private void assertRoundtripMultipleValues(final JsonSerializer serializer) {
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

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());

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

	//endregion

	//region Context

	@Test
	public void contextPassedToDeserializerConstructorIsUsed() {
		// Arrange:
		DeserializationContext context = new DeserializationContext(new MockAccountLookup());

		// Act:
		final JsonDeserializer deserializer = new JsonDeserializer(new JSONObject(), context);

		// Assert:
		Assert.assertThat(deserializer.getContext(), IsEqual.equalTo(context));
	}

	@Test
	public void contextPassedToDeserializerConstructorIsPassedToChildDeserializer() {
		// Arrange:
		DeserializationContext context = new DeserializationContext(new MockAccountLookup());
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObject("test", new MockSerializableEntity(7, "a", 12));

		// Act:
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), context);
		MockSerializableEntity.Activator objectDeserializer = new MockSerializableEntity.Activator();
		deserializer.readObject("test", objectDeserializer);

		// Assert:
		Assert.assertThat(objectDeserializer.getLastContext(), IsEqual.equalTo(context));

	}

	//endregion

	//region Order Enforcement

	@Test
	public void defaultSerializerDoesNotPublishPropertyOrderMetadata() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);
		JSONObject object = serializer.getObject();

		// Assert:
		Assert.assertThat(object.containsKey("_order"), IsEqual.equalTo(false));
	}

	@Test
	public void defaultDeserializerDoesNotEnforceOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());

		// Assert:
		Assert.assertThat(deserializer.readInt("Bar"), IsEqual.equalTo(11));
		Assert.assertThat(deserializer.readInt("Foo"), IsEqual.equalTo(17));
	}

	@Test
	public void serializerCanOptionallyPublishPropertyOrderMetadata() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer(true);

		// Act:
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);
		JSONObject object = serializer.getObject();
		JSONArray orderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);

		// Assert:
		Assert.assertThat(orderArray, IsNull.notNullValue());
		Assert.assertThat(orderArray.size(), IsEqual.equalTo(2));
		Assert.assertThat(orderArray.get(0), IsEqual.equalTo("Foo"));
		Assert.assertThat(orderArray.get(1), IsEqual.equalTo("Bar"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void deserializerCanOptionallyEnforceOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer(true);

		// Act:
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);

		final JsonDeserializer deserializer = createJsonDeserializer(serializer.getObject());
		deserializer.readInt("Bar");
	}

	//endregion

	//region serializeToBytes

	@Test
	public void serializeToJsonProducesSameBytesAsEntitySerialize() throws Exception {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		SerializableEntity entity = new MockSerializableEntity(17, "foo", 42);
		entity.serialize(serializer);
		JSONObject writeObjectJson = serializer.getObject();

		// Assert:
		Assert.assertThat(JsonSerializer.serializeToJson(entity), IsEqual.equalTo(writeObjectJson));
	}

	//endregion

	private JsonDeserializer createJsonDeserializer(final JSONObject object) {
		return new JsonDeserializer(object, null);
	}
}