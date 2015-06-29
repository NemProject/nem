package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class MosaicPropertiesImplTest {

	@Test
	public void canCreateMosaicPropertiesFromOnlyRequiredProperties() {
		// Act:
		final MosaicProperties properties = new MosaicPropertiesImpl(getRequiredProperties());

		// Assert:
		Assert.assertThat(properties.getName(), IsEqual.equalTo("Alice's gift vouchers"));
		Assert.assertThat(properties.getNamespaceId(), IsEqual.equalTo(new NamespaceId("alice.vouchers")));
		Assert.assertThat(properties.getDescription(), IsEqual.equalTo("No description available"));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(0));
		Assert.assertThat(properties.isQuantityMutable(), IsEqual.equalTo(false));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateMosaicPropertiesFromCustomProperties() {
		// Act:
		final MosaicProperties properties = new MosaicPropertiesImpl(getCustomProperties());

		// Assert:
		Assert.assertThat(properties.getName(), IsEqual.equalTo("Bob's gift vouchers"));
		Assert.assertThat(properties.getNamespaceId(), IsEqual.equalTo(new NamespaceId("bob.vouchers")));
		Assert.assertThat(properties.getDescription(), IsEqual.equalTo("This mosaic represents Bob's gift vouchers"));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(2));
		Assert.assertThat(properties.isQuantityMutable(), IsEqual.equalTo(true));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(false));
	}

	@Test
	public void cannotCreateMosaicPropertiesFromNullProperties() {
		ExceptionAssert.assertThrows(v -> new MosaicPropertiesImpl(null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicPropertiesIfRequiredPropertyIsMissing() {
		// Arrange:
		final List<String> propertiesToRemove = Arrays.asList("name", "namespace");

		// Assert:
		propertiesToRemove.stream().forEach(s -> {
			final Properties p = getRequiredProperties();
			p.remove(s);
			ExceptionAssert.assertThrows(v -> new MosaicPropertiesImpl(p), RuntimeException.class);
		});
	}

	@Test
	public void cannotCreateMosaicPropertiesIfAtLeastOnePropertyIsInvalid() {
		// Arrange:
		final HashMap<String, String> map = createInvalidPropertiesMap();

		// Assert:
		map.entrySet().stream().forEach(e -> {
			final Properties p = createInvalidProperties(e.getKey(), e.getValue());
			ExceptionAssert.assertThrows(v -> new MosaicPropertiesImpl(p), IllegalArgumentException.class);
		});
	}

	private static Properties createInvalidProperties(final String propertyName, final String invalidValue) {
		final Properties properties = getRequiredProperties();
		properties.put(propertyName, invalidValue);
		return properties;
	}

	private static Properties getRequiredProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return properties;
	}

	private static Properties getCustomProperties() {
		final Properties properties = new Properties();
		properties.put("description", "This mosaic represents Bob's gift vouchers");
		properties.put("divisibility", "2");
		properties.put("mutableQuantity", "true");
		properties.put("name", "Bob's gift vouchers");
		properties.put("namespace", "bob.vouchers");
		properties.put("transferable", "false");
		return properties;
	}

	private static HashMap<String, String> createInvalidPropertiesMap() {
		final HashMap<String, String> map = new HashMap<>();
		map.put(
				"description",
				"This string is too long.  This string is too long.  This string is too long.  This string is too long.  This string is too long.  ");
		map.put("divisibility", "-1");
		map.put("divisibility", "7");
		map.put("name", "");
		map.put("name", "-name");
		map.put("name", "_name");
		map.put("name", " name");
		map.put("name", "This name is too long. This name is too long. ");
		map.put("namespace", "invalid namespace");
		return map;
	}
}
