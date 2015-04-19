package org.nem.core.serialization.primitive;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractPrimitiveTruncationTest<TSerializer extends Serializer, TDeserializer extends Deserializer, T> {
	private final SerializationPolicy<TSerializer, TDeserializer> policy;

	public AbstractPrimitiveTruncationTest(final SerializationPolicy<TSerializer, TDeserializer> policy) {
		this.policy = policy;
	}

	@Test
	public void canTruncateOnWrite() {
		// Arrange:
		final T originalValue = this.getValue(20);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue, 5);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val");

		// Assert:
		final T expectedValue = this.getTruncatedValue(originalValue, 5);
		Assert.assertThat(value, IsEqual.equalTo(expectedValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(5));
	}

	@Test
	public void canTruncateStringOnRead() {
		// Arrange:
		final T originalValue = this.getValue(20);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val", 5);

		// Assert:
		final T expectedValue = this.getTruncatedValue(originalValue, 5);
		Assert.assertThat(value, IsEqual.equalTo(expectedValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(5));
	}

	@Test
	public void canDefaultTruncateStringOnWrite() {
		// Arrange:
		final int limit = this.getDefaultLimit() * 3;
		final T originalValue = this.getValue(limit);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val", limit);

		// Assert:
		final T expectedValue = this.getTruncatedValue(originalValue, this.getDefaultLimit());
		Assert.assertThat(value, IsEqual.equalTo(expectedValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(this.getDefaultLimit()));
	}

	@Test
	public void canDefaultTruncateStringOnRead() {
		// Arrange:
		final int limit = this.getDefaultLimit() * 3;
		final T originalValue = this.getValue(limit);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue, limit);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val");

		// Assert:
		final T expectedValue = this.getTruncatedValue(originalValue, this.getDefaultLimit());
		Assert.assertThat(value, IsEqual.equalTo(expectedValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(this.getDefaultLimit()));
	}

	@Test
	public void canRoundtripValueSameSizeAsDefaultTruncationLimitWithoutCustomLimits() {
		// Arrange:
		final int limit = this.getDefaultLimit();
		final T originalValue = this.getValue(limit);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val");

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(originalValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(limit));
	}

	@Test
	public void canRoundtripValueLargerThanDefaultTruncationLimit() {
		// Arrange:
		final int limit = this.getDefaultLimit() * 3;
		final T originalValue = this.getValue(limit);
		final TSerializer serializer = this.createSerializer();

		// Act:
		this.writeValue(serializer, "val", originalValue, limit);

		final Deserializer deserializer = this.createDeserializer(serializer);
		final T value = this.readValue(deserializer, "val", limit);

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(originalValue));
		Assert.assertThat(this.getSize(value), IsEqual.equalTo(limit));
	}

	@Test
	public void truncationLimitIsNotAppliedPerObject() {
		// Arrange: write an entity that contains <numFields> fields each equal to the limit
		final int numFields = 3;
		final int limit = this.getDefaultLimit();
		final List<T> originalValues = new ArrayList<>();
		for (int i = 0; i < numFields; ++i) {
			originalValues.add(this.getValue(limit));
		}

		final TSerializer serializer = this.createSerializer();
		serializer.writeObject("obj", s -> {
			for (int i = 0; i < numFields; ++i) {
				this.writeValue(s, Integer.toString(i), originalValues.get(i));
			}
		});

		// Act: deserialize the entity (cheat by reading directly into a list)
		final List<T> values = new ArrayList<>();
		final Deserializer deserializer = this.createDeserializer(serializer);
		deserializer.readOptionalObject("obj", d -> {
			for (int i = 0; i < numFields; ++i) {
				values.add(this.readValue(d, Integer.toString(i)));
			}

			return null;
		});

		// Assert: nothing was truncated
		for (int i = 0; i < numFields; ++i) {
			final T value = values.get(i);
			Assert.assertThat(value, IsEqual.equalTo(originalValues.get(i)));
			Assert.assertThat(this.getSize(value), IsEqual.equalTo(limit));
		}
	}

	@Test
	public void truncationLimitIsNotAppliedPerObjectArray() {
		// Arrange: write <numFields> array elements each containing a single field equal to the limit
		final int numFields = 3;
		final int limit = this.getDefaultLimit();
		final List<T> originalValues = new ArrayList<>();
		for (int i = 0; i < numFields; ++i) {
			originalValues.add(this.getValue(limit));
		}

		final TSerializer serializer = this.createSerializer();
		final List<SerializableEntity> entities = originalValues.stream()
				.map(o -> (SerializableEntity)s -> this.writeValue(s, "val", o))
				.collect(Collectors.toList());
		serializer.writeObjectArray("arr", entities);

		// Act: deserialize the array
		final List<T> values = new ArrayList<>();
		final Deserializer deserializer = this.createDeserializer(serializer);
		deserializer.readObjectArray("arr", d -> values.add(this.readValue(d, "val")));

		// Assert: nothing was truncated
		for (int i = 0; i < numFields; ++i) {
			final T value = values.get(i);
			Assert.assertThat(value, IsEqual.equalTo(originalValues.get(i)));
			Assert.assertThat(this.getSize(value), IsEqual.equalTo(limit));
		}
	}

	protected abstract T getValue(final int length);

	protected abstract T getTruncatedValue(final T value, final int limit);

	protected abstract int getSize(final T value);

	protected abstract int getDefaultLimit();

	protected abstract void writeValue(final Serializer serializer, final String label, final T value, final int limit);

	protected abstract T readValue(final Deserializer deserializer, final String label, final int limit);

	protected abstract void writeValue(final Serializer serializer, final String label, final T value);

	protected abstract T readValue(final Deserializer deserializer, final String label);

	protected TSerializer createSerializer() {
		return this.policy.createSerializer();
	}

	protected Deserializer createDeserializer(final TSerializer serializer) {
		return this.policy.createDeserializer(serializer);
	}
}
