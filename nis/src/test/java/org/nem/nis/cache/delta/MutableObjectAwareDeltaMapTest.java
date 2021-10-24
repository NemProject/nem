package org.nem.nis.cache.delta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;
import org.nem.nis.state.AccountState;

public class MutableObjectAwareDeltaMapTest {

	// region ctor

	@Test
	public void canCreateMutableObjectAwareDeltaMap() {
		// Act:
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Assert:
		MatcherAssert.assertThat(deltaMap.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region getOrDefault

	@Test
	public void getOrDefaultReturnsExistingValueIfKeyIsFound() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);
		final MutableObjectAwareDeltaMap<Address, AccountState> mutableDeltaMap = deltaMap.rebase();
		mutableDeltaMap.put(address, state);
		mutableDeltaMap.commit();

		// Act:
		final AccountState foundState = deltaMap.getOrDefault(address, null);

		// Assert:
		MatcherAssert.assertThat(foundState, IsSame.sameInstance(state));
	}

	@Test
	public void getOrDefaultReturnsDefaultValueIfKeyIsNotFound() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState defaultState = new AccountState(address);
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Act:
		final AccountState foundState = deltaMap.getOrDefault(address, defaultState);

		// Assert:
		MatcherAssert.assertThat(foundState, IsSame.sameInstance(defaultState));
	}

	// endregion

	// region trying to modify delta map that is not a copy

	@Test
	public void cannotPutKeyValuePairIfDeltaMapIsNotACopy() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Assert:
		ExceptionAssert.assertThrows(v -> deltaMap.put(address, state), IllegalStateException.class);
	}

	@Test
	public void cannotRemoveKeyValuePairIfDeltaMapIsNotACopy() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Assert:
		ExceptionAssert.assertThrows(v -> deltaMap.remove(address), IllegalStateException.class);
	}

	@Test
	public void cannotStreamValuesIfDeltaMapIsNotACopy() {
		// Arrange:
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Assert:
		ExceptionAssert.assertThrows(v -> deltaMap.streamValues(), IllegalStateException.class);
	}

	@Test
	public void cannotCommitIfDeltaMapIsNotACopy() {
		// Arrange:
		final MutableObjectAwareDeltaMap<Address, AccountState> deltaMap = new MutableObjectAwareDeltaMap<>(1234);

		// Assert:
		ExceptionAssert.assertThrows(v -> deltaMap.commit(), IllegalStateException.class);
	}

	// endregion
}
