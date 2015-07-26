package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.core.test.*;

import java.util.*;

public class DefaultMosaicPropertiesTest {

	//region ctor

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
		final Collection<NemProperty> nemProperties = Arrays.asList(
				new NemProperty("divisibility", "2"),
				new NemProperty("initialSupply", "123456"),
				new NemProperty("supplyMutable", "true"),
				new NemProperty("transferable", "false"));
		final MosaicProperties properties = new DefaultMosaicProperties(nemProperties);

		// Assert:
		assertCustomProperties(properties);
	}

	private static void assertDefaultProperties(final MosaicProperties properties) {
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(0));
		Assert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(1_000L));
		Assert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(false));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
	}

	private static void assertCustomProperties(final MosaicProperties properties) {
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(2));
		Assert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(123456L));
		Assert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(true));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(false));
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
			Assert.assertThat(mosaicProperties.getInitialSupply(), IsEqual.equalTo(quantity));
		}
	}

	@Test
	public void cannotCreateMosaicPropertiesAroundNullProperties() {
		ExceptionAssert.assertThrows(v -> new DefaultMosaicProperties((Properties)null), IllegalArgumentException.class);
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
		invalidProperties.stream().forEach(e ->
				ExceptionAssert.assertThrows(
						v -> new DefaultMosaicProperties(Collections.singletonList(e)),
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
		ExceptionAssert.assertThrows(
				v -> new DefaultMosaicProperties(properties),
				IllegalArgumentException.class);
	}

	//endregion

	//region asCollection

	@Test
	public void asCollectionReturnsAllKnownDefaultProperties() {
		// Arrange:
		final Properties properties = new Properties();
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

		// Act:
		final Collection<NemProperty> nemProperties = mosaicProperties.asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(
				new NemProperty("divisibility", "0"),
				new NemProperty("initialSupply", "1000"),
				new NemProperty("supplyMutable", "false"),
				new NemProperty("transferable", "true"));
		Assert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
	}

	@Test
	public void asCollectionReturnsAllKnownCustomProperties() {
		// Arrange:
		final Properties properties = getCustomProperties();
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

		// Act:
		final Collection<NemProperty> nemProperties = mosaicProperties.asCollection();

		// Assert:
		final Collection<NemProperty> expectedProperties = Arrays.asList(
				new NemProperty("divisibility", "2"),
				new NemProperty("initialSupply", "123456"),
				new NemProperty("supplyMutable", "true"),
				new NemProperty("transferable", "false"));
		Assert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
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
		final Collection<NemProperty> expectedProperties = Arrays.asList(
				new NemProperty("divisibility", "4"),
				new NemProperty("initialSupply", "1000"),
				new NemProperty("supplyMutable", "false"),
				new NemProperty("transferable", "true"));
		Assert.assertThat(nemProperties, IsEquivalent.equivalentTo(expectedProperties));
	}

	//endregion

	private static Properties getCustomProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "2");
		properties.put("initialSupply", "123456");
		properties.put("supplyMutable", "true");
		properties.put("transferable", "false");
		return properties;
	}
}
