package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class MosaicEntryTest {
	private static final int DEFAULT_DIVISIBILITY = 3;

	// region constructor

	@Test
	public void canCreateEntryWithInitialSupply() {
		// Act:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(3, createMosaicProperties(0, 123456));
		final MosaicEntry entry = new MosaicEntry(mosaicDefinition);

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(Utils.createMosaicDefinition(3)));
		assertSupply(entry, 0, new Supply(123456));
	}

	@Test
	public void canCreateEntryWithExplicitSupply() {
		// Act:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(Utils.createMosaicDefinition(3)));
		assertSupply(entry, new Supply(474));
	}

	@Test
	public void cannotCreateEntryWithExplicitSupplyTooLarge() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(3, createMosaicProperties(1, 1000));

		// Act:
		ExceptionAssert.assertThrows(v -> new MosaicEntry(mosaicDefinition, new Supply(MosaicConstants.MAX_QUANTITY)),
				IllegalArgumentException.class);
	}

	// endregion

	// region copy

	@Test
	public void canCreateEntryCopy() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));

		// Act:
		final MosaicEntry copy = entry.copy();
		entry.increaseSupply(new Supply(111));

		// Assert: only the entry's supply was updated
		assertSupply(entry, new Supply(585));

		MatcherAssert.assertThat(copy.getMosaicDefinition(), IsEqual.equalTo(Utils.createMosaicDefinition(3)));
		assertSupply(copy, new Supply(474));
	}

	@Test
	public void entryCopyIncludesAllBalances() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));
		entry.getBalances().incrementBalance(address1, new Quantity(11));
		entry.getBalances().incrementBalance(address2, new Quantity(22));
		entry.getBalances().incrementBalance(address3, new Quantity(33));

		// Assert:
		final Quantity expectedCreatorBalance = MosaicUtils.toQuantity(new Supply(474), DEFAULT_DIVISIBILITY);
		MatcherAssert.assertThat(entry.getBalances().size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(getCreatorBalance(entry), IsEqual.equalTo(expectedCreatorBalance));
		MatcherAssert.assertThat(entry.getBalances().getBalance(address1), IsEqual.equalTo(new Quantity(11)));
		MatcherAssert.assertThat(entry.getBalances().getBalance(address2), IsEqual.equalTo(new Quantity(22)));
		MatcherAssert.assertThat(entry.getBalances().getBalance(address3), IsEqual.equalTo(new Quantity(33)));
	}

	// endregion

	// region increaseSupply

	@Test
	public void canIncreaseSupply() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));

		// Act:
		entry.increaseSupply(new Supply(234));

		// Assert:
		assertSupply(entry, new Supply(708));
	}

	@Test
	public void cannotIncreaseSupplyIfResultingSupplyIsTooLarge() {
		// Arrange:
		final Supply supply = new Supply(MosaicConstants.MAX_QUANTITY - 5000);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(3, createMosaicProperties(0, 1000));
		final MosaicEntry entry = new MosaicEntry(mosaicDefinition, supply);

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
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));

		// Act:
		entry.decreaseSupply(new Supply(234));

		// Assert:
		assertSupply(entry, new Supply(240));
	}

	@Test
	public void cannotDecreaseSupplyIfCurrentSupplyIsTooSmall() {
		// Arrange:
		final MosaicEntry entry = new MosaicEntry(Utils.createMosaicDefinition(3), new Supply(474));

		// Assert:
		ExceptionAssert.assertThrows(v -> entry.decreaseSupply(new Supply(475)), NegativeQuantityException.class);
	}

	// endregion

	private static MosaicProperties createMosaicProperties(final int divisibility, final long quantity) {
		return Utils.createMosaicProperties(quantity, divisibility, null, null);
	}

	private static void assertSupply(final MosaicEntry entry, final Supply expectedSupply) {
		assertSupply(entry, DEFAULT_DIVISIBILITY, expectedSupply);
	}

	private static void assertSupply(final MosaicEntry entry, final int divisibility, final Supply expectedSupply) {
		// supply increases / decreases should affect the mosaic creator's balance
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(expectedSupply));
		MatcherAssert.assertThat(entry.getBalances().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(getCreatorBalance(entry), IsEqual.equalTo(MosaicUtils.toQuantity(expectedSupply, divisibility)));
	}

	private static Quantity getCreatorBalance(final MosaicEntry entry) {
		return entry.getBalances().getBalance(entry.getMosaicDefinition().getCreator().getAddress());
	}
}
