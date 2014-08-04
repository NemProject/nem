package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

public abstract class SerializerTest<TSerializer extends Serializer, TDeserializer extends Deserializer> {

	/**
	 * Creates a default serializer to use.
	 *
	 * @return A serializer.
	 */
	protected abstract TSerializer createSerializer();

	/**
	 * Creates a deserializer that reads from the specified serializer.
	 *
	 * @param serializer The serializer.
	 * @return A deserializer.
	 */
	protected TDeserializer createDeserializer(final TSerializer serializer) {
		return this.createDeserializer(serializer, null);
	}

	/**
	 * Creates a deserializer that reads from the specified serializer.
	 *
	 * @param serializer The serializer.
	 * @param context The deserialization context.
	 * @return A deserializer.
	 */
	protected abstract TDeserializer createDeserializer(
			final TSerializer serializer,
			final DeserializationContext context);

	//region Primitive Roundtrip (int, long, double)

	@Test
	public void canRoundtripInt() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeInt("int", 0x09513510);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final int i = deserializer.readInt("int");

		// Assert:
		Assert.assertThat(i, IsEqual.equalTo(0x09513510));
	}

	@Test
	public void canRoundtripLong() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeLong("long", 0xF239A033CE951350L);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final long l = deserializer.readLong("long");

		// Assert:
		Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
	}

	@Test
	public void canRoundtripDouble() {
		// Assert:
		this.assertDoubleRoundtrip(0.999999999534338712692260742187500);
	}

	@Test
	public void canRoundtripDoubleNaN() {
		// Assert:
		this.assertDoubleRoundtrip(Double.NaN);
	}

	private void assertDoubleRoundtrip(final double d) {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeDouble("double", d);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final Double readDouble = deserializer.readDouble("double");

		// Assert:
		Assert.assertThat(readDouble, IsEqual.equalTo(d));
	}

	//endregion

	//region BigInteger Roundtrip

	@Test
	public void canRoundtripBigInteger() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		final BigInteger i = new BigInteger("958A7561F014", 16);
		serializer.writeBigInteger("BigInteger", i);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
	}

	@Test
	public void canRoundtripPrefixedUnsignedBigInteger() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		final BigInteger i = new BigInteger(1, new byte[] { (byte)0x90, 0x12 });
		serializer.writeBigInteger("BigInteger", i);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(3, IsEqual.equalTo(i.toByteArray().length));
		Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
	}
//TODO-CR: remove consecutive blank lines

	@Test
	public void canRoundtripOptionalNullBigInteger() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeBigInteger("BigInteger", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readOptionalBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(readBigInteger, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullBigInteger() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeBigInteger("BigInteger", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		assertThrowsMissingPropertyException(
				() -> deserializer.readBigInteger("BigInteger"),
				"BigInteger");
	}

//TODO-CR: remove consecutive blank lines
	//endregion

	//region byte[] Roundtrip

//TODO-CR: this test should be moved up to the BigInteger test group
	@Test
	public void canRoundtripNonPrefixedUnsignedBigInteger() throws Exception {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		final BigInteger i = new BigInteger(new byte[] { (byte)0x90, 0x12 });
		serializer.writeBigInteger("BigInteger", i);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(2, IsEqual.equalTo(i.toByteArray().length));
		Assert.assertThat(readBigInteger, IsEqual.equalTo(new BigInteger(1, new byte[] { (byte)0x90, 0x12 })));
	}

	@Test
	public void canRoundtripBytes() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
		serializer.writeBytes("bytes", bytes);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
	}

	@Test
	public void canRoundtripOptionalNullBytes() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeBytes("bytes", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readOptionalBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullBytes() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeBytes("bytes", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		assertThrowsMissingPropertyException(
				() -> deserializer.readBytes("bytes"),
				"bytes");
	}

	@Test
	public void canRoundtripEmptyBytes() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		final byte[] bytes = new byte[] { };
		serializer.writeBytes("bytes", bytes);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
	}

	//endregion

	//region String Roundtrip

	@Test
	public void canRoundtripString() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeString("String", "BEta GaMMa");

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String");

		// Assert:
		Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
	}

	@Test
	public void canRoundtripOptionalNullString() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeString("String", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readOptionalString("String");

		// Assert:
		Assert.assertThat(s, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullString() {
		// Act:
		assertStringCannotBeRoundTripped(null);
	}

	@Test
	public void cannotRoundtripRequiredEmptyString() {
		// Act:
		assertStringCannotBeRoundTripped("");
	}

	@Test
	public void cannotRoundtripRequiredWhitespaceString() {
		// Act:
		assertStringCannotBeRoundTripped("  \t \t");
	}

	private void assertStringCannotBeRoundTripped(final String s) {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeString("String", s);

		final Deserializer deserializer = this.createDeserializer(serializer);
		assertThrowsMissingPropertyException(
				() -> deserializer.readString("String"),
				"String");
	}

	//endregion

	//region Object Roundtrip

	@Test
	public void canRoundtripObject() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

		final Deserializer deserializer = this.createDeserializer(serializer);
		final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsEqual.equalTo(new MockSerializableEntity(17, "foo", 42)));
	}

	@Test
	public void canRoundtripOptionalNullObject() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final MockSerializableEntity object = deserializer.readOptionalObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullObject() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		assertThrowsMissingPropertyException(
				() -> deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator()),
				"SerializableEntity");
	}

	//endregion

	//region List<SerializableEntity> RoundTrip

	@Test
	public void canRoundtripObjectArray() {
		// Arrange:
		final TSerializer serializer = createSerializer();
		List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(new MockSerializableEntity(17, "foo", 42));
		originalObjects.add(new MockSerializableEntity(111, "bar", 22));
		originalObjects.add(new MockSerializableEntity(1, "alpha", 34));

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(3));
		for (int i = 0; i < objects.size(); ++i)
			Assert.assertThat(objects.get(i), IsEqual.equalTo(originalObjects.get(i)));
	}

	@Test
	public void canRoundtripArrayContainingNullValue() {
		// Arrange:
		final TSerializer serializer = createSerializer();
		List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(null);

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(1));
		Assert.assertThat(objects.get(0), IsNull.nullValue());
	}

	@Test
	public void canRoundtripOptionalNullArray() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObjectArray("oa", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readOptionalObjectArray("oa", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects, IsNull.nullValue());
	}

	@Test
	public void cannotRoundtripRequiredNullArray() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObjectArray("oa", null);

		final Deserializer deserializer = this.createDeserializer(serializer);
		assertThrowsMissingPropertyException(
				() -> deserializer.readObjectArray("oa", new MockSerializableEntity.Activator()),
				"oa");
	}

	@Test
	public void canRoundtripEmptyArray() {
		// Arrange:
		final TSerializer serializer = createSerializer();

		// Act:
		serializer.writeObjectArray("oa", new ArrayList<>());

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readOptionalObjectArray("oa", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(0));
	}

	//endregion

	//region Multiple Object Roundtrip

	@Test
	public void canRoundtripMultipleValues() {
		// Assert:
		assertRoundtripMultipleValues(createSerializer());
	}

	/**
	 * Asserts that using the specified serializer multiple values can be round-tripped.
	 *
	 * @param serializer The serializer.
	 */
	protected void assertRoundtripMultipleValues(final TSerializer serializer) {
		// Act:
		serializer.writeInt("alpha", 0x09513510);
		serializer.writeLong("zeta", 0xF239A033CE951350L);
		serializer.writeBytes("beta", new byte[] { 2, 4, 6 });
		serializer.writeObject("object", new MockSerializableEntity(7, "foo", 5));
		serializer.writeInt("gamma", 7);
		serializer.writeDouble("omega", Double.MIN_NORMAL);
		serializer.writeDouble("psi", Double.MIN_VALUE);
		serializer.writeString("epsilon", "FooBar");
		serializer.writeObjectArray("entities", Arrays.asList(
				new MockSerializableEntity(5, "ooo", 62),
				new MockSerializableEntity(8, "ala", 15)
		));
		serializer.writeBigInteger("bi", new BigInteger("14"));
		serializer.writeLong("sigma", 8);

		final Deserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		Assert.assertThat(deserializer.readInt("alpha"), IsEqual.equalTo(0x09513510));
		Assert.assertThat(deserializer.readLong("zeta"), IsEqual.equalTo(0xF239A033CE951350L));
		Assert.assertThat(deserializer.readBytes("beta"), IsEqual.equalTo(new byte[] { 2, 4, 6 }));

		final MockSerializableEntity entity = deserializer.readObject(
				"object",
				new MockSerializableEntity.Activator());
		Assert.assertThat(entity, IsEqual.equalTo(new MockSerializableEntity(7, "foo", 5)));

		Assert.assertThat(deserializer.readInt("gamma"), IsEqual.equalTo(7));
		Assert.assertThat(deserializer.readDouble("omega"), IsEqual.equalTo(Double.MIN_NORMAL));
		Assert.assertThat(deserializer.readDouble("psi"), IsEqual.equalTo(Double.MIN_VALUE));
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
		final DeserializationContext context = new DeserializationContext(new MockAccountLookup());

		// Act:
		final TSerializer serializer = this.createSerializer();
		final TDeserializer deserializer = this.createDeserializer(serializer, context);

		// Assert:
		Assert.assertThat(deserializer.getContext(), IsEqual.equalTo(context));
	}

	@Test
	public void contextPassedToDeserializerConstructorIsPassedToChildDeserializer() {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(new MockAccountLookup());
		final TSerializer serializer = this.createSerializer();
		serializer.writeObject("test", new MockSerializableEntity(7, "a", 12));

		// Act:
		final TDeserializer deserializer = this.createDeserializer(serializer, context);
		MockSerializableEntity.Activator objectDeserializer = new MockSerializableEntity.Activator();
		deserializer.readObject("test", objectDeserializer);

		// Assert:
		Assert.assertThat(objectDeserializer.getLastContext(), IsEqual.equalTo(context));
	}

	//endregion

	private static void assertThrowsMissingPropertyException(final Supplier<Object> consumer, final String propertyName) {
		ExceptionAssert.assertThrows(
				v -> consumer.get(),
				MissingRequiredPropertyException.class,
				ex -> Assert.assertThat(ex.getPropertyName(), IsEqual.equalTo(propertyName)));
	}
}
