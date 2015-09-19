package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.NetworkInfos;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.AccountState;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PoxFacadeTest<T extends CopyableCache<T> & PoxFacade> {

	/**
	 * Creates a pox facade given an importance calculator.
	 *
	 * @param importanceCalculator The importance calculator.
	 * @return The pox facade
	 */
	protected abstract T createPoxFacade(final ImportanceCalculator importanceCalculator);

	/**
	 * Creates a pox facade.
	 *
	 * @return The pox facade
	 */
	protected T createPoxFacade() {
		return this.createPoxFacade(Mockito.mock(ImportanceCalculator.class));
	}

	//region copy

	@Test
	public void copyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final T facade = this.createPoxFacade(importanceCalculator);
		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Act:
		final PoxFacade copyFacade = facade.copy();

		copyFacade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Assert: recalculate was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), Mockito.any());
	}

	@Test
	public void copyCopiesAllFields() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final T facade = this.createPoxFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);
		facade.recalculateImportances(height1, accountStates);

		// Act:
		final PoxFacade copyFacade = facade.copy();

		// Assert:
		Assert.assertThat(copyFacade.getLastPoxVectorSize(), IsEqual.equalTo(3));
		Assert.assertThat(copyFacade.getLastPoxRecalculationHeight(), IsEqual.equalTo(G_HEIGHT_70.prev()));
	}

	//endregion

	//region shallowCopyTo

	@Test
	public void shallowCopyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final T facade = this.createPoxFacade(importanceCalculator);
		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Act:
		final T copyFacade = this.createPoxFacade();
		facade.shallowCopyTo(copyFacade);

		facade.recalculateImportances(new BlockHeight(1234), new ArrayList<>());

		// Assert: recalculate was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), Mockito.any());
	}

	@Test
	public void shallowCopyCopiesAllFields() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final T facade = this.createPoxFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);
		facade.recalculateImportances(height1, accountStates);

		// Act:
		final T copyFacade = this.createPoxFacade();
		facade.shallowCopyTo(copyFacade);

		// Assert:
		Assert.assertThat(copyFacade.getLastPoxVectorSize(), IsEqual.equalTo(3));
		Assert.assertThat(copyFacade.getLastPoxRecalculationHeight(), IsEqual.equalTo(G_HEIGHT_70.prev()));
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

	private static final BlockHeight G_HEIGHT_A1 = new BlockHeight(359 * 10);
	private static final BlockHeight G_HEIGHT_A2 = new BlockHeight(359 * 20);
	private static final BlockHeight G_HEIGHT_20 = new BlockHeight(359 * 20 + 1);
	private static final BlockHeight G_HEIGHT_A3 = new BlockHeight(359 * 30);
	private static final BlockHeight G_HEIGHT_70 = new BlockHeight(359 * 70 + 1);
	private static final BlockHeight G_HEIGHT_70_PLUS = new BlockHeight(359 * 70 + 200);
	private static final List<BlockHeight> G_HEIGHTS_A1_TO_A3 = Arrays.asList(G_HEIGHT_A1, G_HEIGHT_A2, G_HEIGHT_A3);

	@Test
	public void recalculateImportancesDelegatesToImportanceGenerator() {
		// Arrange:
		final BlockHeight height = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
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
		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
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
		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);
		accountStates.add(new AccountState(NetworkInfos.getDefault().getNemesisBlockInfo().getAddress()));

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
		final PoxFacade facade = this.createPoxFacade(importanceCalculator);

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

		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
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

		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height1, accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoxVectorSize(), IsEqual.equalTo(3));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiRecalculationHeight() {
		// Arrange:
		final BlockHeight height1 = G_HEIGHT_70_PLUS;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoxFacade facade = this.createPoxFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height1, accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoxRecalculationHeight(), IsEqual.equalTo(G_HEIGHT_70.prev()));
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
				.map(AccountState::getHeight)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Collection<AccountState>> createAccountStateCollectionArgumentCaptor() {
		return ArgumentCaptor.forClass((Class)Collection.class);
	}

	//endregion
}