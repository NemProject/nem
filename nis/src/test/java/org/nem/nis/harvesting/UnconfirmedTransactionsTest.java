package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsTest {
	private static final int CONFIRMED_BLOCK_HEIGHT = 3452;

	//region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.addAccount(Amount.fromNem(1000));

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
		final TestContext context = new TestContext(new BalanceValidator());
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
		final Account account1 = context.addAccount(Amount.fromNem(14));
		final Account account2 = context.addAccount(Amount.fromNem(110));
		final TimeInstant currentTime = new TimeInstant(11);
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(currentTime, account2, account1, Amount.fromNem(15), null),
				new TransferTransaction(currentTime, account1, account2, Amount.fromNem(14), null));
		setFeeAndDeadline(transactions.get(0), Amount.fromNem(2));
		setFeeAndDeadline(transactions.get(1), Amount.fromNem(3));
		transactions.forEach(context::signAndAddExisting);

		// Assert:
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(12)));
		Assert.assertThat(context.transactions.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(107)));
	}

	@Test
	public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingImportanceTransactionsImpactingAccount() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final Account sender = context.addAccount(Amount.fromNem(500000));
		final Account remote = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);
		final Transaction t1 = new ImportanceTransferTransaction(currentTime, sender, ImportanceTransferMode.Activate, remote);
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
	public void addNewBatchDoesNotShortCircuitButReturnsFirstFailureIfAnyTransactionFailsSingleValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_FUTURE_DEADLINE,
				ValidationResult.SUCCESS,
				ValidationResult.FAILURE_UNKNOWN);

		// Act:
		final ValidationResult result = context.signAndAddNewBatch(context.createMockTransactionsAsBatch(1, 4));

		// Assert: all successfully validated transactions were added and the first failure was returned
		Mockito.verify(context.singleValidator, Mockito.times(4)).validate(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
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
		final ValidationResult result = context.signAndAddNewBatch(Collections.singletonList(transaction));

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

	@Test
	public void cannotAddChildTransactionIfParentHasBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = new MockTransaction(sender, 7);
		final MockTransaction outer = new MockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));

		context.signAndAddExisting(outer);

		// Act
		final ValidationResult result = context.signAndAddNew(inner);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void cannotAddParentTransactionIfChildHasBeenAdded() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = new MockTransaction(sender, 7);
		final MockTransaction outer = new MockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));

		context.signAndAddExisting(inner);

		// Act
		final ValidationResult result = context.signAndAddNew(outer);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void addNewReturnsFailureIfCacheIsTooFull() {
		// Arrange:
		final TestContext context = new TestContext();
		final AccountState state = Mockito.mock(AccountState.class);
		final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);
		Mockito.when(context.poiFacade.getLastPoiRecalculationHeight()).thenReturn(BlockHeight.ONE);
		Mockito.when(context.accountStateCache.findStateByAddress(Mockito.any())).thenReturn(state);
		Mockito.when(state.getImportanceInfo()).thenReturn(accountImportance);
		Mockito.when(accountImportance.getHeight()).thenReturn(BlockHeight.ONE);
		Mockito.when(accountImportance.getImportance(BlockHeight.ONE)).thenReturn(0.0);
		context.addMockTransactions(context.transactions, 1, 900);

		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		final ValidationResult result = context.signAndAddNew(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL));
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
	public void addAllowsConflictingImportanceTransferTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(50000));
		final Account remote = context.addAccount(Amount.fromNem(100));

		final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferMode.Activate, remote);
		final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferMode.Activate, remote);
		context.signAndAddExisting(t1);

		// Act:
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext(new BalanceValidator());
		final Account sender = context.addAccount(Amount.fromNem(10));

		final MockTransaction t1 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		context.signAndAddExisting(t1);

		// Act:
		final MockTransaction t2 = new MockTransaction(sender);
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
		context.signAndAddNewBatch(Collections.singletonList(transaction));

		// Assert:
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), Mockito.any());
		assertBatchValidation(context.batchValidator, transaction);
	}

	private static void assertBatchValidation(final BatchTransactionValidator validator, final Transaction transaction) {
		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(validator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		Assert.assertThat(pair.getTransactions(), IsEquivalent.equivalentTo(Collections.singletonList(transaction)));
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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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

	@Test
	public void removeAllRebuildsCacheIfIllegalArgumentExceptionOccurs() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(new BalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(50));

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(0));

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.removeAll(block);

		// Assert:
		// - removing the first transaction triggers an exception and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (50) < 80 + 2
		// - second transaction cannot be added - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
	}

	@Test
	public void removeAllRebuildsCacheIfInvalidTransactionInCacheIsDetected() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(new BalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 20, 0);

		final Block block = NisUtils.createRandomBlock();
		final TransferTransaction transaction = context.createTransferTransaction(
				transactions.get(0).getSigner(),
				transactions.get(0).getRecipient(),
				Amount.fromNem(8),
				new TimeInstant(8));
		block.addTransaction(transaction);

		// Act:
		final int numTransactions = context.transactions.size();

		// Before the call to removeAll the transaction contained in the block is usually executed (which
		// will change the confirmed balance) and thus account1 is debited 80 + 2 NEM and account2 is credited 80 NEM
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(18));
		context.setBalance(transactions.get(1).getSigner(), Amount.fromNem(100));
		context.transactions.removeAll(block);

		// Assert:
		// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (18) < 80 + 2
		// - second transaction can be added - account2 balance (100) >= 50 + 2
		// - third transaction can be added - account2 balance (48) >= 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.getAll(), IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
	}

	//endregion

	//region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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

	//region getTransactionsBefore

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(context.transactions, 6, 9);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getTransactionsBefore(new TimeInstant(8)));

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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getTransactionsBefore(new TimeInstant(8)));

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
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.getAll());

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

	@Test
	public void dropExpiredTransactionsDropsAllTransactionsThatAreDependentOnTheDroppedTransactions() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(new BalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		// - first transaction was dropped because it expired
		// - second was dropped because it was dependent on the first - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
	}

	//endregion

	//region tests with real validator

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
		Assert.assertThat(transactions.getAll(), IsEqual.equalTo(Collections.singletonList(t1)));
	}

	//endregion

	//region validation context heights

	@Test
	public void singleValidationContextHeightsAreSetCorrectly() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddExisting(transaction);

		// Assert:
		final ArgumentCaptor<ValidationContext> validationContextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
		Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.any(), validationContextCaptor.capture());
		assertCapturedValidationContext(validationContextCaptor.getValue());
	}

	@Test
	public void batchValidationContextHeightsAreSetCorrectly() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		// Act:
		final MockTransaction transaction = new MockTransaction(sender, 7);
		context.signAndAddNewBatch(Collections.singletonList(transaction));

		// Assert:
		final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
		Mockito.verify(context.batchValidator, Mockito.only()).validate(pairsCaptor.capture());

		final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
		assertCapturedValidationContext(pair.getContext());
	}

	//endregion

	private static void assertCapturedValidationContext(final ValidationContext context) {
		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT + 1)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT)));
	}

	private static TestContext createUnconfirmedTransactionsWithRealValidator() {
		return createUnconfirmedTransactionsWithRealValidator(Mockito.mock(NisCache.class));
	}

	private static TestContext createUnconfirmedTransactionsWithRealValidator(final NisCache nisCache) {
		final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory(new SystemTimeProvider());
		return new TestContext(
				() -> factory.createSingleBuilder(nisCache),
				null,
				factory.createBatch(Mockito.mock(DefaultHashCache.class)),
				nisCache,
				Mockito.mock(ReadOnlyPoiFacade.class));
	}

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	private static class TestContext {
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;
		private final ReadOnlyNisCache nisCache;
		private final ReadOnlyAccountStateCache accountStateCache;
		private final ReadOnlyNamespaceCache namespaceCache;
		private final ReadOnlyPoiFacade poiFacade;
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
			this(
					null,
					singleValidator,
					batchValidator,
					Mockito.mock(ReadOnlyNisCache.class),
					Mockito.mock(ReadOnlyPoiFacade.class));
		}

		private TestContext(
				final Supplier<AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier,
				final SingleTransactionValidator singleValidator,
				final BatchTransactionValidator batchValidator,
				final ReadOnlyNisCache nisCache,
				final ReadOnlyPoiFacade poiFacade) {
			this.singleValidator = singleValidator;
			this.batchValidator = batchValidator;
			this.nisCache = nisCache;
			this.poiFacade = poiFacade;
			this.timeProvider = Mockito.mock(TimeProvider.class);
			this.accountStateCache = Mockito.mock(AccountStateCache.class);
			this.namespaceCache = Mockito.mock(NamespaceCache.class);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
			Mockito.when(this.nisCache.getNamespaceCache()).thenReturn(this.namespaceCache);
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);

			if (null != singleTransactionBuilderSupplier) {
				setSingleTransactionBuilderSupplier(validatorFactory, singleTransactionBuilderSupplier);
			} else {
				setSingleTransactionBuilderSupplier(validatorFactory, () -> {
					final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
					builder.add(this.singleValidator);
					return builder;
				});
			}

			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(TimeInstant.ZERO);
			this.transactions = new UnconfirmedTransactions(
					validatorFactory,
					NisCacheFactory.createReadOnly(this.accountStateCache, transactionHashCache, this.poiFacade),
					this.timeProvider,
					() -> new BlockHeight(CONFIRMED_BLOCK_HEIGHT));
		}

		private static void setSingleTransactionBuilderSupplier(
				final TransactionValidatorFactory validatorFactory,
				final Supplier<AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier) {
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.get());
			Mockito.when(validatorFactory.createIncompleteSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.get());
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

		public Account prepareAccount(final Account account, final Amount amount) {
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

		public TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TimeInstant deadline) {
			final TransferTransaction transaction = new TransferTransaction(deadline, sender, recipient, amount, null);
			transaction.setFee(Amount.fromNem(1));
			transaction.setDeadline(deadline);
			return transaction;
		}

		public List<TransferTransaction> createThreeTransferTransactions(
				final int amount1,
				final int amount2,
				final int amount3) {
			final Account account1 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount1));
			final Account account2 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount2));
			final Account account3 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount3));
			final List<TransferTransaction> transactions = new ArrayList<>();
			transactions.add(this.createTransferTransaction(account1, account2, Amount.fromNem(80), new TimeInstant(5)));
			transactions.add(this.createTransferTransaction(account2, account3, Amount.fromNem(50), new TimeInstant(8)));
			transactions.add(this.createTransferTransaction(account2, account3, Amount.fromNem(10), new TimeInstant(9)));
			transactions.forEach(this::signAndAddExisting);
			return transactions;
		}

		public void setBalance(final Account account, final Amount amount) {
			this.prepareAccount(account, amount);
		}
	}
}
