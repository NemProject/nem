package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.primitive.*;

public class MosaicConstantsTest {
	private static final PublicKey NAMESPACE_OWNER_NEM_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	private static final Quantity NEM_XEM_SUPPLY = Quantity.fromValue(8_999_999_999L);

	// region nem namespace

	@Test
	public void namespaceOwnerNemHasExpectedPublicKey() {
		// Assert:
		Assert.assertThat(MosaicConstants.NAMESPACE_OWNER_NEM.getAddress().getPublicKey(), IsEqual.equalTo(NAMESPACE_OWNER_NEM_KEY));
	}

	@Test
	public void namespaceNemHasExpectedProperties() {
		// Assert:
		Assert.assertThat(MosaicConstants.NAMESPACE_NEM.getId(), IsSame.sameInstance(MosaicConstants.NAMESPACE_ID_NEM));
		Assert.assertThat(MosaicConstants.NAMESPACE_NEM.getOwner(), IsSame.sameInstance(MosaicConstants.NAMESPACE_OWNER_NEM));
		Assert.assertThat(MosaicConstants.NAMESPACE_NEM.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	// endregion

	// region xem mosaic definition

	@Test
	public void mosaicDefinitionXemHasExpectedProperties() {
		// Assert:
		Assert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getCreator(), IsEqual.equalTo(MosaicConstants.NAMESPACE_OWNER_NEM));
		Assert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("reserved xem mosaic")));
		Assert.assertThat(MosaicConstants.MOSAIC_DEFINITION_XEM.getId(), IsEqual.equalTo(new MosaicId(MosaicConstants.NAMESPACE_ID_NEM, "xem")));

		final MosaicProperties properties = MosaicConstants.MOSAIC_DEFINITION_XEM.getProperties();
		Assert.assertThat(properties.asCollection().size(), IsEqual.equalTo(4));
		Assert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY.getRaw()));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(6));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
		Assert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(false));
	}

	// endregion
}