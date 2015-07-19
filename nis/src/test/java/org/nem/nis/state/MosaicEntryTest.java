package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.Properties;

public class MosaicEntryTest {

	//region constructor / copy

	@Test
	public void canCreateEntryWithInitialSupply() {
		// Act:
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(0, 123456));
		final MosaicEntry entry = new MosaicEntry(mosaic);

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Quantity(123456)));
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
	public void cannotCreateEntryWithExplicitSupplyTooLarge() {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(1, 1000));

		// Act:
		ExceptionAssert.assertThrows(
				v -> new MosaicEntry(mosaic, new Quantity(MosaicConstants.MAX_QUANTITY)),
				IllegalArgumentException.class);
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

	@Test
	public void cannotIncreaseSupplyIfResultingSupplyIsTooLarge() {
		// Arrange:
		final Quantity supply = new Quantity(MosaicConstants.MAX_QUANTITY - 5000);
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(0, 1000));
		final MosaicEntry entry = new MosaicEntry(mosaic, supply);

		// Act:
		ExceptionAssert.assertThrows(v -> entry.increaseSupply(new Quantity(5001)), IllegalArgumentException.class);

		// Assert: supply is unchanged
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(supply));
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

	private static MosaicProperties createMosaicProperties(final int divisibility, final long quantity) {
		final Properties properties = new Properties();
		properties.put("divisibility", Integer.toString(divisibility));
		properties.put("quantity", Long.toString(quantity));
		return new DefaultMosaicProperties(properties);
	}
}
