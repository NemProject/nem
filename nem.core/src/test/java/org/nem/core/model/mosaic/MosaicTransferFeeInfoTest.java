package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class MosaicTransferFeeInfoTest {

	// region ctor

	@Test
	public void canCreateMosaicTransferFeeInfo() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();

		// Act:
		final MosaicTransferFeeInfo info = createMosaicTransferFeeInfo(recipient, 5);

		// Assert:
		Assert.assertThat(info.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(info.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(info.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(info.getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	// endregion

	// serialization

	// TODO 20150801 BR -> J: any need to test serialize and deserialize separately?
	@Test
	public void canRoundTripMosaicTransferFeeInfo() {
		// Arrange:
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final MosaicTransferFeeInfo original = createMosaicTransferFeeInfo(recipient, 5);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);

		// Act:
		final MosaicTransferFeeInfo info = new MosaicTransferFeeInfo(deserializer);

		// Assert:
		Assert.assertThat(info.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(info.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(info.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(info.getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	// endregion

	private MosaicTransferFeeInfo createMosaicTransferFeeInfo(final Address recipient, final int id) {
		return new MosaicTransferFeeInfo(
				MosaicTransferFeeType.Absolute,
				recipient,
				Utils.createMosaicId(id),
				Quantity.fromValue(123));
	}
}
