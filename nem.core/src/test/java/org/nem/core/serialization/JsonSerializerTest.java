package org.nem.core.serialization;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.primitive.*;
import org.nem.core.test.*;
import org.nem.core.utils.StringEncoder;

import java.math.*;
import java.util.*;
import java.util.function.Supplier;

public class JsonSerializerTest extends SerializerTest<JsonSerializer, JsonDeserializer> {

	@Override
	protected SerializationPolicy<JsonSerializer, JsonDeserializer> getPolicy() {
		return new JsonSerializationPolicy();
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

	//region readInt

	@Test
	public void canReadLongAsInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeLong("int", 447182L);

		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final int i = deserializer.readInt("int");

		// Assert:
		Assert.assertThat(i, IsEqual.equalTo(447182));
	}

	@Test
	public void cannotReadStringAsInt() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeString("s", "447182");

		final JsonDeserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		assertThrowsTypeMismatchException(
				() -> deserializer.readInt("s"),
				"s");
	}

	//endregion

	//region readLong

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
	public void cannotReadStringAsLong() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeString("s", "447182");

		final JsonDeserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		assertThrowsTypeMismatchException(
				() -> deserializer.readLong("s"),
				"s");
	}

	//endregion

	//region readDouble

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
	public void cannotReadStringAsDouble() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		serializer.writeString("s", "447182");

		final JsonDeserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		assertThrowsTypeMismatchException(
				() -> deserializer.readDouble("s"),
				"s");
	}

	//endregion

	//endregion

	//region Roundtrip Multiple

	@Test
	public void canRoundtripMultipleValuesWithOrderingChecksEnabled() {
		// Assert:
		this.assertRoundtripMultipleValues(new JsonSerializer(true));
	}

	//endregion

	//region Order Enforcement

	//region default (off)

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

	//endregion

	//region order serialization (on)

	//region top-level object

	@Test
	public void serializerCanOptionallyPublishPropertyOrderMetadata() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForFlatObjectWithOrderedReadsEnabled();

		// Act:
		final JSONObject object = serializer.getObject();
		final JSONArray orderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);

		// Assert:
		Assert.assertThat(orderArray, IsNull.notNullValue());
		Assert.assertThat(orderArray, IsEqual.equalTo(Arrays.asList("Foo", "Bar")));
	}

	@Test
	public void deserializerFailsIfOutOfOrderReadIsAttemptedWhenEnforcingOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForFlatObjectWithOrderedReadsEnabled();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		ExceptionAssert.assertThrows(
				v -> deserializer.readInt("Bar"),
				IllegalArgumentException.class);
	}

	private static JsonSerializer createSerializerForFlatObjectWithOrderedReadsEnabled() {
		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);
		return serializer;
	}

	//endregion

	//region nested object

	@Test
	public void serializerCanOptionallyPublishPropertyOrderMetadataForNestedObjects() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForObjectContainingNestedObjectWithOrderedReadsEnabled();

		// Act:
		final JSONObject object = serializer.getObject();
		final JSONArray orderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
		final JSONArray innerOrderArray = (JSONArray)((JSONObject)object.get("Obj")).get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);

		// Assert:
		Assert.assertThat(orderArray, IsNull.notNullValue());
		Assert.assertThat(orderArray, IsEqual.equalTo(Arrays.asList("Foo", "Bar", "Obj")));
		Assert.assertThat(innerOrderArray, IsEqual.equalTo(Arrays.asList("Foo2", "Bar2")));
	}

	@Test
	public void deserializerFailsIfOutOfOrderNestedObjectReadIsAttemptedWhenEnforcingOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForObjectContainingNestedObjectWithOrderedReadsEnabled();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		deserializer.readInt("Foo");
		deserializer.readInt("Bar");
		deserializer.readObject("Obj", d1 -> {
			ExceptionAssert.assertThrows(
					v -> d1.readInt("Bar2"),
					IllegalArgumentException.class);
			return new Object();
		});
	}

	private static JsonSerializer createSerializerForObjectContainingNestedObjectWithOrderedReadsEnabled() {
		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);
		serializer.writeObject("Obj", s1 -> {
			s1.writeInt("Foo2", 9);
			s1.writeInt("Bar2", 3);
		});

		return serializer;
	}

	//endregion

	@Test
	public void serializerCanOptionallyPublishPropertyOrderMetadataForNestedArrays() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForObjectContainingNestedArrayWithOrderedReadsEnabled();

		final JSONObject object = serializer.getObject();
		final JSONArray orderArray = (JSONArray)object.get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
		final JSONArray objArray = ((JSONArray)object.get("Arr"));
		final JSONArray innerOrderArray1 = (JSONArray)((JSONObject)objArray.get(0)).get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);
		final JSONArray innerOrderArray2 = (JSONArray)((JSONObject)objArray.get(1)).get(JsonSerializer.PROPERTY_ORDER_ARRAY_NAME);

		// Assert:
		Assert.assertThat(orderArray, IsNull.notNullValue());
		Assert.assertThat(orderArray, IsEqual.equalTo(Arrays.asList("Foo", "Bar", "Arr")));
		Assert.assertThat(innerOrderArray1, IsEqual.equalTo(Arrays.asList("Foo2", "Bar2")));
		Assert.assertThat(innerOrderArray2, IsEqual.equalTo(Collections.singletonList("Foo3")));
	}

	@Test
	public void deserializerFailsIfOutOfOrderNestedArrayObjectReadIsAttemptedWhenEnforcingOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = createSerializerForObjectContainingNestedArrayWithOrderedReadsEnabled();

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		deserializer.readInt("Foo");
		deserializer.readInt("Bar");
		deserializer.readObjectArray("Arr", d1 -> {
			ExceptionAssert.assertThrows(
					v -> d1.readInt("Bar2"),
					IllegalArgumentException.class);
			return new Object();
		});
	}

	private static JsonSerializer createSerializerForObjectContainingNestedArrayWithOrderedReadsEnabled() {
		final JsonSerializer serializer = new JsonSerializer(true);

		// Act:
		serializer.writeInt("Foo", 17);
		serializer.writeInt("Bar", 11);

		final SerializableEntity entity1 = s1 -> {
			s1.writeInt("Foo2", 9);
			s1.writeInt("Bar2", 3);
		};

		final SerializableEntity entity2 = s2 -> s2.writeInt("Foo3", 9);

		serializer.writeObjectArray("Arr", Arrays.asList(entity1, entity2));
		return serializer;
	}

	//endregion

	@Test
	public void deserializerCannotDeserializeOptionalTrailingValuesWhenEnforcingOrderedReads() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeInt("Foo", 17);

		// Act:
		final JsonDeserializer deserializer = this.createDeserializer(serializer);
		final Integer value1 = deserializer.readInt("Foo");

		// - reading "Bar" after "Foo" throws because "Bar" was never written
		// - for the binary serializer this means that no sentinel value was written and the end of stream would be passed
		// - "optional" here means "can be null" vs "can be present"
		// - ("optional" values must be present for binary serialization to work)
		ExceptionAssert.assertThrows(
				v -> deserializer.readOptionalInt("Bar"),
				IllegalArgumentException.class);

		// Assert:
		Assert.assertThat(value1, IsEqual.equalTo(17));
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

	private static void assertThrowsTypeMismatchException(final Supplier<Object> consumer, final String propertyName) {
		ExceptionAssert.assertThrows(
				v -> consumer.get(),
				TypeMismatchException.class,
				ex -> Assert.assertThat(ex.getPropertyName(), IsEqual.equalTo(propertyName)));
	}
}