package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class MosaicEntryTest {

	//region constructor / copy

	@Test
	public void canCreateEntry() {
		// Act:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void canCreateEntryWithExplicitSupply() {
		// Act:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Quantity(474));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Quantity(474)));
	}

	@Test
	public void canCreateEntryCopy() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Quantity(474));

		// Act:
		final MosaicEntry copy = entry.copy();
		entry.increaseSupply(new Quantity(111));

		// Assert: only the entry's supply was updated
		Assert.assertThat(copy.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		Assert.assertThat(copy.getSupply(), IsEqual.equalTo(new Quantity(474)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Quantity(585)));
	}

	//endregion

	// region increaseSupply

	@Test
	public void canIncreaseSupply() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Quantity(474));

		// Act:
		entry.increaseSupply(new Quantity(234));
		final Quantity supply = entry.getSupply();

		// Assert:
		Assert.assertThat(supply, IsEqual.equalTo(new Quantity(708)));
	}

	// endregion

	// region decreaseSupply

	@Test
	public void canDecreaseSupply() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Quantity(474));

		// Act:
		entry.decreaseSupply(new Quantity(234));
		final Quantity supply = entry.getSupply();

		// Assert:
		Assert.assertThat(supply, IsEqual.equalTo(new Quantity(240)));
	}

	@Test
	public void cannotDecreaseSupplyIfCurrentSupplyIsTooSmall() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Quantity(474));

		// Assert:
		ExceptionAssert.assertThrows(v -> entry.decreaseSupply(new Quantity(475)), NegativeQuantityException.class);
	}

	// endregion
}
