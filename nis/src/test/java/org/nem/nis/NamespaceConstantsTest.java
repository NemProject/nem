package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.MosaicEntry;

public class NamespaceConstantsTest {
	private static final PublicKey LESSOR_PUBLIC_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");

	// region nem namespace

	@Test
	public void lessorHasExpectedPublicKey() {
		// Assert:
		Assert.assertThat(NamespaceConstants.LESSOR.getAddress().getPublicKey(), IsEqual.equalTo(LESSOR_PUBLIC_KEY));
	}

	@Test
	public void namespaceForNemHasExpectedNamespaceId() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getId(), IsSame.sameInstance(NamespaceConstants.NAMESPACE_ID_NEM));
	}

	@Test
	public void namespaceForNemHasExpectedOwner() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getOwner(), IsSame.sameInstance(NamespaceConstants.LESSOR));
	}

	@Test
	public void namespaceForNemHasExpectedBlockHeight() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	// endregion

	// region nem namespace entry

	@Test
	public void namespaceEntryForNemHasExpectedNamespace() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getNamespace(), IsSame.sameInstance(NamespaceConstants.NAMESPACE_NEM));
	}

	@Test
	public void namespaceEntryForNemHasExpectedMosaicEntry() {
		// Arrange:
		final MosaicEntry mosaicEntry = NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().get(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem"));

		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(mosaicEntry.getMosaic(), IsSame.sameInstance(NamespaceConstants.MOSAIC_XEM));
		Assert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(Quantity.fromValue(8_999_999_999_000_000L)));
	}

	// endregion

	// region xem mosaic

	@Test
	public void mosaicForXemHasExpectedCreator() {
		// Assert:
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getCreator(), IsEqual.equalTo(NamespaceConstants.LESSOR));
	}

	@Test
	public void mosaicForXemHasExpectedDescriptor() {
		// Assert:
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("reserved xem mosaic")));
	}

	@Test
	public void mosaicForXemHasExpectedId() {
		// Assert:
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getId(), IsEqual.equalTo(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem")));
	}

	@Test
	public void mosaicForXemHasExpectedProperties() {
		// Arrange:
		final MosaicProperties properties = NamespaceConstants.MOSAIC_XEM.getProperties();

		// Assert:
		Assert.assertThat(properties.getQuantity(), IsEqual.equalTo(8_999_999_999_000_000L));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(6));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
		Assert.assertThat(properties.isQuantityMutable(), IsEqual.equalTo(false));
	}

	// endregion
}
