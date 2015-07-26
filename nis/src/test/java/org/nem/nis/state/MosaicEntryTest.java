package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.Properties;

public class MosaicEntryTest {
	private static final int DEFAULT_DIVISIBILITY = 3;

	//region constructor

	@Test
	public void canCreateEntryWithInitialSupply() {
		// Act:
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(0, 123456));
		final MosaicEntry entry = new MosaicEntry(mosaic);

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		assertSupply(entry, 0, new Supply(123456));
	}

	@Test
	public void canCreateEntryWithExplicitSupply() {
		// Act:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		assertSupply(entry, new Supply(474));
	}

	@Test
	public void cannotCreateEntryWithExplicitSupplyTooLarge() {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(1, 1000));

		// Act:
		ExceptionAssert.assertThrows(
				v -> new MosaicEntry(mosaic, new Supply(MosaicConstants.MAX_QUANTITY)),
				IllegalArgumentException.class);
	}

	//endregion

	//region copy

	@Test
	public void canCreateEntryCopy() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));

		// Act:
		final MosaicEntry copy = entry.copy();
		entry.increaseSupply(new Supply(111));

		// Assert: only the entry's supply was updated
		assertSupply(entry, new Supply(585));

		Assert.assertThat(copy.getMosaic(), IsEqual.equalTo(Utils.createMosaic(3)));
		assertSupply(copy, new Supply(474));
	}

	@Test
	public void entryCopyIncludesAllBalances() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));
		entry.getBalances().incrementBalance(address1, new Quantity(11));
		entry.getBalances().incrementBalance(address2, new Quantity(22));
		entry.getBalances().incrementBalance(address3, new Quantity(33));

		// Assert:
		final Quantity expectedCreatorBalance = MosaicUtils.toQuantity(new Supply(474), DEFAULT_DIVISIBILITY);
		Assert.assertThat(entry.getBalances().size(), IsEqual.equalTo(4));
		Assert.assertThat(getCreatorBalance(entry), IsEqual.equalTo(expectedCreatorBalance));
		Assert.assertThat(entry.getBalances().getBalance(address1), IsEqual.equalTo(new Quantity(11)));
		Assert.assertThat(entry.getBalances().getBalance(address2), IsEqual.equalTo(new Quantity(22)));
		Assert.assertThat(entry.getBalances().getBalance(address3), IsEqual.equalTo(new Quantity(33)));
	}

	//endregion

	// region increaseSupply

	@Test
	public void canIncreaseSupply() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));

		// Act:
		entry.increaseSupply(new Supply(234));

		// Assert:
		assertSupply(entry, new Supply(708));
	}

	@Test
	public void cannotIncreaseSupplyIfResultingSupplyIsTooLarge() {
		// Arrange:
		final Supply supply = new Supply(MosaicConstants.MAX_QUANTITY - 5000);
		final Mosaic mosaic = Utils.createMosaic(3, createMosaicProperties(0, 1000));
		final MosaicEntry entry = new MosaicEntry(mosaic, supply);

		// Act:
		ExceptionAssert.assertThrows(v -> entry.increaseSupply(new Supply(5001)), IllegalArgumentException.class);

		// Assert: supply is unchanged
		assertSupply(entry, 0, supply);
	}

	// endregion

	// region decreaseSupply

	@Test
	public void canDecreaseSupply() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));

		// Act:
		entry.decreaseSupply(new Supply(234));

		// Assert:
		assertSupply(entry, new Supply(240));
	}

	@Test
	public void cannotDecreaseSupplyIfCurrentSupplyIsTooSmall() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaic(3), new Supply(474));

		// Assert:
		ExceptionAssert.assertThrows(v -> entry.decreaseSupply(new Supply(475)), NegativeQuantityException.class);
	}

	// endregion

	private static MosaicProperties createMosaicProperties(final int divisibility, final long quantity) {
		final Properties properties = new Properties();
		properties.put("divisibility", Integer.toString(divisibility));
		properties.put("quantity", Long.toString(quantity));
		return new DefaultMosaicProperties(properties);
	}

	private static void assertSupply(final MosaicEntry entry, final Supply expectedSupply) {
		assertSupply(entry, DEFAULT_DIVISIBILITY, expectedSupply);
	}

	private static void assertSupply(final MosaicEntry entry, final int divisibility, final Supply expectedSupply) {
		// supply increases / decreases should affect the mosaic creator's balance
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(expectedSupply));
		Assert.assertThat(entry.getBalances().size(), IsEqual.equalTo(1));
		Assert.assertThat(getCreatorBalance(entry), IsEqual.equalTo(MosaicUtils.toQuantity(expectedSupply, divisibility)));
	}

	private static Quantity getCreatorBalance(final MosaicEntry entry) {
		return entry.getBalances().getBalance(entry.getMosaic().getCreator().getAddress());
	}
}
