package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;

	//region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.addAccount(Amount.fromNem(100));

		// Act:
		for (int i = 0; i < 17; ++i) {
			context.signAndAddExisting(new MockTransaction(account, i));
		}

		// Assert:
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(17));
	}

	//endregion

	//region getUnconfirmedBalance

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceWhenNoPendingTransactionsImpactAccount() {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(5)));
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(100)));
	}

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingTransferTransactionsImpactingAccount() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final Account account1 = context.addAccount(Amount.fromNem(4));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final TimeInstant currentTime = new TimeInstant(11);
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(currentTime, account2, account1, Amount.fromNem(5), null),
				new TransferTransaction(currentTime, account1, account2, Amount.fromNem(4), null));
		setFeeAndDeadline(transactions.get(0), Amount.fromNem(1));
		setFeeAndDeadline(transactions.get(1), Amount.fromNem(2));
		transactions.forEach(context::signAndAddExisting);

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(3)));
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(98)));
	}

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingImportanceTransactionsImpactingAccount() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final Account sender = context.addAccount(Amount.fromNem(500000));
		final Account remote = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);
		final Transaction t1 = new ImportanceTransferTransaction(currentTime, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		setFeeAndDeadline(t1, Amount.fromNem(10));
		context.signAndAddExisting(t1);

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(sender), IsEqual.equalTo(Amount.fromNem(499990)));
	}

	private static void setFeeAndDeadline(final Transaction transaction, final Amount fee) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
		transaction.setFee(fee);
	}

	//endregion

	//region add[Batch/New/Existing]

	@Test
	public void addNewBatchReturnsSuccessIfAllTransactionsCanBeSuccessfullyAdded() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.signAndAddNewBatch(context.createMockTransactionsAsBatch(1, 2));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
	}

	@Test
	public void addNewBatchShortCircuitsAndReturnsFailureIfAnyTransactionFailsSingleValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_FUTURE_DEADLINE,
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_ENTITY_UNUSABLE);

		// Act:
		final ValidationResult result = context.signAndAddNewBatch(context.createMockTransactionsAsBatch(1, 4));

		// Assert: only first transaction was added and validation stopped after the first failure
		Mockito.verify(context.singleValidator, Mockito.times(2)).validate(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addNewBatchReturnsFailureIfBatchValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(15));
		final ValidationResult result = context.signAndAddNewBatch(Arrays.asList(transaction));

		// Assert:
		Assert.assertThat(result.isFailure(), IsEqual.equalTo(true));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addSucceedsIfTransactionWithSameHashHasNotAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));
		context.signAndAddExisting(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		context.signAndAddExisting(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		final ValidationResult result = context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
	}

	//region validation

	@Test
	public void addExistingSucceedsIfBatchValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addNewFailsIfBatchValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.signAndAddNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_HASH_EXISTS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addNewFailsIfValidationReturnsNeutral() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.NEUTRAL);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(30));
		final ValidationResult result = context.signAndAddNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addFailsIfTransactionValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
		Mockito.verify(context.singleValidator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
	}

	@Test
	public void addFailsIfTransactionConflictsWithExistingImportanceTransferTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(10));
		final Account remote = context.addAccount(Amount.fromNem(100));

		final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferTransaction.Mode.Activate, remote);
		context.signAndAddExisting(t1);

		// Act:
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext(new UniversalTransactionValidator());
		final Account sender = context.addAccount(Amount.fromNem(10));

		final Transaction t1 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		context.signAndAddExisting(t1);

		// Act:
		final Transaction t2 = new MockTransaction(sender);
		t2.setFee(Amount.fromNem(5));
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfTransactionHasExpired() {
		// Arrange:
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(1122450));
		final TestContext context = new TestContext(new TransactionDeadlineValidator(timeProvider));
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(timeProvider.getCurrentTime().addSeconds(-1));

		// Act:
		final ValidationResult result = context.signAndAddNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
	}

	@Test
	public void addNewFailsIfTransactionDoesNotVerify() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		final Account sender = context.addAccount(Amount.fromNem(10));
		final MockTransaction transaction = new MockTransaction(sender);
		transaction.sign();

		// Act (ruin signature by altering the deadline):
		transaction.setDeadline(transaction.getDeadline().addMinutes(1));
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
	}

	@Test
	public void addExistingFailsIfTransactionDoesNotVerify() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		final Account sender = context.addAccount(Amount.fromNem(10));
		final MockTransaction transaction = new MockTransaction(sender);
		transaction.sign();

		// Act (ruin signature by altering the deadline):
		transaction.setDeadline(transaction.getDeadline().addMinutes(1));
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
	}

	@Test
	public void addExistingDelegatesToSingleTransactionValidatorButNotBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddExisting(transaction);

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		Mockito.verify(context.batchValidator, Mockito.never()).validate(Mockito.any());
	}

	@Test
	public void addNewDelegatesToSingleTransactionValidatorAndBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddNew(transaction);

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		assertBatchValidation(context.batchValidator, transaction);
	}

	@Test
	public void addNewBatchDelegatesToSingleTransactionValidatorAndBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddNewBatch(Arrays.asList(transaction));

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		assertBatchValidation(context.batchValidator, transaction);
	}

	private static void assertBatchValidation(final BatchTransactionValidator validator, final Transaction transaction) {
		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(validator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		Assert.assertThat(pair.getTransactions(), IsEquivalent.equivalentTo(Arrays.asList(transaction)));
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<List<TransactionsContextPair>> createPairsCaptor() {
		return ArgumentCaptor.forClass((Class)List.class);
	}

	//endregion

	//region execution

	@Test
	public void addSuccessExecutesTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = Mockito.spy(new MockTransaction(sender, 7));
		context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Mockito.verify(transaction, Mockito.times(1)).execute(Mockito.any());
	}

	@Test
	public void addFailureDoesNotExecuteTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddExisting(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
	}

	//endregion

	@Test
	public void transactionCanBeAddedIfValidationSucceedsAfterValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = context.addAccount(Amount.fromNem(100));

		final MockTransaction transaction = new MockTransaction(sender, 7);
		ValidationResult result = context.signAndAddExisting(transaction);
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));

		Mockito.when(context.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		result = context.signAndAddExisting(transaction);

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
		final Account sender = context.addAccount(Amount.fromNem(100));

		context.signAndAddExisting(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8);
		context.signAndAddExisting(toRemove);
		context.signAndAddExisting(new MockTransaction(sender, 9));

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
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		context.signAndAddExisting(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8); // never added
		context.signAndAddExisting(new MockTransaction(sender, 9));

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
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		// (for some reason passing the spied transaction to both remove and add does not work)
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final Transaction spiedTransaction = Mockito.spy(transaction);
		context.signAndAddExisting(transaction);
		context.transactions.remove(spiedTransaction);

		// Assert:
		Mockito.verify(spiedTransaction, Mockito.times(1)).undo(Mockito.any());
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void removeFailureDoesNotUndoTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

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
		final List<MockTransaction> transactions = context.addMockTransactions(context.transactions, 6, 9);

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
	public void removeAllDoesUndoTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.addMockTransactions(context.transactions, 6, 9);

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(3));

		// Act:
		context.transactions.removeAll(block);

		// Assert:
		// not the greatest test, but the count is 2 for the removed transactions and 1 for the others
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(2));
	}

	//endregion

	//region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getAllReturnsAllTransactionsInSortedOrder() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(2).setFee(Amount.fromNem(11));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
	}

	//endregion

	// region getUnknownTransactions

	@Test
	public void getUnknownTransactionsReturnsAllTransactionsIfHashShortIdListIsEmpty() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 3);
		context.signAndAddNewBatch(transactions);

		// Act:
		final List<Transaction> unknownTransactions = context.transactions.getUnknownTransactions(new ArrayList<>());

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(transactions));
	}

	@Test
	public void getUnknownTransactionsFiltersKnownTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 6);
		context.signAndAddNewBatch(transactions);
		final List<HashShortId> hashShortIds = new ArrayList<>();
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(1)).getShortId()));
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(2)).getShortId()));
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(4)).getShortId()));

		// Act:
		final List<Transaction> unknownTransactions = context.transactions.getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(
				unknownTransactions,
				IsEquivalent.equivalentTo(Arrays.asList(transactions.get(0), transactions.get(3), transactions.get(5))));
	}

	@Test
	public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 6);
		context.signAndAddNewBatch(transactions);
		final List<HashShortId> hashShortIds = transactions.stream()
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());

		// Act:
		final List<Transaction> unknownTransactions = context.transactions.getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
	}

	// endregion

	//region getMostRecentTransactionsForAccount

	@Test
	public void getMostRecentTransactionsReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
		context.signAndAddNewBatch(transactions);

		// Act:
		final List<Transaction> mostRecentTransactions = context.transactions.getMostRecentTransactionsForAccount(account.getAddress(), 20);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 20);
		context.signAndAddNewBatch(transactions);

		// Act:
		final List<Transaction> mostRecentTransactions = context.transactions.getMostRecentTransactionsForAccount(account.getAddress(), 10);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
		context.signAndAddNewBatch(transactions);

		// Act:
		final List<Transaction> mostRecentTransactions = context.transactions.getMostRecentTransactionsForAccount(account.getAddress(), 10);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsTransactionsSortedByTimeInDescendingOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
		context.signAndAddNewBatch(transactions);

		// Act:
		final List<Transaction> mostRecentTransactions = context.transactions.getMostRecentTransactionsForAccount(account.getAddress(), 25);

		// Assert:
		TimeInstant curTimeStamp = new TimeInstant(Integer.MAX_VALUE);
		for (final Transaction tx : mostRecentTransactions) {
			Assert.assertThat(tx.getTimeStamp().compareTo(curTimeStamp) <= 0, IsEqual.equalTo(true));
			curTimeStamp = tx.getTimeStamp();
		}
	}

	//endregion

	//region getTransactions

	@Test
	public void getMostImportantTransactionsReturnsAllTransactionsIfLessThanMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
		final List<Integer> customFieldValues = getCustomFieldValues(transactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsIfMoreThanMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsIfMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 6 + MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> originalTransactions = context.createMockTransactions(6, 9);
		originalTransactions.get(2).setFee(Amount.fromNem(11));
		originalTransactions.forEach(context::signAndAddExisting);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
		final List<Integer> customFieldValues = getCustomFieldValues(transactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
	}

	//endregion

	//region getTransactionsBefore

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {

		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(1).setFee(Amount.fromNem(11));
		transactions.forEach(context::signAndAddExisting);

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
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(7, 9)));
	}

	@Test
	public void dropExpiredTransactionsExecutesAllNonExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(2));
	}

	//endregion

	//region getTransactionsForAccount

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAccountAsSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account1, 1),
				new MockTransaction(account2, 2),
				new MockTransaction(account1, 3),
				new MockTransaction(account2, 4));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account1.getAddress());
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 3)));
	}

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAccountAsTransferRecipient() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final Account account3 = context.addAccount(Amount.fromNem(100));
		final List<TransferTransaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account1, account2, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(2), account2, account1, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(3), account1, account3, Amount.ZERO, null));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account3.getAddress());
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(3))));
	}

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAffectingAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final Account account3 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account1, account2, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(2), account2, account1, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(3), account1, account3, Amount.ZERO, null),
				new MockTransaction(account2, 1, new TimeInstant(4)));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account2.getAddress());
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1), new TimeInstant(2), new TimeInstant(4))));
	}

	@Test
	public void getTransactionsForAccountIncludesConflictingTransactions() {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account2.getAddress());
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1), new TimeInstant(2))));
	}

	//endregion

	//region getTransactionsForNewBlock

	@Test
	public void getTransactionsForNewBlockIncludesTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account2, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(account1.getAddress(), new TimeInstant(6));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2)));
	}

	@Test
	public void getTransactionsForNewBlockExcludesTransactionsSignedByHarvesterAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account1, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account1, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(account1.getAddress(), new TimeInstant(10));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(2, 4)));
	}

	@Test
	public void getTransactionsForNewBlockExcludesConflictingTransactions() {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(
				Utils.generateRandomAddress(),
				new TimeInstant(10));
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1))));
	}

	@Test
	public void getTransactionsForNewBlockDoesNotIncludeExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account2, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		transactions.forEach(context::signAndAddExisting);
		final MockTransaction transaction = new MockTransaction(account2, 5, new TimeInstant(1));
		transaction.setDeadline(new TimeInstant(3600));
		transaction.sign();
		context.signAndAddExisting(transaction);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(account1.getAddress(), new TimeInstant(3601));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2, 3, 4)));
	}
	//endregion

	//region tests with real validator

	@Test
	public void getTransactionsForNewBlockFiltersOutConflictingTransactions() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final Account sender = context.addAccount(Amount.fromNem(10));
		final Account recipient = context.addAccount();
		final UnconfirmedTransactions transactions = context.transactions;
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

		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
		t1.setFee(Amount.fromNem(1));
		t1.sign();
		transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		t2.setFee(Amount.fromNem(2));
		t2.sign();
		transactions.addExisting(t2);

		final List<Transaction> filtered = transactions.getTransactionsForNewBlock(
				Utils.generateRandomAddress(),
				currentTime.addSeconds(1)).getAll();

		// Assert:
		// note: this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(t2, t1)));
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t1)));
	}

	@Test
	public void transactionIsExcludedFromNextBlockIfConfirmedBalanceIsInsufficient() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final UnconfirmedTransactions transactions = context.transactions;
		final Account sender = context.addAccount(Amount.fromNem(10));
		final Account recipient = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
		// 		- initially the balances are: S = 10, R = 0
		// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 2, R = 5, 3 Fee
		// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 4, R = 1, 5 Fee
		//   - getTransactionsBefore() returns SORTED transactions, so S->R is ordered before R->S because it has a greater fee
		//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough *confirmed* balance
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		t2.setFee(Amount.fromNem(2));
		t2.sign();
		transactions.addExisting(t2);

		final List<Transaction> filtered = transactions.getTransactionsForNewBlock(
				Utils.generateRandomAddress(),
				currentTime.addSeconds(1)).getAll();

		// Assert:
		// - this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(t1, t2)));
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t1)));
	}

	@Test
	public void checkingUnconfirmedTransactionsDisallowsAddingDoubleSpendTransactions() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final UnconfirmedTransactions transactions = context.transactions;
		final Account sender = context.addAccount(Amount.fromNem(10));
		final Account recipient = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7));
		t1.sign();
		final ValidationResult result1 = transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime.addSeconds(-1), sender, recipient, Amount.fromNem(7));
		t2.sign();
		final ValidationResult result2 = transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(t1)));
	}


	@Test
	public void getTransactionsForNewBlockDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final AccountStateCache poiFacade = Mockito.mock(AccountStateCache.class);
		final TestContext context = createUnconfirmedTransactionsWithRealValidator(poiFacade);
		final TimeInstant currentTime = new TimeInstant(11);

		final Account multisig = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account cosigner1 = Utils.generateRandomAccount();
		final Account cosigner2 = Utils.generateRandomAccount();
		final Transaction t1 = createTransferTransaction(currentTime, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = new MultisigTransaction(currentTime, cosigner1, t1);
		multisigTransaction.setDeadline(multisigTransaction.getTimeStamp().addHours(2));
		multisigTransaction.sign();

		addPoiState(poiFacade, multisig).getAccountInfo().incrementBalance(Amount.fromNem(10));
		addPoiState(poiFacade, cosigner1).getAccountInfo().incrementBalance(Amount.fromNem(101));
		addPoiState(poiFacade, cosigner2);
		makeCosignatory(poiFacade, cosigner1, multisig);
		makeCosignatory(poiFacade, cosigner2, multisig);

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.transactions.getTransactionsForNewBlock(Utils.generateRandomAddress(), currentTime.addMinutes(10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(0));
	}

	// note: this test actually tests few things, which means it's most likely ugly
	// * adding multisig signature adds it to PRESENT MultisigTransaction
	// * multisig signatures are NOT returned by getTransactionsForNewBlock
	// * multisig transaction is returned only if (number of cosignatories-1) == number of sigs
	@Test
	public void addingMultisigSignatureAddsItToMultisigTransaction() {
		// Arrange:
		final AccountStateCache poiFacade = Mockito.mock(AccountStateCache.class);
		final TestContext context = createUnconfirmedTransactionsWithRealValidator(poiFacade);
		final TimeInstant currentTime = new TimeInstant(11);

		final Account multisig = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account cosigner1 = Utils.generateRandomAccount();
		final Account cosigner2 = Utils.generateRandomAccount();
		final Transaction t1 = createTransferTransaction(currentTime, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction multisigTransaction = new MultisigTransaction(currentTime, cosigner1, t1);
		multisigTransaction.setDeadline(multisigTransaction.getTimeStamp().addHours(2));
		multisigTransaction.sign();
		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, cosigner2, HashUtils.calculateHash(t1));
		signatureTransaction.setDeadline(signatureTransaction.getTimeStamp().addHours(2));
		signatureTransaction.sign();

		addPoiState(poiFacade, multisig).getAccountInfo().incrementBalance(Amount.fromNem(10));
		addPoiState(poiFacade, cosigner1).getAccountInfo().incrementBalance(Amount.fromNem(101));
		addPoiState(poiFacade, cosigner2).getAccountInfo().incrementBalance(Amount.fromNem(101));
		makeCosignatory(poiFacade, cosigner1, multisig);
		makeCosignatory(poiFacade, cosigner2, multisig);

		final ValidationResult result1 = context.transactions.addExisting(multisigTransaction);
		final ValidationResult result2 = context.transactions.addExisting(signatureTransaction);

		// Act:
		final UnconfirmedTransactions blockTransactions = context.transactions.getTransactionsForNewBlock(Utils.generateRandomAddress(), currentTime.addMinutes(10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		final MultisigTransaction transaction = (MultisigTransaction)blockTransactions.getAll().get(0);
		Assert.assertThat(transaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getCosignerSignatures().get(0), IsSame.sameInstance(signatureTransaction));
	}

	private static AccountState addPoiState(final AccountStateCache poiFacade, final Account account) {
		final Address address = account.getAddress();
		final AccountState state = new AccountState(address);
		Mockito.when(poiFacade.findStateByAddress(address))
				.thenReturn(state);

		return state;
	}

	public static void makeCosignatory(final AccountStateCache stateCache, final Account signer, final Account multisig) {
		final BlockHeight blockHeight = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);
		stateCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
		stateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), blockHeight);
	}

	private static TestContext createUnconfirmedTransactionsWithRealValidator() {
		return createUnconfirmedTransactionsWithRealValidator(Mockito.mock(AccountStateCache.class));
	}

	private static TestContext createUnconfirmedTransactionsWithRealValidator(final AccountStateCache stateCache) {
		final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory();
		return new TestContext(
				factory.createSingle(stateCache),
				factory.createBatch(Mockito.mock(DefaultHashCache.class)),
				stateCache);
	}

	//endregion

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	private static List<MockTransaction> createMockTransactions(final int startCustomField, final int endCustomField) {
		final List<MockTransaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			final MockTransaction transaction = new MockTransaction(
					Utils.generateRandomAccount(), //Amount.fromNem(1000)),
					i,
					new TimeInstant(i));
			transaction.setFee(Amount.fromNem(i));
			transactions.add(transaction);
		}

		return transactions;
	}

	private static Collection<Transaction> createMockTransactionsAsBatch(final int startCustomField, final int endCustomField) {
		return createMockTransactions(startCustomField, endCustomField).stream().collect(Collectors.toList());
	}

	private static List<MockTransaction> addMockTransactions(
			final UnconfirmedTransactions unconfirmedTransactions,
			final int startCustomField,
			final int endCustomField) {
		final List<MockTransaction> transactions = createMockTransactions(startCustomField, endCustomField);
		transactions.forEach(Transaction::sign);
		transactions.forEach(unconfirmedTransactions::addExisting);
		return transactions;
	}

	private static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(transaction -> ((MockTransaction)transaction).getCustomField())
				.collect(Collectors.toList());
	}

	private static List<TimeInstant> getTimeInstantsAsList(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(Transaction::getTimeStamp)
				.collect(Collectors.toList());
	}

	private static class TestContext {
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;
		private final ReadOnlyAccountStateCache accountStateCache;
		private final TimeProvider timeProvider;

		private TestContext() {
			this(Mockito.mock(SingleTransactionValidator.class), Mockito.mock(BatchTransactionValidator.class));
			this.setSingleValidationResult(ValidationResult.SUCCESS);
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext(final SingleTransactionValidator singleValidator) {
			this(singleValidator, Mockito.mock(BatchTransactionValidator.class));
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext(final SingleTransactionValidator singleValidator, final BatchTransactionValidator batchValidator) {
			this(singleValidator, batchValidator, Mockito.mock(ReadOnlyAccountStateCache.class));
		}

		private TestContext(
				final SingleTransactionValidator singleValidator,
				final BatchTransactionValidator batchValidator,
				final ReadOnlyAccountStateCache accountStateCache) {
			this.singleValidator = singleValidator;
			this.batchValidator = batchValidator;
			this.accountStateCache = accountStateCache;
			this.timeProvider = Mockito.mock(TimeProvider.class);
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);
			Mockito.when(validatorFactory.createSingle(Mockito.any())).thenReturn(this.singleValidator);
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(TimeInstant.ZERO);
			this.transactions = new UnconfirmedTransactions(
					validatorFactory,
					NisCacheFactory.createReadOnly(this.accountStateCache, transactionHashCache),
					this.timeProvider);
		}

		private void setSingleValidationResult(final ValidationResult result) {
			Mockito.when(this.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		}

		private void setBatchValidationResult(final ValidationResult result) {
			Mockito.when(this.batchValidator.validate(Mockito.any())).thenReturn(result);
		}

		private ValidationResult signAndAddExisting(final Transaction transaction) {
			transaction.sign();
			return this.transactions.addExisting(transaction);
		}

		private ValidationResult signAndAddNew(final Transaction transaction) {
			transaction.sign();
			return this.transactions.addNew(transaction);
		}

		private ValidationResult signAndAddNewBatch(final Collection<Transaction> transactions) {
			transactions.forEach(Transaction::sign);
			return this.transactions.addNewBatch(transactions);
		}

		private Account addAccount() {
			return this.addAccount(Amount.ZERO);
		}

		private Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		private Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

		private List<MockTransaction> createMockTransactions(final int startCustomField, final int endCustomField) {
			final List<MockTransaction> transactions = new ArrayList<>();

			for (int i = startCustomField; i <= endCustomField; ++i) {
				transactions.add(this.createMockTransaction(Utils.generateRandomAccount(), new TimeInstant(i), i));
			}

			return transactions;
		}

		private List<Transaction> createMockTransactionsWithRandomTimeStamp(final Account account, final int count) {
			final List<Transaction> transactions = new ArrayList<>();
			final SecureRandom random = new SecureRandom();

			for (int i = 0; i < count; ++i) {
				transactions.add(this.createMockTransaction(account, new TimeInstant(random.nextInt(1_000_000)), i));
			}

			return transactions;
		}

		private MockTransaction createMockTransaction(final Account account, final TimeInstant timeStamp, final int customField) {
			this.prepareAccount(account, Amount.fromNem(1000));
			final MockTransaction transaction = new MockTransaction(account, customField, timeStamp);
			transaction.setFee(Amount.fromNem(customField));
			return transaction;
		}

		private Collection<Transaction> createMockTransactionsAsBatch(final int startCustomField, final int endCustomField) {
			return this.createMockTransactions(startCustomField, endCustomField).stream().collect(Collectors.toList());
		}

		private List<MockTransaction> addMockTransactions(
				final UnconfirmedTransactions unconfirmedTransactions,
				final int startCustomField,
				final int endCustomField) {
			final List<MockTransaction> transactions = this.createMockTransactions(startCustomField, endCustomField);
			transactions.forEach(Transaction::sign);
			transactions.forEach(unconfirmedTransactions::addExisting);
			return transactions;
		}
	}
}
