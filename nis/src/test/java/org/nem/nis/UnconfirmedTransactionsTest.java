package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class UnconfirmedTransactionsTest {

	//region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Assert:
		Assert.assertThat(createUnconfirmedTransactions(3).size(), IsEqual.equalTo(3));
		Assert.assertThat(createUnconfirmedTransactions(17).size(), IsEqual.equalTo(17));
	}

	//endregion

	//region add

	@Test
	public void transactionCanBeAddedIfTransactionWithSameHashHasNotAlreadyBeenAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions();

		// Act:
		boolean isAdded = transactions.add(new MockTransaction(sender, 7));

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(true));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions();
		transactions.add(new MockTransaction(sender, 7));

		// Act:
		boolean isAdded = transactions.add(new MockTransaction(sender, 7));

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(false));
	}

	@Test
 	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions();

		// Act:
		transactions.add(new MockTransaction(sender, 7));
		boolean isAdded = transactions.add(new MockTransaction(sender, 8));

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(true));
	}

	//endregion

	//region getTransactionsBefore

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeTheSpecifiedTime() {
		// Arrange:
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(10);

		// Act:
		final Collection<Transaction> transactionsBefore = transactions.getTransactionsBefore(new TimeInstant(30));

		// Assert:
		Assert.assertThat(
				getCustomFieldValues(transactionsBefore),
				IsEquivalent.equivalentTo(new Integer[] { 0, 1, 2 }));
	}

	@Test
 	public void getTransactionsBeforeDoesNotRemoveTransactions() {
		// Arrange:
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(10);

		// Act:
		final Collection<Transaction> transactionsBefore1 = transactions.getTransactionsBefore(new TimeInstant(30));
		final Collection<Transaction> transactionsBefore2 = transactions.getTransactionsBefore(new TimeInstant(30));

		// Assert:
		Assert.assertThat(transactionsBefore1.size(), IsEqual.equalTo(3));
		Assert.assertThat(transactionsBefore2.size(), IsEqual.equalTo(3));
	}

	//endregion

	//region removeAll

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final List<Transaction> transactions = new ArrayList<>();
		final UnconfirmedTransactions unconfirmedTransactions = new UnconfirmedTransactions();
		for (int i = 0; i < 10; ++i) {
			final Transaction transaction = new MockTransaction(i, new TimeInstant(100));
			transactions.add(transaction);
			unconfirmedTransactions.add(transaction);
		}

		final Block block = new Block(Utils.generateRandomAccount(), new byte[] { }, new TimeInstant(0), 1);
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(7));
		block.addTransaction(transactions.get(4));

		// Act:
		unconfirmedTransactions.removeAll(block);

		// Assert:
		Assert.assertThat(
				getCustomFieldValues(unconfirmedTransactions.getTransactionsBefore(new TimeInstant(101))),
				IsEquivalent.equivalentTo(new Integer[] { 0, 2, 3, 5, 6, 8, 9 }));
	}

	//endregion

	private static List<Integer> getCustomFieldValues(final Iterable<Transaction> transactions) {
		final List<Integer> customFields = new ArrayList<>();
		for (final Transaction transaction : transactions) {
			customFields.add(((MockTransaction)transaction).getCustomField());
		}

		return customFields;
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(int numTransactions) {
		final UnconfirmedTransactions transactions = new UnconfirmedTransactions();
		for (int i = 0; i < numTransactions; ++i) {
			transactions.add(new MockTransaction(i, new TimeInstant(i * 10)));
		}

		return transactions;
	}
}
