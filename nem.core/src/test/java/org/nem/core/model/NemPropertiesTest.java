package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NemPropertiesTest {

	//region getString

	@Test
	public void canReadRequiredNonNullStringProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getString("s"), IsEqual.equalTo("nem"));
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

	//endregion

	//region getInteger

	@Test
	public void canReadRequiredParsableIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getInteger("i"), IsEqual.equalTo(625));
	}

	@Test
	public void cannotReadRequiredNonParsableIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		ExceptionAssert.assertThrows(
				v -> properties.getInteger("s"),
				RuntimeException.class);
	}

	@Test
	public void cannotReadRequiredNullIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		ExceptionAssert.assertThrows(
				v -> properties.getInteger("x"),
				RuntimeException.class);
	}

	//endregion

	//region getOptionalString

	@Test
	public void canReadOptionalNonNullStringProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalString("s", "abc"), IsEqual.equalTo("nem"));
	}

	@Test
	public void canReadOptionalNullStringProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalString("x", "abc"), IsEqual.equalTo("abc"));
	}

	//endregion

	//region getOptionalInteger

	@Test
	public void canReadOptionalParsableIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalInteger("i", 1337), IsEqual.equalTo(625));
	}

	@Test
	public void cannotReadOptionalNonParsableIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		ExceptionAssert.assertThrows(
				v -> properties.getOptionalInteger("s", 1337),
				RuntimeException.class);
	}

	@Test
	public void canReadOptionalNullIntegerProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalInteger("x", 1337), IsEqual.equalTo(1337));
	}

	//endregion

	//region getOptionalBoolean

	@Test
	public void canReadOptionalParsableBooleanProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalBoolean("b", false), IsEqual.equalTo(true));
	}

	@Test
	public void canReadOptionalNonParsableBooleanProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalBoolean("s", true), IsEqual.equalTo(false));
	}

	@Test
	public void canReadOptionalNullBooleanProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(properties.getOptionalBoolean("x", true), IsEqual.equalTo(true));
	}

	//endregion

	//region getOptionalStringArray

	@Test
	public void canReadOptionalNullStringArrayProperty() {
		// Act:
		final NemProperties properties = createNemProperties();

		// Assert:
		Assert.assertThat(
				properties.getOptionalStringArray("x", "abc|xyz"),
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

	private void assertCanReadStringArray(final String value, final String[] expectedValues) {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("sa", value);

		// Act:
		final NemProperties nemProperties = new NemProperties(properties);

		// Assert:
		Assert.assertThat(
				nemProperties.getOptionalStringArray("sa", ""),
				IsEqual.equalTo(expectedValues));
	}

	//endregion

	// region asCollection

	@Test
	public void asCollectionReturnsAllEntriesAsCollection() {
		// Act:
		final Collection<NemProperty> properties = createNemProperties().asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(
				new NemProperty("s", "nem"),
				new NemProperty("i", "625"),
				new NemProperty("b", "true"));
		Assert.assertThat(properties, IsEquivalent.equivalentTo(expectedProperties));
	}

	//endregion

	private static NemProperties createNemProperties() {
		// Arrange:
		final Properties properties = new Properties();
		properties.put("s", "nem");
		properties.put("i", "625");
		properties.put("b", "true");

		// Act:
		return new NemProperties(properties);
	}
}