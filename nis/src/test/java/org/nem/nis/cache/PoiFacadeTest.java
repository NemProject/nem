package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.DebitPredicate;

import java.util.*;
import java.util.stream.*;

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

	@Test
	public void recalculateImportancesDelegatesToImportanceGenerator() {
		// Arrange:
		final BlockHeight height = new BlockHeight(70);
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with three accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesIgnoresAccountsWithGreaterHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(20);
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with two accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L)));
	}

	@Test
	public void recalculateImportancesIgnoresNemesisAccount() {
		// Arrange:
		final BlockHeight height = new BlockHeight(70);
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);
		accountStates.add(new AccountState(NemesisBlock.ADDRESS));

		// Act:
		facade.recalculateImportances(height, accountStates);

		// Assert: the generator was called once and passed a collection with three accounts (but not the nemesis account)
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesDoesNotRecalculateImportancesForLastBlockHeight() {
		// Arrange:
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		final PoiFacade facade = this.createPoiFacade(importanceCalculator);

		// Act:
		facade.recalculateImportances(new BlockHeight(7), new ArrayList<>());
		facade.recalculateImportances(new BlockHeight(7), new ArrayList<>());

		// Assert: the generator was only called once
		Mockito.verify(importanceCalculator, Mockito.times(1)).recalculate(Mockito.any(), Mockito.any());
	}

	@Test
	public void recalculateImportancesRecalculatesImportancesForNewBlockHeight() {
		// Arrange:
		final int height1 = 70;
		final int height2 = 80;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1), accountStates);
		facade.recalculateImportances(new BlockHeight(height2), accountStates);

		// Assert: the generator was called twice and passed a collection with three accounts
		final ArgumentCaptor<Collection<AccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceCalculator, Mockito.times(2)).recalculate(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiVectorSize() {
		// Arrange:
		final int height1 = 70;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1), accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoiVectorSize(), IsEqual.equalTo(3));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiRecalculationHeight() {
		// Arrange:
		final int height1 = 70;
		final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);

		final PoiFacade facade = this.createPoiFacade(importanceCalculator);
		final List<AccountState> accountStates = createAccountStatesForRecalculateTests(3);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1), accountStates);

		// Assert:
		Assert.assertThat(facade.getLastPoiRecalculationHeight(), IsEqual.equalTo(new BlockHeight(70)));
	}

	private static List<AccountState> createAccountStatesForRecalculateTests(final int numAccounts) {
		final List<AccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < numAccounts; ++i) {
			accountStates.add(new AccountState(Utils.generateRandomAddress()));
			accountStates.get(i).setHeight(new BlockHeight((i + 1) * 10));
		}

		return accountStates;
	}

	private List<Long> heightsAsList(final Collection<AccountState> accountStates) {
		return accountStates.stream()
				.map(as -> as.getHeight().getRaw())
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Collection<AccountState>> createAccountStateCollectionArgumentCaptor() {
		return ArgumentCaptor.forClass((Class)Collection.class);
	}

	//endregion
}