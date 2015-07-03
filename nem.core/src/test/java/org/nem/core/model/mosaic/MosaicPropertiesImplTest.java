package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.ExceptionAssert;
import wiremock.org.apache.commons.lang.StringUtils;

import java.util.*;

// TODO 20150702 J-J review these tests in more detail

public class MosaicPropertiesImplTest {

	// region ctor

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
		ExceptionAssert.assertThrows(v -> new MosaicPropertiesImpl((NemProperties)null), IllegalArgumentException.class);
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

	// TODO 20150702 J-J: ignoring the test for now until createInvalidPropertiesMap is fixed
	@Test
	public void cannotCreateMosaicPropertiesIfAtLeastOnePropertyIsInvalid() {
		// Arrange:
		final List<NemProperty> list = createInvalidPropertiesList();

		// Assert:
		list.stream().forEach(e -> {
			final Properties p = createInvalidProperties(e.getName(), e.getValue());
			ExceptionAssert.assertThrows(v -> new MosaicPropertiesImpl(p), IllegalArgumentException.class);
		});
	}

	// endregion

	// region delegation

	@Test
	public void asCollectionDelegatesToNemProperties() {
		// Arrange:
		final NemProperties properties = Mockito.spy(new NemProperties(getRequiredProperties()));
		final MosaicProperties mosaicProperties = new MosaicPropertiesImpl(properties);

		// Act:
		mosaicProperties.asCollection();

		// Assert:
		Mockito.verify(properties, Mockito.times(1)).asCollection();
	}

	// endregion

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

	private static List<NemProperty> createInvalidPropertiesList() {
		// TODO 20150720 J-B: this is clearly not what you want as the map will only have one value for each key ^^
		// > you really want to return a list of pairs instead
		// TODO 20150703 BR -> J: lol yes.
		final List<NemProperty> list = new ArrayList<>();
		final String shortOne = "This string is too long.";
		list.add(new NemProperty("description", StringUtils.repeat(shortOne, 512 / shortOne.length() + 1)));
		list.add(new NemProperty("description", ""));
		list.add(new NemProperty("description", "      "));
		list.add(new NemProperty("divisibility", "-1"));
		list.add(new NemProperty("divisibility", "7"));
		list.add(new NemProperty("name", ""));
		list.add(new NemProperty("name", "-name"));
		list.add(new NemProperty("name", "_name"));
		list.add(new NemProperty("name", " name"));
		list.add(new NemProperty("name", "This name is too long. This name is too long. "));
		list.add(new NemProperty("namespace", "invalid namespace"));
		return list;
	}
}
