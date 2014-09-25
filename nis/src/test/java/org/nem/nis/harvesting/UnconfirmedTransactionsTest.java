package org.nem.nis.harvesting;

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
	public void addSucceedsIfTransactionWithSameHashHasNotAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		context.transactions.add(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		context.transactions.add(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		final ValidationResult result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
	}

	@Test
	public void addSucceedsIfValidationReturnsNeutralAndNeutralIsAllowed() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.NEUTRAL);
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction, UnconfirmedTransactions.AddOptions.AllowNeutral);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfValidationReturnNeutralAndNeutralIsNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.NEUTRAL);
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction, UnconfirmedTransactions.AddOptions.RejectNeutral);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addFailsIfValidationReturnNeutralAndNeutralIsImplicitlyNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.NEUTRAL);
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addFailsIfTransactionValidationFails() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
		Mockito.verify(context.validator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
	}

	@Test
	public void addFailsIfTransactionConflictsWithExistingImportanceTransferTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account remote = Utils.generateRandomAccount();

		final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferTransaction.Mode.Activate, remote);
		context.transactions.add(t1);

		// Act:
		final ValidationResult result = context.transactions.add(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	// TODO-CR 20140924 J-J: need to review why this test is failing
	@Ignore
	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));

		final Transaction t1 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		final Transaction t2 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		context.transactions.add(t1);

		// Act:
		final ValidationResult result = context.transactions.add(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addDelegatesToTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.add(transaction);

		// Assert:
		Mockito.verify(context.validator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
	}

	@Test
	public void addSuccessExecutesTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = Mockito.spy(new MockTransaction(sender, 7));
		context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Mockito.verify(transaction, Mockito.times(1)).execute(Mockito.any());
	}

	@Test
	public void addFailureDoesNotExecuteTransaction() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void transactionCanBeAddedIfValidationSucceedsAfterValidationFails() {
		// Arrange:
		final TestContext context = new TestContext(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount();

		final MockTransaction transaction = new MockTransaction(sender, 7);
		ValidationResult result = context.transactions.add(transaction);
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));

		Mockito.when(context.validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		result = context.transactions.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	//endregion

	//region remove

	@Test
	public void canRemoveKnownTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = createSenderWithAmount(100);

		context.transactions.add(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8);
		context.transactions.add(toRemove);
		context.transactions.add(new MockTransaction(sender, 9));

		// Act:
		final boolean isRemoved = context.transactions.remove(toRemove);
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(isRemoved, IsEqual.equalTo(true));
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(9, 7)));
	}

	@Test
	public void removeReturnsFalseWhenAttemptingToRemoveUnknownTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = createSenderWithAmount(100);

		// Act:
		context.transactions.add(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8); // never added
		context.transactions.add(new MockTransaction(sender, 9));

		final boolean isRemoved = context.transactions.remove(toRemove);
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(isRemoved, IsEqual.equalTo(false));
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(9, 7)));
	}

	@Test
	public void removeSuccessUndoesTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		// (for some reason passing the spied transaction to both remove and add does not work)
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final Transaction spiedTransaction = Mockito.spy(transaction);
		context.transactions.add(transaction);
		context.transactions.remove(spiedTransaction);

		// Assert:
		Mockito.verify(spiedTransaction, Mockito.times(1)).undo(Mockito.any());
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void removeFailureDoesNotUndoTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.remove(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
	}

	//endregion

	//region removeAll

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = addMockTransactions(context.transactions, 6, 9);

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(3));

		// Act:
		context.transactions.removeAll(block);
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 8)));
	}

	@Test
	public void removeAllDoesNotUndoTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = addMockTransactions(context.transactions, 6, 9);

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(3));

		// Act:
		context.transactions.removeAll(block);

		// Assert:
		for (final MockTransaction transaction : transactions) {
			// not the greatest test, but the count is 1 for all because it is incremented by execute
			Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		}
	}

	//endregion

	//region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getAllReturnsAllTransactionsInSortedOrder() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = createMockTransactions(6, 9);
		transactions.get(2).setFee(Amount.fromNem(11));
		transactions.forEach(context.transactions::add);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
	}

	//endregion

	//region getTransactionsBefore

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = new TestContext();
		addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = createMockTransactions(6, 9);
		transactions.get(1).setFee(Amount.fromNem(11));
		transactions.forEach(context.transactions::add);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(7, 6)));
	}

	//endregion

	//region dropExpiredTransactions

	@Test
	public void dropExpiredTransactionsRemovesAllTransactionsBeforeSpecifiedTimeInstant() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context.transactions::add);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(7, 9)));
	}

	@Test
	public void dropExpiredTransactionsUndoesRemovedTransactions() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context.transactions::add);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(1));
	}

	//endregion

	private static List<MockTransaction> createMockTransactions(final int startCustomField, final int endCustomField) {
		final List<MockTransaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			final MockTransaction transaction = new MockTransaction(i, new TimeInstant(i));
			transaction.setFee(Amount.fromNem(i));
			transactions.add(transaction);
		}

		return transactions;
	}

	private static List<MockTransaction> addMockTransactions(
			final UnconfirmedTransactions unconfirmedTransactions,
			final int startCustomField,
			final int endCustomField) {
		final List<MockTransaction> transactions = createMockTransactions(startCustomField, endCustomField);
		transactions.forEach(unconfirmedTransactions::add);
		return transactions;
	}

	private class TestContext {
		private final TransactionValidator validator;
		private final UnconfirmedTransactions transactions;

		private TestContext() {
			this(ValidationResult.SUCCESS);
		}

		private TestContext(final ValidationResult result) {
			this(Mockito.mock(TransactionValidator.class));
			Mockito.when(this.validator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		}


		private TestContext(final TransactionValidator validator) {
			this.validator = validator;
			this.transactions =  new UnconfirmedTransactions(this.validator);
		}
	}

	//@Test
	//public void canDropTransactions() {
	//	// Arrange:
	//	final Account sender = createSenderWithAmount(100);
	//	final Account recipient = Utils.generateRandomAccount();
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
	//	final TimeInstant currentTime = TimeInstant.ZERO.addHours(24);
	//
	//	// Act:
	//	Transaction temp;
	//	transactions.addValid(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7)));
	//	temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(8));
	//	temp.setDeadline(currentTime.addSeconds(-2));
	//	transactions.addValid(temp);
	//	transactions.addValid(createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(9)));
	//	temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(10));
	//	temp.setDeadline(currentTime.addHours(-3));
	//	transactions.addValid(temp);
	//	temp = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(11));
	//	temp.setDeadline(currentTime.addSeconds(1));
	//	transactions.addValid(temp);
	//
	//	transactions.dropExpiredTransactions(currentTime);
	//	final List<Amount> amountList = getAmountsBeforeAsList(transactions, currentTime.addSeconds(1));
	//
	//	// Assert:
	//	final Amount[] expectedAmounts = new Amount[] { Amount.fromNem(7), Amount.fromNem(9), Amount.fromNem(11) };
	//	Assert.assertThat(amountList, IsEquivalent.equivalentTo(expectedAmounts));
	//}
	////endregion
	//
	////region sorting
	//
	//@Test
	//public void returnedTransactionsAreSortedByFee() {
	//	// Arrange:
	//	final int numTransactions = 5;
	//	final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactionsWithAscendingFees(numTransactions);
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore = unconfirmedTransactions.getTransactionsBefore(new TimeInstant(100));
	//	final ArrayList<Transaction> transactions = new ArrayList<>(transactionsBefore);
	//
	//	// Assert:
	//	for (int i = 1; i < numTransactions; ++i) {
	//		Assert.assertThat(transactions.get(i - 1).getFee().compareTo(transactions.get(i).getFee()), IsEqual.equalTo(1));
	//	}
	//}
	//
	//@Test
	//public void getTransactionsBeforeSortsTransactions() {
	//	// Arrange
	//	final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactions();
	//	final MockTransaction transaction1 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(10), 1);
	//	final MockTransaction transaction2 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(15), 1);
	//	final MockTransaction transaction3 = new MockTransaction(MockTransaction.TYPE, 123, new TimeInstant(10), 2);
	//	Arrays.asList(transaction1, transaction2, transaction3).forEach(unconfirmedTransactions::addValid);
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore = unconfirmedTransactions.getTransactionsBefore(new TimeInstant(100));
	//	final ArrayList<Transaction> transactions = new ArrayList<>(transactionsBefore);
	//
	//	// Assert:
	//	Assert.assertThat(transactions, IsEqual.equalTo(Arrays.asList(transaction3, transaction1, transaction2)));
	//}
	//
	////endregion
	//
	////region getTransactionsBefore | getAll
	//
	//@Test
	//public void getTransactionsBeforeReturnsAllTransactionsBeforeTheSpecifiedTime() {
	//	// Arrange:
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactions(10);
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore = transactions.getTransactionsBefore(new TimeInstant(30));
	//
	//	// Assert:
	//	Assert.assertThat(
	//			getCustomFieldValues(transactionsBefore),
	//			IsEquivalent.equivalentTo(new Integer[] { 0, 1, 2 }));
	//}
	//
	//@Test
	//public void getTransactionsBeforeReturnsTransactionsInSortedOrder() {
	//	// Arrange:
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
	//	transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
	//	transactions.addValid(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
	//	transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
	//	transactions.addValid(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore = transactions.getTransactionsBefore(new TimeInstant(10));
	//
	//	// Assert:
	//	Assert.assertThat(
	//			getCustomFieldValues(transactionsBefore),
	//			IsEqual.equalTo(Arrays.asList(3, 4, 1)));
	//}
	//
	//@Test
	//public void getTransactionsBeforeDoesNotRemoveTransactions() {
	//	// Arrange:
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactions(10);
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore1 = transactions.getTransactionsBefore(new TimeInstant(30));
	//	final Collection<Transaction> transactionsBefore2 = transactions.getTransactionsBefore(new TimeInstant(30));
	//
	//	// Assert:
	//	Assert.assertThat(transactionsBefore1.size(), IsEqual.equalTo(3));
	//	Assert.assertThat(transactionsBefore2.size(), IsEqual.equalTo(3));
	//}
	//
	//@Test
	//public void getAllReturnsTransactionsInSortedOrder() {
	//	// Arrange:
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactions();
	//	transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(1), 1));
	//	transactions.addValid(createTransaction(new TimeInstant(11), Amount.fromNem(2), 2));
	//	transactions.addValid(createTransaction(new TimeInstant(9), Amount.fromNem(4), 3));
	//	transactions.addValid(createTransaction(new TimeInstant(7), Amount.fromNem(1), 4));
	//
	//	// Act:
	//	final Collection<Transaction> transactionsBefore = transactions.getAll();
	//
	//	// Assert:
	//	Assert.assertThat(
	//			getCustomFieldValues(transactionsBefore),
	//			IsEqual.equalTo(Arrays.asList(3, 2, 4, 1)));
	//}
	////endregion
	//
	////region removeConflictingTransactions | removeAll
	//
	//@Test
	//public void filteringOutConflictingTransactions() {
	//	// Arrange:
	//	final Account sender = createSenderWithAmount(10);
	//	final Account recipient = createSenderWithAmount(0);
	//	// TODO 20140921 J-G: i think the removeConflictingTransactions is confusing in that it takes a list
	//	// i think  getTransactionsBefore and removeConflictingTransactions should return UnconfirmedTransactions
	//	// but that's a refactoring for another day ;)
	//	// (also a smell that i needed to use the "real" validator to get this test to pass, imo)
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
	//	final TimeInstant currentTime = new TimeInstant(11);
	//
	//	// Act:
	//	//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
	//	// 		- initially the balances are: S = 10, R = 0
	//	// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 4, R = 5, 1 Fee
	//	// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 6, R = 1, 3 Fee
	//	//   - getTransactionsBefore() returns SORTED transactions, so R->S is ordered before S->R because it has a greater fee
	//	//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough balance
	//	//
	//	// However, this and test and one I'm gonna add below, should reject R's transaction for the following reason:
	//	// R doesn't have funds on the account, we don't want such TX because this would lead to creation
	//	// of a block that would get discarded (TXes are validated first, and then executed)
	//
	//	final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
	//	transactions.addValid(first);
	//	first.setFee(Amount.fromNem(1));
	//	final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
	//	second.setFee(Amount.fromNem(2));
	//	transactions.addValid(second);
	//
	//	transactions.dropExpiredTransactions(currentTime);
	//	final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
	//	final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);
	//
	//	// Assert:
	//	// note: this checks that both TXes have been added and that returned TXes are in proper order
	//	// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
	//	Assert.assertThat(transactionList, IsEqual.equalTo(Arrays.asList(second, first)));
	//	Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(first)));
	//}
	//
	//@Test
	//public void transactionIsRemovedIfAccountDoesntHaveEnoughFunds() {
	//	// Arrange:
	//	final Account sender = createSenderWithAmount(10);
	//	final Account recipient = createSenderWithAmount(0);
	//
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
	//	final TimeInstant currentTime = new TimeInstant(11);
	//
	//	// Act:
	//	//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
	//	// 		- initially the balances are: S = 10, R = 0
	//	// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 2, R = 5, 3 Fee
	//	// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 4, R = 1, 5 Fee
	//	//   - getTransactionsBefore() returns SORTED transactions, so S->R is ordered before R->S because it has a greater fee
	//	//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough *confirmed* balance
	//
	//	final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
	//	first.setFee(Amount.fromNem(3));
	//	transactions.addValid(first);
	//	final Transaction second = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
	//	second.setFee(Amount.fromNem(2));
	//	transactions.addValid(second);
	//
	//	transactions.dropExpiredTransactions(currentTime);
	//	final List<Transaction> transactionList = transactions.getTransactionsBefore(currentTime.addSeconds(1));
	//	final List<Transaction> filtered = transactions.removeConflictingTransactions(transactionList);
	//
	//	// Assert:
	//	// - this checks that both TXes have been added and that returned TXes are in proper order
	//	// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
	//	Assert.assertThat(transactionList, IsEqual.equalTo(Arrays.asList(first, second)));
	//	Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(first)));
	//}
	//
	//// TODO 20140923 J-G: so, what benefits do we get checking by checking the unconfirmed balances?
	//// TODO 20140924 G-J well, not sure if that's gonna answer your question
	//// let's assume S has 10, and he makes two distinct TXes with amount of 7
	//// because we execute first TX unconfirmed balance is changed, and second one won't be added
	//// to unconfirmed TXes at all.
	////
	//// If it WOULD be added, there's a chance, someone could hang whole network (I think we had such
	//// bug somewhere in the beginning)
	//// Let's say both TXes have been added, now harvester would fail to generate a block (as long as deadline haven't passed)
	//// If attacker would send those two TXes to whole network, he'd basically stop whole harvesting
	//@Test
	//public void checkingUnconfirmedTxesDisallowsAddingDoubleSpendTransactions() {
	//	// Arrange:
	//	final Account sender = createSenderWithAmount(10);
	//	final Account recipient = createSenderWithAmount(0);
	//
	//	final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
	//	final TimeInstant currentTime = new TimeInstant(11);
	//
	//	// Act:
	//	final Transaction first = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7));
	//	transactions.addValid(first);
	//	final Transaction second = createTransferTransaction(currentTime.addSeconds(-1), sender, recipient, Amount.fromNem(7));
	//	transactions.addValid(second);
	//
	//	// Assert:
	//	Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(first)));
	//}
	//
	//private static UnconfirmedTransactions createUnconfirmedTransactionsWithRealValidator() {
	//	return createUnconfirmedTransactions(NisUtils.createTransactionValidatorFactory().create(Mockito.mock(PoiFacade.class)));
	//}
	//
	//@Test
	//public void removeAllRemovesAllTransactionsInBlock() {
	//	// Arrange:
	//	final List<Transaction> transactions = new ArrayList<>();
	//	final UnconfirmedTransactions unconfirmedTransactions = createUnconfirmedTransactions();
	//	for (int i = 0; i < 10; ++i) {
	//		final Transaction transaction = new MockTransaction(i, new TimeInstant(100));
	//		transactions.add(transaction);
	//		unconfirmedTransactions.addValid(transaction);
	//	}
	//
	//	final List<Transaction> blockTransactions = Arrays.asList(
	//			transactions.get(1),
	//			transactions.get(7),
	//			transactions.get(4));
	//	final Block block = NisUtils.createRandomBlock();
	//	blockTransactions.stream().forEach(block::addTransaction);
	//
	//	// Act:
	//	unconfirmedTransactions.removeAll(block);
	//
	//	// Assert:
	//	Assert.assertThat(
	//			getCustomFieldValues(unconfirmedTransactions.getTransactionsBefore(new TimeInstant(101))),
	//			IsEquivalent.equivalentTo(new Integer[] { 0, 2, 3, 5, 6, 8, 9 }));
	//}
	//
	////endregion

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

	private static List<Amount> getAmountsAsList(final UnconfirmedTransactions transactions) {
		return transactions.getAll().stream()
				.map(transaction -> ((TransferTransaction)transaction).getAmount())
				.collect(Collectors.toList());
	}

	private static List<Amount> getAmountsBeforeAsList(final UnconfirmedTransactions transactions, final TimeInstant instant) {
		return transactions.getTransactionsBefore(instant).stream()
				.map(transaction -> ((TransferTransaction)transaction).getAmount())
				.collect(Collectors.toList());
	}
}
