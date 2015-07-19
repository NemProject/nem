package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;

public class NamespaceConstantsTest {
	private static final PublicKey NAMESPACE_OWNER_NEM_KEY = PublicKey.fromHexString("3e82e1c1e4a75adaa3cba8c101c3cd31d9817a2eb966eb3b511fb2ed45b8e262");
	private static final Quantity NEM_XEM_SUPPLY = Quantity.fromValue(8_999_999_999_000_000L);

	// region nem namespace

	@Test
	public void namespaceOwnerNemHasExpectedPublicKey() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_OWNER_NEM.getAddress().getPublicKey(), IsEqual.equalTo(NAMESPACE_OWNER_NEM_KEY));
	}

	@Test
	public void namespaceNemHasExpectedProperties() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getId(), IsSame.sameInstance(NamespaceConstants.NAMESPACE_ID_NEM));
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getOwner(), IsSame.sameInstance(NamespaceConstants.NAMESPACE_OWNER_NEM));
		Assert.assertThat(NamespaceConstants.NAMESPACE_NEM.getHeight(), IsEqual.equalTo(BlockHeight.MAX));
	}

	// endregion

	// region nem namespace entry

	@Test
	public void namespaceEntryNemHasExpectedNamespace() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getNamespace(), IsSame.sameInstance(NamespaceConstants.NAMESPACE_NEM));
	}

	@Test
	public void namespaceEntryNemHasExpectedMosaicEntry() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(mosaicEntry.getMosaic(), IsSame.sameInstance(NamespaceConstants.MOSAIC_XEM));
		Assert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	@Test
	public void namespaceEntryNemCannotHaveMosaicsAdded() {
		// Arrange:
		final NamespaceEntry entry = NamespaceConstants.NAMESPACE_ENTRY_NEM;

		// Act:
		ExceptionAssert.assertThrows(
				v -> entry.getMosaics().add(Utils.createMosaic(12)),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
	}

	@Test
	public void namespaceEntryNemCannotHaveMosaicsRemoved() {
		// Arrange:
		final NamespaceEntry entry = NamespaceConstants.NAMESPACE_ENTRY_NEM;

		// Act:
		ExceptionAssert.assertThrows(
				v -> entry.getMosaics().remove(NamespaceConstants.MOSAIC_XEM.getId()),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
	}

	// endregion

	// region xem mosaic

	@Test
	public void mosaicXemHasExpectedProperties() {
		// Assert:
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getCreator(), IsEqual.equalTo(NamespaceConstants.NAMESPACE_OWNER_NEM));
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("reserved xem mosaic")));
		Assert.assertThat(NamespaceConstants.MOSAIC_XEM.getId(), IsEqual.equalTo(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem")));

		final MosaicProperties properties = NamespaceConstants.MOSAIC_XEM.getProperties();
		Assert.assertThat(properties.asCollection().size(), IsEqual.equalTo(4));
		Assert.assertThat(properties.getInitialQuantity(), IsEqual.equalTo(NEM_XEM_SUPPLY.getRaw()));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(6));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
		Assert.assertThat(properties.isQuantityMutable(), IsEqual.equalTo(false));
	}

	@Test
	public void mosaicXemCannotHaveSupplyIncreased() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Act:
		ExceptionAssert.assertThrows(
				v -> mosaicEntry.increaseSupply(new Quantity(1)),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	@Test
	public void mosaicXemCannotHaveSupplyDecreased() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Act:
		ExceptionAssert.assertThrows(
				v -> mosaicEntry.decreaseSupply(new Quantity(1)),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	// endregion

	private static MosaicEntry getMosaicXemEntry() {
		return NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().get(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem"));
	}
}
