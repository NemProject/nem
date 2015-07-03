package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;

import java.util.Properties;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = createProperties();

		// Act:
		final Mosaic mosaic = new Mosaic(creator, properties, GenericAmount.fromValue(123));

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getProperties(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(mosaic.getName(), IsEqual.equalTo("Alice's gift vouchers"));
	}

	@Test
	public void cannotCreateMosaicWithNullCreator() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(null, createProperties(), GenericAmount.fromValue(123)), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), null, GenericAmount.fromValue(123)), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), createProperties(), null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithZeroAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(Utils.generateRandomAccount(), createProperties(), GenericAmount.ZERO), IllegalArgumentException.class);
	}

	// endregion

	// region delegation

	@Test
	public void getPropertiesDelegatesToMosaicProperties() {
		// Arrange:
		final MosaicProperties properties = Mockito.spy(createProperties());
		final Mosaic mosaic = new Mosaic(Utils.generateRandomAccount(), properties, GenericAmount.fromValue(123));

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
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = createProperties();
		final Mosaic original = new Mosaic(creator, properties, GenericAmount.fromValue(123));

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getProperties(), IsEquivalent.equivalentTo(createProperties().asCollection()));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(mosaic.getName(), IsEqual.equalTo("Alice's gift vouchers"));
	}

	// TODO 20150207 J-J should we have tests that validate we can't deserialize with zero amount / null values

	// endregion

	private static MosaicProperties createProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return new MosaicPropertiesImpl(properties);
	}
}
