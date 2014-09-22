package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

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
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		transactions.add(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		transactions.add(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		final ValidationResult result = transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCanBeAddedIfTransactionPredicateReturnsFalse() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.add(transaction, hash -> false);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionPredicateReturnsTrue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.add(transaction, hash -> true);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void transactionCannotBeAddedIfValidationFails() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.FAILURE_PAST_DEADLINE);
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(validator);

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.add(transaction, hash -> false);

		// Assert:
		Mockito.verify(validator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void conflictingImportanceTransferTransactionCannotBeAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account remote = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		transactions.add(new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote));

		// Act:
		final Transaction transaction = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferTransaction.Mode.Activate, remote);
		final ValidationResult result = transactions.add(transaction, hash -> false);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}

	//endregion

	//region remove

	@Test
	public void canRemoveKnownTransaction() {
		// Arrange:
		final Account sender = createSenderWithAmount(100);
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		final TimeInstant txTime = new TimeInstant(11);
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(7)));
		final Transaction toRemove = createTransferTransaction(txTime, sender, recipient, Amount.fromNem(8));
		transactions.add(toRemove);
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(9)));

		final boolean isRemoved = transactions.remove(toRemove);
		final List<Amount> amountList = getAmountsBeforeAsList(transactions, txTime.addMinutes(5));

		// Assert:
		Assert.assertThat(isRemoved, IsEqual.equalTo(true));
		Assert.assertThat(amountList, IsEquivalent.equivalentTo(new Amount[] { Amount.fromNem(9), Amount.fromNem(7) }));
	}

	@Test
	public void removeReturnsFalseWhenAttemptingToRemoveUnknownTransaction() {
		// Arrange:
		final Account sender = createSenderWithAmount(100);
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		final TimeInstant txTime = new TimeInstant(11);
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(7)));
		final Transaction toRemove = createTransferTransaction(txTime, sender, recipient, Amount.fromNem(8)); // never added
		transactions.add(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(9)));

		final boolean isRemoved = transactions.remove(toRemove);
		final List<Amount> amountList = getAmountsBeforeAsList(transactions, txTime.addMinutes(5));

		// Assert:
		Assert.assertThat(isRemoved, IsEqual.equalTo(false));
		Assert.assertThat(amountList, IsEquivalent.equivalentTo(new Amount[] { Amount.fromNem(9), Amount.fromNem(7) }));
	}

	@Test
	public void canDropTransactions() {
		// Arrange:
		final Account sender = createSenderWithAmount(100);
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		final TimeInstant currentTime = TimeInstant.ZERO.addHours(24);

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
		final List<Amount> amountList = getAmountsBeforeAsList(transactions, currentTime.addSeconds(1));

		// Assert:
		final Amount[] expectedAmounts = new Amount[] { Amount.fromNem(7), Amount.fromNem(9), Amount.fromNem(11) };
		Assert.assertThat(amountList, IsEquivalent.equivalentTo(expectedAmounts));
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
		for (int i = 1; i < numTransactions; ++i) {
			Assert.assertThat(transactions.get(i - 1).getFee().compareTo(transactions.get(i).getFee()), IsEqual.equalTo(1));
		}
	}

	@Test
	public void getTransactionsBeforeSortsTransactions() {
		// Arrange
		final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactions();
		final MockTransaction transaction1 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(10), 1);
		final MockTransaction transaction2 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(15), 1);
		final MockTransaction transaction3 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(10), 2);
		Arrays.asList(transaction1, transaction2, transaction3).forEach(unconfirmedTransactions::add);

		// Act:
		final Collection<Transaction> transactionsBefore = unconfirmedTransactions.getTransactionsBefore(new TimeInstant(100));
		final ArrayList<Transaction> transactions = new ArrayList<>(transactionsBefore);

		// Assert:
		Assert.assertThat(transactions, IsEqual.equalTo(Arrays.asList(transaction3, transaction1, transaction2)));
	}

	//endregion

	//region getTransactionsBefore | getAll

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
	public void getTransactionsBeforeReturnsTransactionsInSortedOrder() {
		// Arrange:
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		transactions.add(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
		transactions.add(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
		transactions.add(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
		transactions.add(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));

		// Act:
		final Collection<Transaction> transactionsBefore = transactions.getTransactionsBefore(new TimeInstant(10));

		// Assert:
		Assert.assertThat(
				getCustomFieldValues(transactionsBefore),
				IsEqual.equalTo(Arrays.asList(3, 4, 1)));
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
	public void getAllReturnsTransactionsInSortedOrder() {
		// Arrange:
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		transactions.add(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
		transactions.add(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
		transactions.add(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
		transactions.add(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));

		// Act:
		final Collection<Transaction> transactionsBefore = transactions.getAll();

		// Assert:
		Assert.assertThat(
				getCustomFieldValues(transactionsBefore),
				IsEqual.equalTo(Arrays.asList(3, 2, 4, 1)));
	}
	//endregion

	//region removeConflictingTransactions | removeAll

	@Test
	public void filteringOutConflictingTransactions() {
		// Arrange:
		final Account sender = createSenderWithAmount(3);
		final Account recipient = createSenderWithAmount(0);
		// TODO 20140921 J-G: i think the removeConflictingTransactions is confusing in that it takes a list
		// i think  getTransactionsBefore and removeConflictingTransactions should return UnconfirmedTransactions
		// but that's a refactoring for another day ;)
		// (also a smell that i needed to use the "real" validator to get this test to pass, imo)
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(
				new TransactionValidatorFactory().create(Mockito.mock(PoiFacade.class)));
		final TimeInstant currentTime = new TimeInstant(11);

		// Act: the second transaction is invalid because the recipient has an insufficient balance
		// TODO 20140921 J-G: why isn't the balance checked on add?
		// TODO 20140922 G-J: unconfirmed balance is checked, not sure if that answers your question
		// TODO 20140922 J-G: so, i think this is happening (please correct if i'm wrong), but i'm not sure if it's desirable?:
		// UnconfirmedTransactions::add adds each transaction and "executes" them, which means it updates the unconfirmed balances
		// initially the balances are: S = 3, R = 0
		// after "first" transaction is added, the (unconfirmed) balances are: S = 0, R = 2, 1 Fee
		// after the "second" transaction is added, the (unconfirmed) balances are: S = 1, R = 0, 2 Fee
		// so, both transactions get added
		// UnconfirmedTransactions::removeConflictingTransactions re-adds all transactions without "executing" them
		// after "first" transaction is added, the balances are: S = 0, R = 0
		// since R < 2, the second transaction is prevented from inclusion in the block
		final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(2));
		transactions.add(first);
		final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(1));
		second.setFee(Amount.fromNem(1));
		transactions.add(second);

		transactions.dropExpiredTransactions(currentTime);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
		final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);

		// Assert:
		Assert.assertThat(transactionList, IsEquivalent.equivalentTo(new Transaction[] { first, second }));
		Assert.assertThat(filtered, IsEquivalent.equivalentTo(new Transaction[] { first }));
	}

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final List<Transaction> transactions = new ArrayList<>();
		final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactions();
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
	}

	//endregion

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	public static Transaction createTransaction(final TimeInstant timeStamp, final Amount fee, final int customField) {
		final Account sender = createSenderWithAmount(fee.getNumNem());

		final MockTransaction transaction = new MockTransaction(sender, customField, timeStamp);
		transaction.setFee(fee);
		transaction.setDeadline(timeStamp.addSeconds(1));
		return transaction;
	}

	private static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(transaction -> ((MockTransaction)transaction).getCustomField())
				.collect(Collectors.toList());
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final int numTransactions) {
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		for (int i = 0; i < numTransactions; ++i) {
			transactions.add(new MockTransaction(i, new TimeInstant(i * 10)));
		}

		return transactions;
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions() {
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		return createUnconfirmedTransactions(validator);
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final TransactionValidator validator) {
		return new UnconfirmedTransactions(validator);
	}

	private static UnconfirmedTransactions createUnconfirmedTransactionsWithAscendingFees(final int numTransactions) {
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		for (int i = 0; i < numTransactions; ++i) {
			final MockTransaction mockTransaction = new MockTransaction(i, new TimeInstant(i * 10));
			mockTransaction.setFee(Amount.fromNem(i + 1));
			transactions.add(mockTransaction);
		}

		return transactions;
	}

	private static Account createSenderWithAmount(final long nems) {
		final Account sender = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(nems);
		sender.incrementBalance(amount);
		return sender;
	}

	private static List<Amount> getAmountsBeforeAsList(final UnconfirmedTransactions transactions, final TimeInstant instant) {
		return transactions.getTransactionsBefore(instant).stream()
				.map(transaction -> ((TransferTransaction)transaction).getAmount())
				.collect(Collectors.toList());
	}
}
