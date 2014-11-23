package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.*;

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
		final TSerializer serializer = this.createSerializer();

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
		final TSerializer serializer = this.createSerializer();

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
		final TSerializer serializer = this.createSerializer();

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
		final TSerializer serializer = this.createSerializer();

		// Act:
		final BigInteger i = new BigInteger(1, new byte[] { (byte)0x90, 0x12 });
		serializer.writeBigInteger("BigInteger", i);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(3, IsEqual.equalTo(i.toByteArray().length));
		Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
	}

	@Test
	public void canRoundtripNonPrefixedUnsignedBigInteger() throws Exception {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final BigInteger i = new BigInteger(new byte[] { (byte)0x90, 0x12 });
		serializer.writeBigInteger("BigInteger", i);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

		// Assert:
		Assert.assertThat(2, IsEqual.equalTo(i.toByteArray().length));
		Assert.assertThat(readBigInteger, IsEqual.equalTo(new BigInteger(1, new byte[] { (byte)0x90, 0x12 })));
	}

	//endregion

	//region byte[] Roundtrip

	@Test
	public void canRoundtripBytes() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
		serializer.writeBytes("bytes", bytes);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
	}

	@Test
	public void canRoundtripEmptyBytes() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

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
		// Assert:
		this.assertCanRoundtripString("BEta GaMMa");
	}

	@Test
	public void canRoundtripUtf8String() {
		// Assert:
		this.assertCanRoundtripString("zuação danada");
	}

	private void assertCanRoundtripString(final String original) {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", original);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String");

		// Assert:
		Assert.assertThat(s, IsEqual.equalTo(original));
	}

	@Test
	public void cannotRoundtripRequiredEmptyString() {
		// Act:
		this.assertStringCannotBeRoundTripped("");
	}

	@Test
	public void cannotRoundtripRequiredWhitespaceString() {
		// Act:
		this.assertStringCannotBeRoundTripped("  \t \t");
	}

	private void assertStringCannotBeRoundTripped(final String s) {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

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
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeObject("SerializableEntity", new MockSerializableEntity(17, "foo", 42));

		final Deserializer deserializer = this.createDeserializer(serializer);
		final MockSerializableEntity object = deserializer.readObject("SerializableEntity", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(object, IsEqual.equalTo(new MockSerializableEntity(17, "foo", 42)));
	}

	//endregion

	//region List<SerializableEntity> RoundTrip

	@Test
	public void canRoundtripObjectArray() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		final List<SerializableEntity> originalObjects = new ArrayList<>();
		originalObjects.add(new MockSerializableEntity(17, "foo", 42));
		originalObjects.add(new MockSerializableEntity(111, "bar", 22));
		originalObjects.add(new MockSerializableEntity(1, "alpha", 34));

		// Act:
		serializer.writeObjectArray("SerializableArray", originalObjects);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readObjectArray("SerializableArray", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(3));
		for (int i = 0; i < objects.size(); ++i) {
			Assert.assertThat(objects.get(i), IsEqual.equalTo(originalObjects.get(i)));
		}
	}

	@Test
	public void canRoundtripArrayContainingNullValue() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();
		final List<SerializableEntity> originalObjects = new ArrayList<>();
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
	public void canRoundtripEmptyArray() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeObjectArray("oa", new ArrayList<>());

		final Deserializer deserializer = this.createDeserializer(serializer);
		final List<MockSerializableEntity> objects = deserializer.readOptionalObjectArray("oa", new MockSerializableEntity.Activator());

		// Assert:
		Assert.assertThat(objects.size(), IsEqual.equalTo(0));
	}

	//endregion

	//region Serialization of null

	//region SerializationPolicy

	private class SerializationPolicy<T> {
		public final BiConsumer<Serializer, String> writeNullValue;
		public final BiFunction<Deserializer, String, T> readValue;
		public final BiFunction<Deserializer, String, T> readOptionalValue;

		public SerializationPolicy(
				final BiConsumer<Serializer, String> writeNullValue,
				final BiFunction<Deserializer, String, T> readValue,
				final BiFunction<Deserializer, String, T> readOptionalValue) {
			this.writeNullValue = writeNullValue;
			this.readValue = readValue;
			this.readOptionalValue = readOptionalValue;
		}

		private void assertCanReadOptionalNullValue() {
			// Arrange:
			final TSerializer serializer = this.createSerializer();
			final Deserializer deserializer = this.createDeserializer(serializer);

			// Act:
			final T value = this.readOptionalValue.apply(deserializer, "value");

			// Assert:
			Assert.assertThat(value, IsNull.nullValue());
		}

		private void assertCannotReadRequiredNullValue() {
			// Arrange:
			final TSerializer serializer = this.createSerializer();
			final Deserializer deserializer = this.createDeserializer(serializer);

			// Assert:
			assertThrowsMissingPropertyException(
					() -> this.readValue.apply(deserializer, "value"),
					"value");
		}

		private void assertCanRoundtripOptionalNullValue() {
			// Arrange:
			final TSerializer serializer = this.createSerializer();
			this.writeNullValue.accept(serializer, "value");
			final Deserializer deserializer = this.createDeserializer(serializer);

			// Act:
			final T value = this.readOptionalValue.apply(deserializer, "value");

			// Assert:
			Assert.assertThat(value, IsNull.nullValue());
		}

		private void assertCannotRoundtripRequiredNullValue() {
			// Arrange:
			final TSerializer serializer = this.createSerializer();
			this.writeNullValue.accept(serializer, "value");
			final Deserializer deserializer = this.createDeserializer(serializer);

			// Act:
			assertThrowsMissingPropertyException(
					() -> this.readValue.apply(deserializer, "value"),
					"value");
		}

		private TSerializer createSerializer() {
			return SerializerTest.this.createSerializer();
		}

		private Deserializer createDeserializer(final TSerializer serializer) {
			return SerializerTest.this.createDeserializer(serializer);
		}
	}

	private final SerializationPolicy<Integer> intSerializationPolicy = new SerializationPolicy<>(
			null,
			(deserializer, label) -> deserializer.readInt(label),
			(deserializer, label) -> deserializer.readOptionalInt(label));

	private final SerializationPolicy<Long> longSerializationPolicy = new SerializationPolicy<>(
			null,
			(deserializer, label) -> deserializer.readLong(label),
			(deserializer, label) -> deserializer.readOptionalLong(label));

	private final SerializationPolicy<Double> doubleSerializationPolicy = new SerializationPolicy<>(
			null,
			(deserializer, label) -> deserializer.readDouble(label),
			(deserializer, label) -> deserializer.readOptionalDouble(label));

	private final SerializationPolicy<BigInteger> bigIntegerSerializationPolicy = new SerializationPolicy<>(
			(serializer, label) -> serializer.writeBigInteger(label, null),
			(deserializer, label) -> deserializer.readBigInteger(label),
			(deserializer, label) -> deserializer.readOptionalBigInteger(label));

	private final SerializationPolicy<byte[]> bytesSerializationPolicy = new SerializationPolicy<>(
			(serializer, label) -> serializer.writeBytes(label, null),
			(deserializer, label) -> deserializer.readBytes(label),
			(deserializer, label) -> deserializer.readOptionalBytes(label));

	private final SerializationPolicy<String> stringSerializationPolicy = new SerializationPolicy<>(
			(serializer, label) -> serializer.writeString(label, null),
			(deserializer, label) -> deserializer.readString(label),
			(deserializer, label) -> deserializer.readOptionalString(label));

	private final SerializationPolicy<MockSerializableEntity> objectSerializationPolicy = new SerializationPolicy<>(
			(serializer, label) -> serializer.writeObject(label, null),
			(deserializer, label) -> deserializer.readObject(label, MockSerializableEntity::new),
			(deserializer, label) -> deserializer.readOptionalObject(label, MockSerializableEntity::new));

	private final SerializationPolicy<List<MockSerializableEntity>> objectArraySerializationPolicy = new SerializationPolicy<>(
			(serializer, label) -> serializer.writeObjectArray(label, null),
			(deserializer, label) -> deserializer.readObjectArray(label, MockSerializableEntity::new),
			(deserializer, label) -> deserializer.readOptionalObjectArray(label, MockSerializableEntity::new));

	//endregion

	//region Primitive (int, long, double)

	@Test
	public void canReadOptionalNullInt() {
		// Assert:
		this.intSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullInt() {
		// Assert:
		this.intSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canReadOptionalNullLong() {
		// Assert:
		this.longSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullLong() {
		// Assert:
		this.longSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canReadOptionalNullDouble() {
		// Assert:
		this.doubleSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullDouble() {
		// Assert:
		this.doubleSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	//endregion

	//region BigInteger

	@Test
	public void canReadOptionalNullBigInteger() {
		// Assert:
		this.bigIntegerSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullBigInteger() {
		// Assert:
		this.bigIntegerSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canRoundtripOptionalNullBigInteger() {
		// Assert:
		this.bigIntegerSerializationPolicy.assertCanRoundtripOptionalNullValue();
	}

	@Test
	public void cannotRoundtripRequiredNullBigInteger() {
		// Assert:
		this.bigIntegerSerializationPolicy.assertCannotRoundtripRequiredNullValue();
	}

	//endregion

	//region byte[]

	@Test
	public void canReadOptionalNullBytes() {
		// Assert:
		this.bytesSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullBytes() {
		// Assert:
		this.bytesSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canRoundtripOptionalNullBytes() {
		// Assert:
		this.bytesSerializationPolicy.assertCanRoundtripOptionalNullValue();
	}

	@Test
	public void cannotRoundtripRequiredNullBytes() {
		// Assert:
		this.bytesSerializationPolicy.assertCannotRoundtripRequiredNullValue();
	}

	//endregion

	//region String

	@Test
	public void canReadOptionalNullString() {
		// Assert:
		this.stringSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullString() {
		// Assert:
		this.stringSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canRoundtripOptionalNullString() {
		// Assert:
		this.stringSerializationPolicy.assertCanRoundtripOptionalNullValue();
	}

	@Test
	public void cannotRoundtripRequiredNullString() {
		// Assert:
		this.stringSerializationPolicy.assertCannotRoundtripRequiredNullValue();
	}

	//endregion

	//region Object

	@Test
	public void canReadOptionalNullObject() {
		// Assert:
		this.objectSerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullObject() {
		// Assert:
		this.objectSerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canRoundtripOptionalNullObject() {
		// Assert:
		this.objectSerializationPolicy.assertCanRoundtripOptionalNullValue();
	}

	@Test
	public void cannotRoundtripRequiredNullObject() {
		// Assert:
		this.objectSerializationPolicy.assertCannotRoundtripRequiredNullValue();
	}

	//endregion

	//region ObjectArray

	@Test
	public void canReadOptionalNullObjectArray() {
		// Assert:
		this.objectArraySerializationPolicy.assertCanReadOptionalNullValue();
	}

	@Test
	public void cannotReadRequiredNullObjectArray() {
		// Assert:
		this.objectArraySerializationPolicy.assertCannotReadRequiredNullValue();
	}

	@Test
	public void canRoundtripOptionalNullObjectArray() {
		// Assert:
		this.objectArraySerializationPolicy.assertCanRoundtripOptionalNullValue();
	}

	@Test
	public void cannotRoundtripRequiredNullObjectArray() {
		// Assert:
		this.objectArraySerializationPolicy.assertCannotRoundtripRequiredNullValue();
	}

	//endregion

	//endregion

	//region Multiple Object Roundtrip

	@Test
	public void canRoundtripMultipleValues() {
		// Assert:
		this.assertRoundtripMultipleValues(this.createSerializer());
	}

	/**
	 * Asserts that using the specified serializer multiple values can be
	 * round-tripped.
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
				new MockSerializableEntity(8, "ala", 15)));
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

	@Test
	public void canReadTrailingOptionalsToAllowStructureExpansion() throws Exception {
		// Arrange:
		// - V1 schema { height: int }
		// - V2 schema { height: int, minBlocks: int, maxTransactions: int }
		final TSerializer serializer = this.createSerializer();
		serializer.writeInt("height", 111);

		// Act:
		final Deserializer deserializer = this.createDeserializer(serializer);

		// Assert:
		Assert.assertThat(deserializer.readInt("height"), IsEqual.equalTo(111));
		Assert.assertThat(deserializer.readOptionalInt("minBlocks"), IsNull.nullValue());
		Assert.assertThat(deserializer.readOptionalInt("maxTransactions"), IsNull.nullValue());
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
		final MockSerializableEntity.Activator objectDeserializer = new MockSerializableEntity.Activator();
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
