package org.nem.core.model.mosaic;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.core.test.*;

public class DefaultMosaicPropertiesTest {

	// region ctor

	@Test
	public void canCreateMosaicPropertiesAroundDefaultProperties() {
		// Act:
		final MosaicProperties properties = new DefaultMosaicProperties(new Properties());

		// Assert:
		assertDefaultProperties(properties);
	}

	@Test
	public void canCreateMosaicPropertiesAroundCustomProperties() {
		// Act:
		final MosaicProperties properties = new DefaultMosaicProperties(getCustomProperties());

		// Assert:
		assertCustomProperties(properties);
	}

	@Test
	public void canCreateMosaicPropertiesAroundEmptyNemPropertyCollection() {
		// Act:
		final MosaicProperties properties = new DefaultMosaicProperties(Collections.emptyList());

		// Assert:
		assertDefaultProperties(properties);
	}

	@Test
	public void canCreateMosaicPropertiesAroundCustomNemPropertyCollection() {
		// Act:
		final Collection<NemProperty> nemProperties = Arrays.asList(new NemProperty("divisibility", "2"),
				new NemProperty("initialSupply", "123456"), new NemProperty("supplyMutable", "true"),
				new NemProperty("transferable", "false"));
		final MosaicProperties properties = new DefaultMosaicProperties(nemProperties);

		// Assert:
		assertCustomProperties(properties);
	}

	private static void assertDefaultProperties(final MosaicProperties properties) {
		MatcherAssert.assertThat(properties.getDivisibility(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(1_000L));
		MatcherAssert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
	}

	private static void assertCustomProperties(final MosaicProperties properties) {
		MatcherAssert.assertThat(properties.getDivisibility(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(123456L));
		MatcherAssert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(properties.isTransferable(), IsEqual.equalTo(false));
	}

	@Test
	public void canCreateMosaicPropertiesWithExtremeQuantities() {
		for (final long quantity : Arrays.asList(0L, MosaicConstants.MAX_QUANTITY / 1000)) {
			// Arrange:
			final Properties properties = getCustomProperties();
			properties.put("divisibility", "3");
			properties.put("initialSupply", Long.toString(quantity));

			// Act:
			final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

			// Assert:
			MatcherAssert.assertThat(mosaicProperties.getInitialSupply(), IsEqual.equalTo(quantity));
		}
	}

	@Test
	public void cannotCreateMosaicPropertiesAroundNullProperties() {
		ExceptionAssert.assertThrows(v -> new DefaultMosaicProperties((Properties) null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicPropertiesIfAtLeastOnePropertyIsInvalid() {
		// Arrange:
		final List<NemProperty> invalidProperties = createInvalidPropertiesList();

		// Assert:
		invalidProperties.stream().forEach(e -> {
			final Properties properties = getCustomProperties();
			properties.put(e.getName(), e.getValue());
			ExceptionAssert.assertThrows(v -> new DefaultMosaicProperties(properties), IllegalArgumentException.class);
		});
	}

	@Test
	public void cannotCreateMosaicPropertiesAroundNemPropertyCollectionIfAtLeastOnePropertyIsInvalid() {
		// Arrange:
		final List<NemProperty> invalidProperties = createInvalidPropertiesList();

		// Assert:
		invalidProperties.stream().forEach(e -> ExceptionAssert.assertThrows(v -> new DefaultMosaicProperties(Collections.singletonList(e)),
				IllegalArgumentException.class));
	}

	private static List<NemProperty> createInvalidPropertiesList() {
		final List<NemProperty> list = new ArrayList<>();
		list.add(new NemProperty("divisibility", "-1"));
		list.add(new NemProperty("divisibility", "7"));
		list.add(new NemProperty("initialSupply", "-1"));
		list.add(new NemProperty("initialSupply", Long.toString(MosaicConstants.MAX_QUANTITY + 1)));
		return list;
	}

	@Test
	public void cannotCreateMosaicPropertiesIfQuantityIsTooLargeRelativeToDivisibility() {
		// Arrange:
		final Properties properties = getCustomProperties();
		properties.put("divisibility", "4");
		properties.put("initialSupply", Long.toString(MosaicConstants.MAX_QUANTITY / 1000));

		// Act:
		ExceptionAssert.assertThrows(v -> new DefaultMosaicProperties(properties), IllegalArgumentException.class);
	}

	// endregion

	// region asCollection

	@Test
	public void asCollectionReturnsAllKnownDefaultProperties() {
		// Arrange:
		final Properties properties = new Properties();
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

		// Act:
		final Collection<NemProperty> nemProperties = mosaicProperties.asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(new NemProperty("divisibility", "0"),
				new NemProperty("initialSupply", "1000"), new NemProperty("supplyMutable", "false"),
				new NemProperty("transferable", "true"));
		MatcherAssert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
	}

	@Test
	public void asCollectionReturnsAllKnownCustomProperties() {
		// Arrange:
		final Properties properties = getCustomProperties();
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

		// Act:
		final Collection<NemProperty> nemProperties = mosaicProperties.asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(new NemProperty("divisibility", "2"),
				new NemProperty("initialSupply", "123456"), new NemProperty("supplyMutable", "true"),
				new NemProperty("transferable", "false"));
		MatcherAssert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
	}

	@Test
	public void asCollectionDoesNotReturnUnknownProperties() {
		// Arrange:
		final Properties properties = new Properties();
		properties.put("divisibility", "4");
		properties.put("random", "this property should not show up");
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

		// Act:
		final Collection<NemProperty> nemProperties = mosaicProperties.asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(new NemProperty("divisibility", "4"),
				new NemProperty("initialSupply", "1000"), new NemProperty("supplyMutable", "false"),
				new NemProperty("transferable", "true"));
		MatcherAssert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
	}

	// endregion

	// region equals / hashCode

	private static Map<String, DefaultMosaicProperties> createMosaicPropertiesForEqualityTests() {
		final Map<String, DefaultMosaicProperties> map = new HashMap<>();
		map.put("default", new DefaultMosaicProperties(getCustomProperties()));

		final Properties propertiesWithDiffPropertyValue = getCustomProperties();
		propertiesWithDiffPropertyValue.setProperty("divisibility", "4");
		map.put("diff-property-value", new DefaultMosaicProperties(propertiesWithDiffPropertyValue));

		final Properties propertiesWithOneLessProperty = getCustomProperties();
		propertiesWithOneLessProperty.remove("divisibility");
		map.put("one-less-property", new DefaultMosaicProperties(propertiesWithOneLessProperty));

		final Properties propertiesWithOneMoreProperty = getCustomProperties();
		propertiesWithOneMoreProperty.setProperty("random", "123");
		map.put("one-more-property", new DefaultMosaicProperties(propertiesWithOneMoreProperty));

		return map;
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		// note: equals is using asCollection() method for testing equality
		final DefaultMosaicProperties properties = new DefaultMosaicProperties(getCustomProperties());

		// Assert:
		for (final Map.Entry<String, DefaultMosaicProperties> entry : createMosaicPropertiesForEqualityTests().entrySet()) {
			MatcherAssert.assertThat(entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(properties)) : IsEqual.equalTo(properties));
		}

		MatcherAssert.assertThat(properties, IsNot.not(IsEqual.equalTo("foo")));
		MatcherAssert.assertThat(properties, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		// note: hashCode is using asCollection() method for calculating the hashCode
		final int hashCode = new DefaultMosaicProperties(getCustomProperties()).hashCode();

		// Assert:
		for (final Map.Entry<String, DefaultMosaicProperties> entry : createMosaicPropertiesForEqualityTests().entrySet()) {
			MatcherAssert.assertThat(entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		switch (propertyName) {
			case "default":
			case "one-more-property": // note that the "extra" property is masked out, so the remaining properties are equal
				return false;
		}

		return true;
	}

	// endregion

	private static Properties getCustomProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "2");
		properties.put("initialSupply", "123456");
		properties.put("supplyMutable", "true");
		properties.put("transferable", "false");
		return properties;
	}
}
