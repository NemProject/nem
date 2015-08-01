package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class MosaicTransferFeeInfoTest {

	// region ctor

	@Test
	public void canCreateMosaicTransferFeeInfo() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();

		// Act:
		final MosaicTransferFeeInfo info = createMosaicTransferFeeInfo(recipient);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	// endregion

	// serialization

	@Test
	public void cannotDeserializeWithInvalidRecipient() {
		// Arrange:
		final Deserializer deserializer = createDeserializer("foo");

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicTransferFeeInfo(deserializer), IllegalArgumentException.class);
	}

	@Test
	public void canDeserializeWithValidRecipient() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final Deserializer deserializer = createDeserializer(recipient.getEncoded());

		// Assert:
		final MosaicTransferFeeInfo info =  new MosaicTransferFeeInfo(deserializer);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	@Test
	public void canRoundTripMosaicTransferFeeInfo() {
		// Arrange:
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final MosaicTransferFeeInfo original = createMosaicTransferFeeInfo(recipient);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);

		// Act:
		final MosaicTransferFeeInfo info = new MosaicTransferFeeInfo(deserializer);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	// endregion

	private static void assertMosaicTransferFeeInfo(final MosaicTransferFeeInfo info, final Address recipient) {
		// Assert:
		Assert.assertThat(info.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(info.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(info.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(info.getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	private static MosaicTransferFeeInfo createMosaicTransferFeeInfo(final Address recipient) {
		return new MosaicTransferFeeInfo(
				MosaicTransferFeeType.Absolute,
				recipient,
				Utils.createMosaicId(5),
				Quantity.fromValue(123));
	}

	private static Deserializer createDeserializer(final String recipient) {
		final JsonSerializer serializer = new JsonSerializer();
		final MosaicTransferFeeInfo info = createMosaicTransferFeeInfo(Utils.generateRandomAddress());
		info.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();
		jsonObject.put("recipient", recipient);
		return new JsonDeserializer(serializer.getObject(), null);
	}
}
