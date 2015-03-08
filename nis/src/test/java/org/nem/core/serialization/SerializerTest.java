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
		return this.createDeserializer(serializer, new DeserializationContext(null));
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

	//region Truncation

	@Test
	public void canTruncateBytesOnWrite() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
		serializer.writeBytes("bytes", bytes, 3);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(new byte[] { 0x50, (byte)0xFF, 0x00 }));
	}

	@Test
	public void canTruncateBytesOnRead() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
		serializer.writeBytes("bytes", bytes);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes", 3);

		// Assert:
		Assert.assertThat(readBytes, IsEqual.equalTo(new byte[] { 0x50, (byte)0xFF, 0x00 }));
	}

	@Test
	public void canDefaultTruncateBytesOnWrite() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final byte[] bytes = new byte[2048];
		serializer.writeBytes("bytes", bytes);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes", 2048);

		// Assert:
		Assert.assertThat(readBytes.length, IsEqual.equalTo(1024));
	}

	@Test
	public void canDefaultTruncateBytesOnRead() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		final byte[] bytes = new byte[2048];
		serializer.writeBytes("bytes", bytes, 2048);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final byte[] readBytes = deserializer.readBytes("bytes");

		// Assert:
		Assert.assertThat(readBytes.length, IsEqual.equalTo(1024));
	}

	@Test
	public void canTruncateStringOnWrite() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", "0123456789", 5);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String");

		// Assert:
		Assert.assertThat(s, IsEqual.equalTo("01234"));
	}

	@Test
	public void canTruncateStringOnRead() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", "0123456789");

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String", 5);

		// Assert:
		Assert.assertThat(s, IsEqual.equalTo("01234"));
	}

	@Test
	public void canDefaultTruncateStringOnWrite() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", new String(new char[200]));

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String", 200);

		// Assert:
		Assert.assertThat(s.length(), IsEqual.equalTo(128));
	}

	@Test
	public void canDefaultTruncateStringOnRead() {
		// Arrange:
		final TSerializer serializer = this.createSerializer();

		// Act:
		serializer.writeString("String", new String(new char[200]), 200);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final String s = deserializer.readString("String");

		// Assert:
		Assert.assertThat(s.length(), IsEqual.equalTo(128));
	}

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
}
