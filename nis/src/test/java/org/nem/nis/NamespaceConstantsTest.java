package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.*;
import org.nem.nis.state.*;

public class NamespaceConstantsTest {
	private static final Supply NEM_XEM_SUPPLY = Supply.fromValue(8_999_999_999L);

	// region nem namespace entry

	@Test
	public void namespaceEntryNemHasExpectedNamespace() {
		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getNamespace(), IsSame.sameInstance(MosaicConstants.NAMESPACE_NEM));
	}

	@Test
	public void namespaceEntryNemHasExpectedMosaicEntry() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Assert:
		Assert.assertThat(NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(mosaicEntry.getMosaic(), IsSame.sameInstance(MosaicConstants.MOSAIC_XEM));
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
				v -> entry.getMosaics().remove(MosaicConstants.MOSAIC_XEM.getId()),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
	}

	// endregion

	// region mosaic entry

	@Test
	public void mosaicXemCannotHaveSupplyIncreased() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Act:
		ExceptionAssert.assertThrows(
				v -> mosaicEntry.increaseSupply(new Supply(1)),
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
				v -> mosaicEntry.decreaseSupply(new Supply(1)),
				UnsupportedOperationException.class);

		// Assert:
		Assert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	// endregion

	private static MosaicEntry getMosaicXemEntry() {
		return NamespaceConstants.NAMESPACE_ENTRY_NEM.getMosaics().get(new MosaicId(MosaicConstants.NAMESPACE_ID_NEM, "xem"));
	}
}
