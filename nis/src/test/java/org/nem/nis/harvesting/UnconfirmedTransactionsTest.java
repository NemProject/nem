package org.nem.nis.harvesting;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;
	private static final TimeProvider TIME_PROVIDER = Utils.createMockTimeProvider(25);

	//region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();

		// Act:
		for (int i = 0; i < 17; ++i) {
			context.transactions.addExisting(new MockTransaction(account, i));
		}

		// Assert:
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(17));
	}

	//endregion

	//region getUnconfirmedBalance

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceWhenNoPendingTransactionsImpactAccount() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(5));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext(new TransferTransactionValidator());

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(5)));
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(100)));
	}

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingTransactionsImpactingAccount() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(4));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(5), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(4), null));
		transactions.get(0).setFee(Amount.fromNem(1));
		transactions.get(1).setFee(Amount.fromNem(2));
		transactions.forEach(context.transactions::addExisting);

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(3)));
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(98)));
	}

	//endregion

	//region add[Batch/New/Existing]

	@Test
	public void addNewBatchReturnsSuccessIfAtLeastOneTransactionCanBeSuccessfullyAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(10));
		final ValidationResult result = context.transactions.addNewBatch(Arrays.asList(transaction, transaction));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addNewBatchReturnsNeutralIfNoTransactionCanBeSuccessfullyAdded() {
		// TODO 20141104 J-B: why not failure?
		// TODO 20141105 BR -> J: which FAILURE should be returned? In a batch there could be many different failures.
		// TODO                   You don't want to stop if one transaction fails once the batch validation succeeded or do you?
		// TODO 20141106 J-B: 'You don't want to stop if one transaction fails' - right now it just feels inconsistent since
		// > we stop if one transaction in the batch fails batch validation but not if one fails regular validation
		// > however, this is moot since we aren't actually checking the result :)
		// TODO 20141107 BR -> J: Not moot, just a question if an attack vector is possible, see comments in UnconfirmedTransactions class.
		// TODO 20141107 J-B: well, moot now because BlockChain is not checking the result ;)

		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_FUTURE_DEADLINE);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(40));
		final ValidationResult result = context.transactions.addNewBatch(Arrays.asList(transaction, transaction));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addNewBatchReturnsFailureIfBatchValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.SUCCESS);
		context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(15));
		final ValidationResult result = context.transactions.addNewBatch(Arrays.asList(transaction));

		// Assert:
		Assert.assertThat(result.isFailure(), IsEqual.equalTo(true));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addSucceedsIfTransactionWithSameHashHasNotAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfTransactionWithSameHashHasAlreadyBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));
		context.transactions.addExisting(new MockTransaction(sender, 7));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void multipleTransactionsWithDifferentHashesCanBeAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		context.transactions.addExisting(new MockTransaction(sender, 7));

		final MockTransaction transaction = new MockTransaction(sender, 8);
		final ValidationResult result = context.transactions.addExisting(transaction);

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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.addExisting(transaction);

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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.addNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_HASH_EXISTS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addNewFailsIfValidationReturnsNeutral() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.NEUTRAL);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(30));
		final ValidationResult result = context.transactions.addNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addFailsIfTransactionValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
		Mockito.verify(context.singleValidator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
	}

	@Test
	public void addFailsIfTransactionConflictsWithExistingImportanceTransferTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account remote = Utils.generateRandomAccount(Amount.fromNem(100));

		final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferTransaction.Mode.Activate, remote);
		context.transactions.addExisting(t1);

		// Act:
		final ValidationResult result = context.transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext(new UniversalTransactionValidator());
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));

		final Transaction t1 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		context.transactions.addExisting(t1);

		// Act:
		final Transaction t2 = new MockTransaction(sender);
		t2.setFee(Amount.fromNem(5));
		final ValidationResult result = context.transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addExistingDelegatesToSingleTransactionValidatorButNotBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.addExisting(transaction);

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		Mockito.verify(context.batchValidator, Mockito.never()).validate(Mockito.any());
	}

	@Test
	public void addNewDelegatesToSingleTransactionValidatorAndBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.addNew(transaction);

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		assertBatchValidation(context.batchValidator, transaction);
	}

	@Test
	public void addNewBatchDelegatesToSingleTransactionValidatorAndBatchTransactionValidatorForValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.addNewBatch(Arrays.asList(transaction));

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

	//region time-based validation

	@Test
	public void addExistingSucceedsIfTimeStampIsTooFarInThePast() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(TIME_PROVIDER.getCurrentTime()).thenReturn(new TimeInstant(1000));
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(900));
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	// TODO 20141107 BR -> G: Why should this test pass? (It does because of the way it is set up)
	@Test
	public void addExistingSucceedsIfTimeStampIsTooFarInTheFuture() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(TIME_PROVIDER.getCurrentTime()).thenReturn(new TimeInstant(1000));
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(1100));
		final ValidationResult result = context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addNewSucceedsIfTimeStampIsTooFarInThePast() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(TIME_PROVIDER.getCurrentTime()).thenReturn(new TimeInstant(1000));
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(900));
		final ValidationResult result = context.transactions.addNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addNewFailsIfTimeStampIsTooFarInTheFuture() {
		// Arrange:
		final TestContext context = new TestContext(new NonFutureEntityValidator(TIME_PROVIDER));
		Mockito.when(TIME_PROVIDER.getCurrentTime()).thenReturn(new TimeInstant(1000));
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(1100));
		final ValidationResult result = context.transactions.addNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void addNewCanAddTransactionAtGenesisTime() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(TIME_PROVIDER.getCurrentTime()).thenReturn(new TimeInstant(25));
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7, TimeInstant.ZERO);
		final ValidationResult result = context.transactions.addNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	//endregion

	//region execution

	@Test
	public void addSuccessExecutesTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = Mockito.spy(new MockTransaction(sender, 7));
		context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Mockito.verify(transaction, Mockito.times(1)).execute(Mockito.any());
	}

	@Test
	public void addFailureDoesNotExecuteTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.transactions.addExisting(transaction);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
	}

	//endregion

	@Test
	public void transactionCanBeAddedIfValidationSucceedsAfterValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		final MockTransaction transaction = new MockTransaction(sender, 7);
		ValidationResult result = context.transactions.addExisting(transaction);
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));

		Mockito.when(context.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		result = context.transactions.addExisting(transaction);

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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		context.transactions.addExisting(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8);
		context.transactions.addExisting(toRemove);
		context.transactions.addExisting(new MockTransaction(sender, 9));

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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		context.transactions.addExisting(new MockTransaction(sender, 7));
		final Transaction toRemove = new MockTransaction(sender, 8); // never added
		context.transactions.addExisting(new MockTransaction(sender, 9));

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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

		// Act:
		// (for some reason passing the spied transaction to both remove and add does not work)
		final MockTransaction transaction = new MockTransaction(sender, 7);
		final Transaction spiedTransaction = Mockito.spy(transaction);
		context.transactions.addExisting(transaction);
		context.transactions.remove(spiedTransaction);

		// Assert:
		Mockito.verify(spiedTransaction, Mockito.times(1)).undo(Mockito.any());
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void removeFailureDoesNotUndoTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));

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
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.transactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
	}

	//endregion

	//region getTransactions

	@Test
	public void getMostImportantTransactionsReturnsAllTransactionsIfLessThanMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		addMockTransactions(context.transactions, 6, 9);

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
		addMockTransactions(context.transactions, 6, 90);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsIfMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		addMockTransactions(context.transactions, 6, 6 + MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1);

		// Act:
		final List<Transaction> transactions = context.transactions.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> originalTransactions = createMockTransactions(6, 9);
		originalTransactions.get(2).setFee(Amount.fromNem(11));
		originalTransactions.forEach(context.transactions::addExisting);

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
		transactions.forEach(context.transactions::addExisting);

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
		transactions.forEach(context.transactions::addExisting);

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
		transactions.forEach(context.transactions::addExisting);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(1));
	}

	//endregion

	//region getTransactionsForAccount

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAccountAsSigner() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account1, 1),
				new MockTransaction(account2, 2),
				new MockTransaction(account1, 3),
				new MockTransaction(account2, 4));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account1.getAddress());
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 3)));
	}

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAccountAsTransferRecipient() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account3 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();
		final List<TransferTransaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account1, account2, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(2), account2, account1, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(3), account1, account3, Amount.ZERO, null));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account3.getAddress());
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(3))));
	}

	@Test
	public void getTransactionsForAccountIncludesAllAccountsWithAffectingAccount() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account3 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account1, account2, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(2), account2, account1, Amount.ZERO, null),
				new TransferTransaction(new TimeInstant(3), account1, account3, Amount.ZERO, null),
				new MockTransaction(account2, 1, new TimeInstant(4)));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForAccount(account2.getAddress());
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1), new TimeInstant(2), new TimeInstant(4))));
	}

	@Test
	public void getTransactionsForAccountIncludesConflictingTransactions() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(5));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(context.transactions::addExisting);

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
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account2, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(account1.getAddress(), new TimeInstant(6));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2)));
	}

	@Test
	public void getTransactionsForNewBlockExcludesTransactionsSignedByHarvesterAddress() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account1, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account1, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(account1.getAddress(), new TimeInstant(10));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(2, 4)));
	}

	@Test
	public void getTransactionsForNewBlockExcludesConflictingTransactions() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount(Amount.fromNem(5));
		final Account account2 = Utils.generateRandomAccount(Amount.fromNem(100));
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(context.transactions::addExisting);

		// Act:
		final UnconfirmedTransactions filteredTransactions = context.transactions.getTransactionsForNewBlock(
				Utils.generateRandomAddress(),
				new TimeInstant(10));
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions.getAll());

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1))));
	}

	//endregion

	//region tests with real validator

	@Test
	public void getTransactionsForNewBlockFiltersOutConflictingTransactions() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account recipient = Utils.generateRandomAccount();
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

		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(5));
		transactions.addExisting(t1);
		t1.setFee(Amount.fromNem(1));
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		t2.setFee(Amount.fromNem(2));
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
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
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
		transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(2));
		t2.setFee(Amount.fromNem(2));
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
	// TODO 20140926 J-G how would he stop the whole network; when re-validating against confirmed balance only one would be chosen?

	// TODO 20141106 J-G,B: while we're here can you answer my question ;)?
	// TODO 20141107 BR -> J: the term ValidateAgainstConfirmedBalance is misleading, it means that the transaction is not executed during add().
	// TODO                   There is no check against a confirmed balance.
	// TODO                   So the above is valid and a harvester would create a block that doesn't pass processBlock().
	@Test
	public void checkingUnconfirmedTransactionsDisallowsAddingDoubleSpendTransactions() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(10));
		final Account recipient = Utils.generateRandomAccount();
		final UnconfirmedTransactions transactions = createUnconfirmedTransactionsWithRealValidator();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7));
		transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime.addSeconds(-1), sender, recipient, Amount.fromNem(7));
		transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Arrays.asList(t1)));
	}

	private static UnconfirmedTransactions createUnconfirmedTransactionsWithRealValidator() {
		final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory();
		final TestContext context = new TestContext(
				factory.create(Mockito.mock(PoiFacade.class)),
				factory.createBatch(Mockito.mock(PoiFacade.class)));
		return context.transactions;
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
					Utils.generateRandomAccount(Amount.fromNem(100)),
					i,
					new TimeInstant(i));
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
				.map(transaction -> transaction.getTimeStamp())
				.collect(Collectors.toList());
	}

	private static class TestContext {
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;

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
			this.singleValidator = singleValidator;
			this.batchValidator = batchValidator;
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
			Mockito.when(validatorFactory.createBatch(poiFacade)).thenReturn(this.batchValidator);
			Mockito.when(validatorFactory.createSingle(poiFacade)).thenReturn(this.singleValidator);
			this.transactions = new UnconfirmedTransactions(
					TIME_PROVIDER,
					validatorFactory,
					poiFacade);
		}

		private void setSingleValidationResult(final ValidationResult result) {
			Mockito.when(this.singleValidator.validate(Mockito.any())).thenReturn(result);
			Mockito.when(this.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		}

		private void setBatchValidationResult(final ValidationResult result) {
			Mockito.when(this.batchValidator.validate(Mockito.any())).thenReturn(result);
		}
	}
}
