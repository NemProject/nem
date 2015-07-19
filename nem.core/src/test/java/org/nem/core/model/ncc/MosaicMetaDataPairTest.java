package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.*;

public class MosaicMetaDataPairTest {
	@Test
	public void canCreateMosaicMetaDataPair() {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(Utils.generateRandomAccount());
		final DefaultMetaData metaData = new DefaultMetaData(123L);

		// Act:
		final MosaicMetaDataPair entity = new MosaicMetaDataPair(mosaic, metaData);

		// Assert:
		Assert.assertThat(entity.getMosaic(), IsSame.sameInstance(mosaic));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripMosaicMetaDataPair() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();

		// Act:
		final MosaicMetaDataPair metaDataPair = createRoundTrippedPair(creator, 5678);

		// Assert:
		Assert.assertThat(metaDataPair.getMosaic().getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(metaDataPair.getMetaData().getId(), IsEqual.equalTo(5678L));
	}

	private static MosaicMetaDataPair createRoundTrippedPair(final Account creator, final long id) {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(creator);
		final DefaultMetaData metaData = new DefaultMetaData(id);
		final MosaicMetaDataPair entity = new MosaicMetaDataPair(mosaic, metaData);

		// Act:
		return new MosaicMetaDataPair(Utils.roundtripSerializableEntity(entity, new MockAccountLookup()));
	}
}
