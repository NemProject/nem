package org.nem.core.serialization;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.MockSerializableEntity;
import org.nem.core.utils.StringEncoder;

import java.math.*;
import java.util.*;

public class JsonSerializerTest extends SerializerTest<JsonSerializer, JsonDeserializer> {

	@Override
	protected JsonSerializer createSerializer() {
		return new JsonSerializer();
	}

	@Override
	protected JsonDeserializer createDeserializer(
			final JsonSerializer serializer,
			final DeserializationContext context) {
		return new JsonDeserializer(serializer.getObject(), context);
	}

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
	public void canWriteDouble() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeDouble("double", 0.999999999534338712692260742187500);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("double"), IsEqual.equalTo(0.999999999534338712692260742187500));
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
		Assert.assertThat(object.get("BigInteger"), IsEqual.equalTo("00958a7561f014"));
	}

	@Test
	public void canWriteUnsignedBigInteger() throws Exception {
		// Arrange:
		final BigInteger i = new BigInteger(1, new byte[] { (byte)0x90, 0x12 });
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeBigInteger("BigInteger", i);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("BigInteger"), IsEqual.equalTo("009012"));
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
		Assert.assertThat(object.get("bytes"), IsEqual.equalTo("50ff007c215a"));
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
		final List<SerializableEntity> originalObjects = new ArrayList<>();
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
			final JSONObject object,
			final int expectedIntValue,
			final String expectedStringValue,
			final long expectedLongValue) {
		// Assert:
		Assert.assertThat(object.get("int"), IsEqual.equalTo(expectedIntValue));
		Assert.assertThat(object.get("s"), IsEqual.equalTo(expectedStringValue));
		Assert.assertThat(object.get("long"), IsEqual.equalTo(expectedLongValue));
	}

	//endregion

	//region Read

	@Test
	public void canReadOptionalNullInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final Integer i = deserializer.readOptionalInt("int");

		// Assert:
		Assert.assertThat(i, IsNull.nullValue());
	}

	@Test
	public void canReadIntAsLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeInt("long", 447182);

		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final long l = deserializer.readLong("long");

		// Assert:
		Assert.assertThat(l, IsEqual.equalTo(447182L));
	}

	@Test
	public void canReadOptionalNullLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final Long l = deserializer.readOptionalLong("long");

		// Assert:
		Assert.assertThat(l, IsNull.nullValue());
	}

	@Test
	public void canReadBigDecimalAsDouble() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.getObject().put("BigDecimal", new BigDecimal("4471.82"));

		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final double d = deserializer.readDouble("BigDecimal");

		// Assert:
		Assert.assertThat(d, IsEqual.equalTo(4471.82));
	}

	@Test
	public void canReadOptionalNullDouble() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final Double d = deserializer.readOptionalDouble("double");

		// Assert:
		Assert.assertThat(d, IsNull.nullValue());
	}

	//endregion

	//region Roundtrip Multiple

	@Test
	public void canRoundtripMultipleValuesWithOrderingChecksEnabled() {
		// Assert:
		this.assertRoundtripMultipleValues(new JsonSerializer(true));
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
		final JSONObject object = serializer.getObject();

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

		final JsonDeserializer deserializer = this.createDeserializer(serializer);

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
		final JSONObject object = serializer.getObject();
		final JSONArray orderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);

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

		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		deserializer.readInt("Bar");
	}

	//endregion

	//region serializeToJson / serializeToBytes

	@Test
	public void serializeToJsonProducesSameJsonObjectAsEntitySerialize() throws Exception {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final SerializableEntity entity = new MockSerializableEntity(17, "foo", 42000000000L);
		entity.serialize(serializer);
		final JSONObject expectedJsonObject = serializer.getObject();

		// Act:
		final JSONObject resultingJsonObject = JsonSerializer.serializeToJson(entity);

		// Act / Assert:
		Assert.assertThat(resultingJsonObject, IsEqual.equalTo(expectedJsonObject));
	}

	@Test
	public void serializeToBytesProducesSameBytesAsEntitySerialize() throws Exception {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final SerializableEntity entity = new MockSerializableEntity(17, "foo", 42000000000L);
		entity.serialize(serializer);
		final JSONObject expectedJsonObject = serializer.getObject();

		// Act:
		final byte[] resultingBytes = JsonSerializer.serializeToBytes(entity);
		final JSONObject resultingJsonObject = (JSONObject)JSONValue.parse(StringEncoder.getString(resultingBytes));

		// Act / Assert:
		Assert.assertThat(resultingJsonObject, IsEqual.equalTo(expectedJsonObject));
	}

	//endregion
}