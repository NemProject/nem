package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.*;
import org.nem.nis.state.*;

public class NemNamespaceEntryTest {
	private static final Supply NEM_XEM_SUPPLY = Supply.fromValue(8_999_999_999L);

	@After
	public void resetDefaultNamespaceEntry() {
		NemNamespaceEntry.resetToDefault();
	}

	// region nem namespace entry

	@Test
	public void namespaceEntryNemHasExpectedNamespace() {
		// Assert:
		MatcherAssert.assertThat(NemNamespaceEntry.getDefault().getNamespace(), IsEqual.equalTo(MosaicConstants.NAMESPACE_NEM));
	}

	@Test
	public void namespaceEntryNemHasExpectedMosaicEntry() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Assert:
		MatcherAssert.assertThat(NemNamespaceEntry.getDefault().getMosaics().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mosaicEntry.getMosaicDefinition(), IsEqual.equalTo(MosaicConstants.MOSAIC_DEFINITION_XEM));
		MatcherAssert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	@Test
	public void namespaceEntryNemCannotHaveMosaicsAdded() {
		// Arrange:
		final NamespaceEntry entry = NemNamespaceEntry.getDefault();

		// Act:
		ExceptionAssert.assertThrows(v -> entry.getMosaics().add(Utils.createMosaicDefinition(12)), UnsupportedOperationException.class);

		// Assert:
		MatcherAssert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
	}

	@Test
	public void namespaceEntryNemCannotHaveMosaicsRemoved() {
		// Arrange:
		final NamespaceEntry entry = NemNamespaceEntry.getDefault();

		// Act:
		ExceptionAssert.assertThrows(v -> entry.getMosaics().remove(MosaicConstants.MOSAIC_DEFINITION_XEM.getId()),
				UnsupportedOperationException.class);

		// Assert:
		MatcherAssert.assertThat(entry.getMosaics().size(), IsEqual.equalTo(1));
	}

	@Test
	public void defaultNamespaceEntryCanBeChanged() {
		// Arrange:
		final NamespaceEntry entry = NemNamespaceEntry.getDefault();

		// Act:
		NemNamespaceEntry.setDefault(new BlockHeight(10));

		// Assert:
		MatcherAssert.assertThat(NemNamespaceEntry.getDefault(), IsNot.not(IsEqual.equalTo(entry)));
	}

	@Test
	public void defaultNamespaceEntryCanBeReset() {
		// Arrange:
		final NamespaceEntry entry = NemNamespaceEntry.getDefault();
		NemNamespaceEntry.setDefault(new BlockHeight(2));

		// Act:
		NemNamespaceEntry.resetToDefault();

		// Assert:
		MatcherAssert.assertThat(NemNamespaceEntry.getDefault(), IsSame.sameInstance(entry));
	}

	@Test
	public void defaultNetworkCannotBeChangedAfterBeingSet() {
		// Arrange:
		NemNamespaceEntry.setDefault(new BlockHeight(20));

		// Act:
		ExceptionAssert.assertThrows(v -> NemNamespaceEntry.setDefault(new BlockHeight(1)), IllegalStateException.class);
	}

	// endregion

	// region mosaic entry

	@Test
	public void mosaicXemCannotHaveSupplyIncreased() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Act:
		ExceptionAssert.assertThrows(v -> mosaicEntry.increaseSupply(new Supply(1)), UnsupportedOperationException.class);

		// Assert:
		MatcherAssert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	@Test
	public void mosaicXemCannotHaveSupplyDecreased() {
		// Arrange:
		final MosaicEntry mosaicEntry = getMosaicXemEntry();

		// Act:
		ExceptionAssert.assertThrows(v -> mosaicEntry.decreaseSupply(new Supply(1)), UnsupportedOperationException.class);

		// Assert:
		MatcherAssert.assertThat(mosaicEntry.getSupply(), IsEqual.equalTo(NEM_XEM_SUPPLY));
	}

	// endregion

	private static MosaicEntry getMosaicXemEntry() {
		return NemNamespaceEntry.getDefault().getMosaics().get(new MosaicId(MosaicConstants.NAMESPACE_ID_NEM, "xem"));
	}
}
