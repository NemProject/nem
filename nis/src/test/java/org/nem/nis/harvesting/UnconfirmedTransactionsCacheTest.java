package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsCacheTest {

	//region construction

	@Test
	public void cacheIsInitiallyEmpty() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
	}

	//endregion

	//region add

	@Test
	public void canAddNewTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final boolean result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void canAddNewTransactionWithNewChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));

		// Act:
		final boolean result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(3));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction2), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction);

		// Act:
		final boolean result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingTransactionAsChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction);

		final MockTransaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		transaction2.setChildTransactions(Arrays.asList(transaction));

		// Act:
		final boolean result = cache.add(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction));
		cache.add(transaction);

		final MockTransaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		transaction2.setChildTransactions(Arrays.asList(innerTransaction));

		// Act:
		final boolean result = cache.add(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingChildTransactionAsTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction));
		cache.add(transaction);

		// Act:
		final boolean result = cache.add(innerTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	//endregion

	//region remove

	@Test
	public void canRemoveExistingTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction0 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction0);

		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction);

		// Act:
		final boolean result = cache.remove(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction0), IsEqual.equalTo(true));
	}

	@Test
	public void canRemoveExistingTransactionWithNewChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction0 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction0);

		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction);

		// Act:
		final boolean result = cache.remove(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction0), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNewTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction0 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction0);

		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final boolean result = cache.remove(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction0), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNewTransactionWithChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction0 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction0);

		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));

		// Act:
		final boolean result = cache.remove(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction0), IsEqual.equalTo(true));
	}

	// TODO 20140122 J-G: i'm not sure what this test should do?
	@Test
	public void canRemoveTransactionFirstSeenAsChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction);

		// Act:
		final boolean result = cache.remove(innerTransaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	//endregion

	//region clear

	@Test
	public void clearRemovesAllTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(new MockTransaction(Utils.generateRandomAccount()));
		cache.add(new MockTransaction(Utils.generateRandomAccount()));
		cache.add(new MockTransaction(Utils.generateRandomAccount()));

		// Act:
		cache.clear();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
	}

	@Test
	public void clearRemovesAllChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction());
		cache.add(createTransactionWithTwoChildTransaction());
		cache.add(createTransactionWithTwoChildTransaction());

		// Act:
		cache.clear();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
	}

	//endregion

	//region contains

	@Test
	public void containsReturnsTrueForRootTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);
		cache.add(transaction2);

		// Assert:
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(true));

	}

	@Test
	public void containsReturnsTrueForChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction);

		// Assert:
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction2), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseForOtherTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);

		// Assert:
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(false));
	}

	//endregion

	//region stream / flatStream

	@Test
	public void streamReturnsAllRootTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction(10));
		cache.add(createTransactionWithTwoChildTransaction(20));
		cache.add(createTransactionWithTwoChildTransaction(30));

		// Assert:
		Assert.assertThat(
				cache.stream().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(10, 20, 30));
	}

	@Test
	public void flatStreamReturnsAllRootTransactionsAndChildren() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction(10));
		cache.add(createTransactionWithTwoChildTransaction(20));
		cache.add(createTransactionWithTwoChildTransaction(30));

		// Assert:
		Assert.assertThat(
				cache.streamFlat().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(10, 11, 12, 20, 21, 22, 30, 31, 32));
	}

	//endregion

	private static Transaction createTransactionWithTwoChildTransaction() {
		return createTransactionWithTwoChildTransaction(0);
	}

	private static Transaction createTransactionWithTwoChildTransaction(final int seed) {
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount(), seed + 1);
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount(), seed + 2);
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), seed);
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		return transaction;
	}
}