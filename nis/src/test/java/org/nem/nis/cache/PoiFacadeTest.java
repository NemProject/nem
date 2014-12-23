package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.NemesisBlock;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.state.AccountState;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PoiFacadeTest<T extends CopyableCache<T> & PoiFacade> {

	/**
	 * Creates a poi facade given an importance calculator.
	 *
	 * @param importanceCalculator The importance calculator.
	 * @return The poi facade
	 */
	protected abstract T createPoiFacade(final ImportanceCalculator importanceCalculator);

	/**
	 * Creates a poi facade.
	 *
	 * @return The poi facade
	 */
	protected T createPoiFacade() {
		return this.createPoiFacade(Mockito.mock(ImportanceCalculator.class));
	}

	//region copy

	@Test
	public void copyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final T facade = this.createPoiFacade(importanceCalculator);
		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Act:
		final PoiFacade copyFacade = facade.copy();

		copyFacade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Assert: recalculate was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), Mockito.any());
	}

	//endregion

	//region shallowCopyTo

	@Test
	public void shallowCopyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final T facade = this.createPoiFacade(importanceCalculator);
		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Act:
		final T copyFacade = this.createPoiFacade();
		facade.shallowCopyTo(copyFacade);

		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Assert: recalculate was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), Mockito.any());
	}

	//endregion

	//region recalculateImportances

	/**
	 * The grouping calculation is always based on the previous block height:
	 * - GroupedHeight.fromHeight(359 * N + 1) == 359 * N
	 * - GroupedHeight.fromHeight(359 * N) == 359 * (N - 1)
	 * X == GroupedHeight.fromHeight(X) is only true when X == 1.
	 * createAccountStatesForRecalculateTests creates account using GROUPING * X * 10,
	 * which is equal to the grouped height of (GROUPING * X * 10 + 1).
	 */

	private final static BlockHeight G_HEIGHT_A1 = new BlockHeight(359 * 10);
	private final static BlockHeight G_HEIGHT_A2 = new BlockHeight(359 * 20);
	private final static BlockHeight G_HEIGHT_20 = new BlockHeight(359 * 20 + 1);
	private final static BlockHeight G_HEIGHT_A3 = new BlockHeight(359 * 30);
	private final static BlockHeight G_HEIGHT_70 = new BlockHeight(359 * 70 + 1);
	private final static BlockHeight G_HEIGHT_70_PLUS = new BlockHeight(359 * 70 + 200);
	private final static List<BlockHeight> G_HEIGHTS_A1_TO_A3 = Arrays.asList(G_HEIGHT_A1, G_HEIGHT_A2, G_HEIGHT_A3);

	@Test
	public void recalculateImportancesDelegatesToImportanceGenerator() {
		// Arrange:
		final BlockHeight height = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with three accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.eq(G_HEIGHT_70.prev()), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(G_HEIGHTS_A1_TO_A3));
	}

	@Test
	public void recalculateImportancesIgnoresAccountsWithGreaterHeight() {
		// Arrange:
		final BlockHeight height = G_HEIGHT_20;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with two accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.eq(G_HEIGHT_20.prev()), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(G_HEIGHT_A1, G_HEIGHT_A2)));
	}

	@Test
	public void recalculateImportancesIgnoresNemesisAccount() {
		// Arrange:
		final BlockHeight height = G_HEIGHT_70;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);
		accountStates.add(new AccountState(NemesisBlock.ADDRESS));

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with three accounts (but not the nemesis account)
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.eq(G_HEIGHT_70.prev()), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(G_HEIGHTS_A1_TO_A3));
	}

	@Test
	public void recalculateImportancesDoesNotRecalculateImportancesForLastBlockHeight() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);

		// Act:
		facade.recalculateImportances(G_HEIGHT_20, new ArrayList<>());
		facade.recalculateImportances(G_HEIGHT_20, new ArrayList<>());

		// Assert: the generator was only called once
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.eq(G_HEIGHT_20.prev()), Mockito.any());
	}

	@Test
	public void recalculateImportancesRecalculatesImportancesForNewBlockHeight() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_20;
		final BlockHeight height2 = G_HEIGHT_70;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height1, accountStates);
		facade.recalculateImportances(height2, accountStates);

		// Assert: the generator was called twice and passed a collection with three accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(2)).recalculate(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(G_HEIGHTS_A1_TO_A3));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiVectorSize() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height1, accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoiVectorSize(), IsEqual.equalTo(3));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiRecalculationHeight() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height1, accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoiRecalculationHeight(), IsEqual.equalTo(G_HEIGHT_70.prev()));
	}

	private static List<AccountState> createAccountStatesForRecalculateTests(final int numAccounts) {
		final List<AccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < numAccounts; ++i) {
			accountStates.add(new AccountState(Utils.generateRandomAddress()));
			accountStates.get(i).setHeight(new BlockHeight(359 * ((i + 1) * 10)));
		}

		return accountStates;
	}

	private List<BlockHeight> heightsAsList(final Collection<AccountState> accountStates) {
		return accountStates.stream()
				.map(as -> as.getHeight())
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Collection<AccountState>> createAccountStateCollectionArgumentCaptor() {
		return ArgumentCaptor.forClass((Class)Collection.class);
	}

	//endregion
}