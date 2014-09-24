package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.harvesting.UnconfirmedTransactions;
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
		final ValidationResult result = transactions.addValid(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCannotBeAddedIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		transactions.addValid(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.addValid(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();

		// Act:
		transactions.addValid(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		final ValidationResult result = transactions.addValid(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCanBeAddedIfValidationReturnNeutralAndNeutralIsAllowed() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(ValidationResult.NEUTRAL);

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.addValidOrNeutral(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionCannotBeAddedIfValidationReturnNeutralAndNeutralIsNotAllowed() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions(ValidationResult.NEUTRAL);

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = transactions.addValid(transaction);

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
		final ValidationResult result = transactions.addValid(transaction);

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

		transactions.addValid(new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote));

		// Act:
		final Transaction transaction = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferTransaction.Mode.Activate, remote);
		final ValidationResult result = transactions.addValid(transaction);

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
		transactions.addValid(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(7)));
		final Transaction toRemove = createTransferTransaction(txTime, sender, recipient, Amount.fromNem(8));
		transactions.addValid(toRemove);
		transactions.addValid(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(9)));

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
		transactions.addValid(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(7)));
		final Transaction toRemove = createTransferTransaction(txTime, sender, recipient, Amount.fromNem(8)); // never added
		transactions.addValid(createTransferTransaction(txTime, sender, recipient, Amount.fromNem(9)));

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
		transactions.addValid(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7)));
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(8));
		temp.setDeadline(currentTime.addSeconds(-2));
		transactions.addValid(temp);
		transactions.addValid(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(9)));
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(10));
		temp.setDeadline(currentTime.addHours(-3));
		transactions.addValid(temp);
		temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(11));
		temp.setDeadline(currentTime.addSeconds(1));
		transactions.addValid(temp);

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
		Arrays.asList(transaction1, transaction2, transaction3).forEach(unconfirmedTransactions::addValid);

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
		transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
		transactions.addValid(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
		transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
		transactions.addValid(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));

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
		transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
		transactions.addValid(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
		transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
		transactions.addValid(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));

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
		final Account sender = createSenderWithAmount(10);
		final Account recipient = createSenderWithAmount(0);
		// TODO 20140921 J-G: i think the removeConflictingTransactions is confusing in that it takes a list
		// i think  getTransactionsBefore and removeConflictingTransactions should return UnconfirmedTransactions
		// but that's a refactoring for another day ;)
		// (also a smell that i needed to use the "real" validator to get this test to pass, imo)
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
		// 		- initially the balances are: S = 10, R = 0
		// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 4, R = 5, 1 Fee
		// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 6, R = 1, 3 Fee
		//   - getTransactionsBefore() returns SORTED transactions, so R->S is ordered before S->R because it has a greater fee
		//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough balance
		//
		// However, this and test and one I'm gonna add below, should reject R's transaction for the following reason:
		// R doesn't have funds on the account, we don't want such TX because this would lead to creation
		// of a block that would get discarded (TXes are validated first, and then executed)

		final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
		transactions.addValid(first);
		first.setFee(Amount.fromNem(1));
		final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		second.setFee(Amount.fromNem(2));
		transactions.addValid(second);

		transactions.dropExpiredTransactions(currentTime);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
		final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);

		// Assert:
		// note: this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(transactionList, IsEqual.equalTo(Arrays.asList(second, first)));
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(first)));
	}

	@Test
	public void transactionIsRemovedIfAccountDoesntHaveEnoughFunds() {
		// Arrange:
		final Account sender = createSenderWithAmount(10);
		final Account recipient = createSenderWithAmount(0);

		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
		// 		- initially the balances are: S = 10, R = 0
		// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 2, R = 5, 3 Fee
		// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 4, R = 1, 5 Fee
		//   - getTransactionsBefore() returns SORTED transactions, so S->R is ordered before R->S because it has a greater fee
		//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough *confirmed* balance

		final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
		first.setFee(Amount.fromNem(3));
		transactions.addValid(first);
		final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		second.setFee(Amount.fromNem(2));
		transactions.addValid(second);

		transactions.dropExpiredTransactions(currentTime);
		final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
		final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);

		// Assert:
		// - this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(transactionList, IsEqual.equalTo(Arrays.asList(first, second)));
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(first)));
	}

	// TODO 20140923 J-G: so, what benefits do we get checking by checking the unconfirmed balances?
	// TODO 20140924 G-J well, not sure if that's gonna answer your question
	// let's assume S has 10, and he makes two distinct TXes with amount of 7
	// because we execute first TX unconfirmed balance is changed, and second one won't be added
	// to unconfirmed TXes at all.
	//
	// If it WOULD be added, there's a chance, someone could hang whole network (I think we had such
	// bug somewhere in the beginning)
	// Let's say both TXes have been added, now harvester would fail to generate a block (as long as deadline haven't passed)
	// If attacker would send those two TXes to whole network, he'd basically stop whole harvesting
	@Test
	public void checkingUnconfirmedTxesDisallowsAddingDoubleSpendTransactions() {
		// Arrange:
		final Account sender = createSenderWithAmount(10);
		final Account recipient = createSenderWithAmount(0);

		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7));
		transactions.addValid(first);
		final Transaction second = createTransferTransaction(currentTime.addSeconds(-1), sender, recipient, Amount.fromNem(7));
		transactions.addValid(second);

		// Assert:
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(first)));
	}

	private static UnconfirmedTransactions createUnconfirmedTransactionsWithRealValidator() {
		return createUnconfirmedTransactions(NisUtils.createTransactionValidatorFactory().create(Mockito.mock(PoiFacade.class)));
	}

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final List<Transaction> transactions = new ArrayList<>();
		final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactions();
		for (int i = 0; i < 10; ++i) {
			final Transaction transaction = new MockTransaction(i, new TimeInstant(100));
			transactions.add(transaction);
			unconfirmedTransactions.addValid(transaction);
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
			transactions.addValid(new MockTransaction(i, new TimeInstant(i * 10)));
		}

		return transactions;
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions() {
		return createUnconfirmedTransactions(ValidationResult.SUCCESS);
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final TransactionValidator validator) {
		return new UnconfirmedTransactions(validator);
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final ValidationResult result) {
		final TransactionValidator validator = Mockito.mock(TransactionValidator.class);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		return createUnconfirmedTransactions(validator);
	}

	private static UnconfirmedTransactions createUnconfirmedTransactionsWithAscendingFees(final int numTransactions) {
		final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
		for (int i = 0; i < numTransactions; ++i) {
			final MockTransaction mockTransaction = new MockTransaction(i, new TimeInstant(i * 10));
			mockTransaction.setFee(Amount.fromNem(i + 1));
			transactions.addValid(mockTransaction);
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
