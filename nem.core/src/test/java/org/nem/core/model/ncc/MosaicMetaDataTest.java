package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class MosaicMetaDataTest {

	@Test
	public void canCreateMetaData() {
		// Arrange:
		final MosaicMetaData metaData = new MosaicMetaData(321L, new Supply(987));

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
		Assert.assertThat(metaData.getSupply(), IsEqual.equalTo(new Supply(987)));
	}

	@Test
	public void canRoundTripDefaultMetaData() {
		// Arrange:
		final MosaicMetaData metaData = createRoundTrippedMetaData(456L, 398);

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
		Assert.assertThat(metaData.getSupply(), IsEqual.equalTo(new Supply(398)));
	}

	private static MosaicMetaData createRoundTrippedMetaData(final long id, final int supply) {
		// Arrange:
		final MosaicMetaData metaData = new MosaicMetaData(id, new Supply(supply));

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new MosaicMetaData(deserializer);
	}
}