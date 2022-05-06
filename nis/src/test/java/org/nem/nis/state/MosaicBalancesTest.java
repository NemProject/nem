package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.*;

public class MosaicBalancesTest {

	// region constructor

	@Test
	public void mapIsInitiallyEmpty() {
		// Act:
		final MosaicBalances balances = new MosaicBalances();

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region get

	@Test
	public void getReturnsZeroForUnknownAddress() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();
		balances.incrementBalance(address, new Quantity(221));

		// Act:
		final Quantity quantity = balances.getBalance(Utils.generateRandomAddress());

		// Assert:
		MatcherAssert.assertThat(quantity, IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void getReturnsAccurateBalanceForKnownAddress() {
		// Act:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();
		balances.incrementBalance(address, new Quantity(221));

		// Act:
		final Quantity quantity = balances.getBalance(address);

		// Assert:
		MatcherAssert.assertThat(quantity, IsEqual.equalTo(new Quantity(221)));
	}

	// endregion

	// region getOwners

	@Test
	public void getOwnersReturnsEmptyCollectionIfMosaicHasNoOwners() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();

		// Act:
		final Collection<Address> owners = balances.getOwners();

		// Assert:
		MatcherAssert.assertThat(owners.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getOwnersReturnsAllOwners() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Collection<Address> expectedOwners = new HashSet<>();
		for (int i = 0; i < 10; ++i) {
			final Address address = Utils.generateRandomAddress();
			expectedOwners.add(address);
			balances.incrementBalance(address, new Quantity(123));
		}

		// Act:
		final Collection<Address> owners = balances.getOwners();

		// Assert:
		MatcherAssert.assertThat(owners, IsEquivalent.equivalentTo(expectedOwners));
	}

	// endregion

	// region add

	@Test
	public void canIncrementBalanceForSingleAccount() {
		// Act:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address, new Quantity(221));

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address), IsEqual.equalTo(new Quantity(221)));
	}

	@Test
	public void canIncrementBalanceForSingleAccountMultipleTimes() {
		// Act:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address, new Quantity(221));
		balances.incrementBalance(address, new Quantity(373));

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address), IsEqual.equalTo(new Quantity(594)));
	}

	@Test
	public void canIncrementBalanceForMultipleAccounts() {
		// Act:
		final MosaicBalances balances = new MosaicBalances();
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address1, new Quantity(221));
		balances.incrementBalance(address2, new Quantity(373));
		final Quantity quantity1 = balances.getBalance(address1);
		final Quantity quantity2 = balances.getBalance(address2);

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(quantity1, IsEqual.equalTo(new Quantity(221)));
		MatcherAssert.assertThat(quantity2, IsEqual.equalTo(new Quantity(373)));
	}

	@Test
	public void incrementAutoPrunesAccountsWithZeroBalances() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address1, Quantity.ZERO);
		balances.incrementBalance(address2, new Quantity(373));

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address1), IsEqual.equalTo(Quantity.ZERO));
		MatcherAssert.assertThat(balances.getBalance(address2), IsEqual.equalTo(new Quantity(373)));
	}

	// endregion

	// region decrementBalance

	@Test
	public void canDecrementBalanceToNonZero() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address, new Quantity(221));
		balances.decrementBalance(address, new Quantity(21));

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address), IsEqual.equalTo(new Quantity(200)));
	}

	@Test
	public void decrementAutoPrunesAccountsWithZeroBalances() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address1, new Quantity(221));
		balances.incrementBalance(address2, new Quantity(373));
		balances.decrementBalance(address1, new Quantity(221));

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address1), IsEqual.equalTo(Quantity.ZERO));
		MatcherAssert.assertThat(balances.getBalance(address2), IsEqual.equalTo(new Quantity(373)));
	}

	@Test
	public void cannotDecrementBalanceToSubZero() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address = Utils.generateRandomAddress();

		// Act:
		balances.incrementBalance(address, new Quantity(221));
		ExceptionAssert.assertThrows(v -> balances.decrementBalance(address, new Quantity(222)), IllegalArgumentException.class);

		// Assert:
		MatcherAssert.assertThat(balances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(balances.getBalance(address), IsEqual.equalTo(new Quantity(221)));
	}

	// endregion

	// region copy

	@Test
	public void canCreateBalancesCopy() {
		// Arrange:
		final MosaicBalances balances = new MosaicBalances();
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		balances.incrementBalance(address1, new Quantity(221));
		balances.incrementBalance(address2, new Quantity(373));

		// Act:
		final MosaicBalances copy = balances.copy();
		balances.incrementBalance(address2, new Quantity(27));

		// Assert: only the quantity in the original was updated
		MatcherAssert.assertThat(balances.getBalance(address2), IsEqual.equalTo(new Quantity(400)));

		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(copy.getBalance(address1), IsEqual.equalTo(new Quantity(221)));
		MatcherAssert.assertThat(copy.getBalance(address2), IsEqual.equalTo(new Quantity(373)));
	}

	// endregion
}
