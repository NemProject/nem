package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.test.*;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsTest {
	public static TransferTransaction createTransferTransaction(final TimeInstant timestamp, final Account sender, final Account recipient, final Amount amount) {
		TransferTransaction transferTransaction = new TransferTransaction(timestamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timestamp.addSeconds(1));
		return transferTransaction;
	}

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
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		boolean isAdded = transactions.add(transaction);

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(true));
		Assert.assertThat(transactions.isSubscribed(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();
		transactions.add(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		boolean isAdded = transactions.add(transaction);

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(false));
		Assert.assertThat(transactions.isSubscribed(transaction), IsEqual.equalTo(false));
	}

	@Test
 	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();

		// Act:
		transactions.add(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		boolean isAdded = transactions.add(transaction);

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(true));
		Assert.assertThat(transactions.isSubscribed(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void transactionCanBeAddedIfTransactionPredicateReturnsFalse() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		boolean isAdded = transactions.add(transaction, hash -> false);

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(true));
		Assert.assertThat(transactions.isSubscribed(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionPredicateReturnsTrue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		boolean isAdded = transactions.add(transaction, hash -> true);

		// Assert:
		Assert.assertThat(isAdded, IsEqual.equalTo(false));
		Assert.assertThat(transactions.isSubscribed(transaction), IsEqual.equalTo(false));
	}

	//endregion

	//region remove
	@Test
	public void canRemoveTransaction() {
		// Arrange:
		final Account sender = createSenderWithAmount(100);
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();

		// Act:
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant txTime = systemTimeProvider.getCurrentTime();
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(7)));
		Transaction toRemove = createTransferTransaction(txTime, sender, recipient, Amount.fromNem(8));
		transactions.add(toRemove);
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(9)));

		boolean result = transactions.remove(toRemove);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(txTime.addMinutes(5));

		// Assert:
		Assert.assertTrue(result);
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));
		Assert.assertThat(((TransferTransaction)transactionList.get(0)).getAmount(), IsEqual.equalTo(Amount.fromNem(9)));
		Assert.assertThat(((TransferTransaction)transactionList.get(1)).getAmount(), IsEqual.equalTo(Amount.fromNem(7)));
		Assert.assertThat(transactions.isSubscribed(toRemove), IsEqual.equalTo(false));
	}

	@Test
	public void canDropTransactions() {
		// Arrange:
		final Account sender = createSenderWithAmount(100);
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();
		final TimeInstant currentTime = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction temp;
		transactions.add(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7)));
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(8));
		temp.setDeadline(currentTime.addSeconds(-2));
		transactions.add(temp);
		transactions.add(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(9)));
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(10));
		temp.setDeadline(currentTime.addHours(-3));
		transactions.add(temp);
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(11));
		temp.setDeadline(currentTime.addSeconds(1));
		transactions.add(temp);


		transactions.dropExpiredTransactions(currentTime);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(3));
		Assert.assertThat(((TransferTransaction )transactionList.get(0)).getAmount(), IsEqual.equalTo(Amount.fromNem(11)));
		Assert.assertThat(((TransferTransaction )transactionList.get(1)).getAmount(), IsEqual.equalTo(Amount.fromNem(9)));
		Assert.assertThat(((TransferTransaction )transactionList.get(2)).getAmount(), IsEqual.equalTo(Amount.fromNem(7)));
	}
	//endregion

	//region sorting

	@Test
	public void returnedTransactionsAreSortedByFee() {
		// Arrange:
		final int numTransactions = 5;
		final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactionsWithAscendingFees(numTransactions);

		// Act:
		final Collection<Transaction> transactionsBefore = unconfirmedTransactions.getTransactionsBefore(new TimeInstant(100));
		final ArrayList<Transaction> transactions = new ArrayList<>(transactionsBefore);

		// Assert:
		for (int i = 1; i < numTransactions; ++i)
			Assert.assertThat(transactions.get(i - 1).getFee().compareTo(transactions.get(i).getFee()), IsEqual.equalTo(1));
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

	@Test
	public void filteringOutConflictingTransactions() {
		// Arrange:
		final Account sender = createSenderWithAmount(3);
		final Account recipient = createSenderWithAmount(0);
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();
		final TimeInstant currentTime = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(2));
		transactions.add(first);
		final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(1));
		second.setFee(Amount.fromNem(1));
		transactions.add(second);

		transactions.dropExpiredTransactions(currentTime);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
		final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);

		// Assert:
		Assert.assertThat(transactionList.size(), IsEqual.equalTo(2));
		Assert.assertThat(transactionList.get(0), IsEqual.equalTo(second));
		Assert.assertThat(transactionList.get(1), IsEqual.equalTo(first));
		Assert.assertThat(filtered.size(), IsEqual.equalTo(1));
		Assert.assertThat(filtered.get(0), IsEqual.equalTo(first));
	}
	//endregion

	//region removeAll

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final List<Transaction> transactions = new ArrayList<>();
		final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactionsInstance();
		for (int i = 0; i < 10; ++i) {
			final Transaction transaction = new MockTransaction(i, new TimeInstant(100));
			transactions.add(transaction);
			unconfirmedTransactions.add(transaction);
		}

		final List<Transaction> blockTransactions = Arrays.asList(
				transactions.get(1),
				transactions.get(7),
				transactions.get(4));
		final Block block = NisUtils.createRandomBlock();
		blockTransactions.stream().forEach(block::addTransaction);

		// Act:
		unconfirmedTransactions.removeAll(block);

		// Assert:
		Assert.assertThat(
				getCustomFieldValues(unconfirmedTransactions.getTransactionsBefore(new TimeInstant(101))),
				IsEquivalent.equivalentTo(new Integer[] { 0, 2, 3, 5, 6, 8, 9 }));
		Assert.assertThat(
				blockTransactions.stream().anyMatch(unconfirmedTransactions::isSubscribed),
				IsEqual.equalTo(false));
	}

	//endregion

	private static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(transaction -> ((MockTransaction)transaction).getCustomField())
				.collect(Collectors.toList());
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(int numTransactions) {
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();
		for (int i = 0; i < numTransactions; ++i) {
			transactions.add(new MockTransaction(i, new TimeInstant(i * 10)));
		}

		return transactions;
	}

	private static UnconfirmedTransactions createUnconfirmedTransactionsInstance() {
		return new UnconfirmedTransactions();
	}


	private static UnconfirmedTransactions createUnconfirmedTransactionsWithAscendingFees(int numTransactions) {
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsInstance();
		for (int i = 0; i < numTransactions; ++i) {
			final MockTransaction mockTransaction = new MockTransaction(i, new TimeInstant(i * 10));
			mockTransaction.setFee(Amount.fromNem(i + 1));
			transactions.add(mockTransaction);
		}

		return transactions;
	}

	private Account createSenderWithAmount(long nems) {
		final Account sender =Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(nems);
		sender.incrementBalance(amount);
		sender.addHistoricalBalance(BlockHeight.ONE, amount);
		return sender;
	}
}
