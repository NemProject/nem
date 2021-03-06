package org.nem.core.model.mosaic;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.primitive.*;

public class MosaicConstantsTest {
	private static final PublicKey NAMESPACE_OWNER_NEM_KEY = PublicKey
			.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	private static final Quantity NEM_XEM_SUPPLY = Quantity.fromValue(8_999_999_999L);

	// region nem namespace

	@Test
	public void namespaceOwnerNemHasExpectedPublicKey() {
		// Assert:
		MatcherAssert.assertThat(MosaicConstants.NAMESPACE_OWNER_NEM.getAddress().getPublicKey(), IsEqual.equalTo(NAMESPACE_OWNER_NEM_KEY));
	}

	@Test
	public void namespaceNemHasExpectedProperties() {
		// Assert:
		MatcherAssert.assertThat(MosaicConstants.NAMESPACE_NEM.getId(), IsSame.sameInstance(MosaicConstants.NAMESPACE_ID_NEM));
		MatcherAssert.assertThat(MosaicConstants.NAMESPACE_NEM.getOwner(), IsSame.sameInstance(MosaicConstants.NAMESPACE_OWNER_NEM));
		MatcherAssert.assertThat(MosaicConstants.NAMESPACE_NEM.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	// endregion

	// region xem mosaic definition

	@Test
	public void mosaicIdXemHasExpectedProperties() {
		// Assert:
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_ID_XEM.getNamespaceId(), IsEqual.equalTo(MosaicConstants.NAMESPACE_ID_NEM));
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_ID_XEM.getName(), IsEqual.equalTo("xem"));
	}

	@Test
	public void mosaicDefinitionXemHasExpectedProperties() {
		// Assert:
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getCreator(), IsEqual.equalTo(MosaicConstants.NAMESPACE_OWNER_NEM));
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getDescriptor(),
				IsEqual.equalTo(new MosaicDescriptor("reserved xem mosaic")));
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getId(), IsEqual.equalTo(MosaicConstants.MOSAIC_ID_XEM));
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.isMosaicLevyPresent(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getMosaicLevy(), IsNull.nullValue());

		final MosaicProperties properties = MosaicConstants.MOSAIC_DEFINITION_XEM.getProperties();
		MatcherAssert.assertThat(properties.asCollection().size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY.getRaw()));
		MatcherAssert.assertThat(properties.getDivisibility(), IsEqual.equalTo(6));
		MatcherAssert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(false));
	}

	// endregion
}
