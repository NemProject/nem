package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.test.ExceptionAssert;

import java.util.Properties;

@RunWith(Enclosed.class)
public class NemPropertiesTest {

	//region StringPropertyTest

	public static class StringPropertyTest {

		@Test
		public void canReadRequiredNonNullStringProperty() {
			// Arrange:
			final NemProperties properties = createNemProperties();

			// Act:
			final String value = properties.getString("s");

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo("nem"));
		}

		@Test
		public void cannotReadRequiredNullStringProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Assert:
			ExceptionAssert.assertThrows(
					v -> properties.getString("x"),
					RuntimeException.class);
		}

		@Test
		public void canReadOptionalNonNullStringProperty() {
			// Arrange:
			final NemProperties properties = createNemProperties();

			// Act:
			final String value = properties.getOptionalString("s", "abc");

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo("nem"));
		}

		@Test
		public void canReadOptionalNullStringProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Act:
			final String value = properties.getOptionalString("x", "abc");

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo("abc"));
		}
	}

	//endregion

	//region AbstractParsablePropertyTest (Integer, Long, Boolean)

	private abstract static class AbstractParsablePropertyTest<T> {
		private final String key;
		private final T parsedValue;
		private final T defaultValue;

		protected AbstractParsablePropertyTest(final String key, final T parsedValue, final T defaultValue) {
			this.key = key;
			this.parsedValue = parsedValue;
			this.defaultValue = defaultValue;
		}

		protected abstract T getValue(final NemProperties properties, final String key);
		protected abstract T getOptionalValue(final NemProperties properties, final String key);

		// region required

		@Test
		public void canReadRequiredParsableProperty() {
			// Arrange:
			final NemProperties properties = createNemProperties();

			// Act:
			final T value = this.getValue(properties, this.key);

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo(this.parsedValue));
		}

		@Test
		public void cannotReadRequiredNonParsableProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Assert:
			ExceptionAssert.assertThrows(
					v -> this.getValue(properties, "s"),
					RuntimeException.class);
		}

		@Test
		public void cannotReadRequiredNullProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Assert:
			ExceptionAssert.assertThrows(
					v -> this.getValue(properties, "x"),
					RuntimeException.class);
		}

		//endregion

		//region optional

		@Test
		public void canReadOptionalParsableProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Act:
			final T value = this.getOptionalValue(properties, this.key);

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo(this.parsedValue));
		}

		@Test
		public void cannotReadOptionalNonParsableProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Assert:
			ExceptionAssert.assertThrows(
					v -> this.getOptionalValue(properties, "s"),
					RuntimeException.class);
		}

		@Test
		public void canReadOptionalNullProperty() {
			// Act:
			final NemProperties properties = createNemProperties();

			// Act:
			final T value = this.getOptionalValue(properties, "x");

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo(this.defaultValue));
		}

		//endregion
	}

	public static class IntegerPropertyTest extends AbstractParsablePropertyTest<Integer> {

		public IntegerPropertyTest() {
			super("i", 625, 1337);
		}

		@Override
		protected Integer getValue(final NemProperties properties, final String key) {
			return properties.getInteger(key);
		}

		@Override
		protected Integer getOptionalValue(final NemProperties properties, final String key) {
			return properties.getOptionalInteger(key, 1337);
		}
	}

	public static class LongPropertyTest extends AbstractParsablePropertyTest<Long> {

		public LongPropertyTest() {
			super("l", 256L, 1337L);
		}

		@Override
		protected Long getValue(final NemProperties properties, final String key) {
			return properties.getLong(key);
		}

		@Override
		protected Long getOptionalValue(final NemProperties properties, final String key) {
			return properties.getOptionalLong(key, 1337L);
		}
	}

	public static class BooleanPropertyTest extends AbstractParsablePropertyTest<Boolean> {

		public BooleanPropertyTest() {
			super("b", true, false);
		}

		@Override
		protected Boolean getValue(final NemProperties properties, final String key) {
			return properties.getBoolean(key);
		}

		@Override
		protected Boolean getOptionalValue(final NemProperties properties, final String key) {
			return properties.getOptionalBoolean(key, false);
		}
	}

	//endregion

	//region StringArrayPropertyTest

	public static class StringArrayPropertyTest {

		@Test
		public void canReadOptionalNullStringArrayProperty() {
			// Arrange:
			final NemProperties properties = createNemProperties();

			// Act:
			final String[] value = properties.getOptionalStringArray("x", "abc|xyz");

			// Assert:
			Assert.assertThat(
					value,
					IsEqual.equalTo(new String[] { "abc", "xyz" }));
		}

		@Test
		public void canReadOptionalStringArrayWithNoValues() {
			// Assert:
			this.assertCanReadStringArray(" \t \t", new String[] {});
		}

		@Test
		public void canReadOptionalStringArrayWithSingleValue() {
			// Assert:
			this.assertCanReadStringArray("10.0.0.10", new String[] { "10.0.0.10" });
		}

		@Test
		public void canReadOptionalStringArrayWithMultipleValues() {
			// Assert:
			this.assertCanReadStringArray(
					"10.0.0.10|10.0.0.20|10.0.0.30",
					new String[] { "10.0.0.10", "10.0.0.20", "10.0.0.30" });
		}

		@Test
		public void canReadOptionalStringArrayWithBlankValues() {
			// Assert:
			this.assertCanReadStringArray(
					"10.0.0.10|| |10.0.0.30",
					new String[] { "10.0.0.10", "", " ", "10.0.0.30" });
		}

		private void assertCanReadStringArray(final String rawValue, final String[] expectedValues) {
			// Arrange:
			final Properties properties = new Properties();
			properties.setProperty("sa", rawValue);
			final NemProperties nemProperties = new NemProperties(properties);

			// Act:
			final String[] value = nemProperties.getOptionalStringArray("sa", "");

			// Assert:
			Assert.assertThat(value, IsEqual.equalTo(expectedValues));
		}
	}

	//endregion

	private static NemProperties createNemProperties() {
		// Arrange:
		final Properties properties = new Properties();
		properties.put("s", "nem");
		properties.put("i", "625");
		properties.put("l", "256");
		properties.put("b", "true");

		// Act:
		return new NemProperties(properties);
	}
}