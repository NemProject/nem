package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final Mosaic mosaic = new Mosaic(
				creator,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				properties);

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	@Test
	public void cannotCreateMosaicWithNullCreator() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				null,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullId() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				null,
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullDescriptor() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				null,
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullNamespaceId() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				null,
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				null,
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithZeroAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.ZERO,
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaic() {
		// Arrange:
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final Mosaic original = new Mosaic(
				creator,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				properties);

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	private void assertMosaicProperties(final Mosaic mosaic, final Account creator, final MosaicProperties properties) {
		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getId(), IsEqual.equalTo(new MosaicId("Alice's vouchers")));
		Assert.assertThat(mosaic.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaic.getNamespaceId(), IsEqual.equalTo(new NamespaceId("alice.vouchers")));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getProperties().asCollection(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
	}

	// TODO 20150207 J-J should we have tests that validate we can't deserialize with zero amount / null values

	// endregion
}
