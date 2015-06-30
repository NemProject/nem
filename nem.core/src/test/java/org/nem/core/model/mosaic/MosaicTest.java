package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.*;

import java.util.Properties;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final Properties properties = createProperties();

		// Act:
		final Mosaic mosaic = new Mosaic(creator, properties);

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getName(), IsEqual.equalTo("Alice's gift vouchers"));
	}

	@Test
	public void cannotCreateMosaicWithNullCreator() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(null, createProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), (Properties)null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullMosaicProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), (MosaicProperties)null), IllegalArgumentException.class);
	}

	// endregion

	// region delegation

	@Test
	public void getPropertiesDelegatesToMosaicProperties() {
		// Arrange:
		final MosaicProperties properties = Mockito.spy(new MosaicPropertiesImpl(createProperties()));
		final Mosaic mosaic = new Mosaic(Utils.generateRandomAccount(), properties);

		// Act:
		mosaic.getProperties();

		// Assert:
		Mockito.verify(properties, Mockito.times(1)).asCollection();
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaic() {
		// Arrange:
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final Properties properties = createProperties();
		final Mosaic original = new Mosaic(creator, properties);

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getProperties(), IsEquivalent.equivalentTo(new NemProperties(createProperties()).asCollection()));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	private static Properties createProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return properties;
	}
}
