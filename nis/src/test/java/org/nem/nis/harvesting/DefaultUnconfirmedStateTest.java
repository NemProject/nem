package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class DefaultUnconfirmedStateTest {
	private static final int CONFIRMED_BLOCK_HEIGHT = 3452;
	private static final int MOSAIC_CONFIRMED_BLOCK_HEIGHT = 1103452;

	@Before
	public void setup() {
		Utils.setupGlobals();
	}

	@After
	public void destroy() {
		Utils.resetGlobals();
	}

	//region getUnconfirmedBalance

	public static class UnconfirmedBalanceTest {

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceWhenNoPendingTransactionsImpactAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account account1 = context.addAccount(Amount.fromNem(5));
			final Account account2 = context.addAccount(Amount.fromNem(100));

			// Assert:
			Assert.assertThat(context.state.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(5)));
			Assert.assertThat(context.state.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(100)));
		}

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingTransactionsImpactingAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			final List<Transaction> transactions = Arrays.asList(
					createTransfer(account2, account1, 15, 2),
					createTransfer(account1, account2, 14, 3));
			transactions.forEach(context.state::addNew);

			// Assert:
			Assert.assertThat(context.state.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(12)));
			Assert.assertThat(context.state.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(107)));
		}

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingImportanceTransactionsImpactingAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account sender = context.addAccount(Amount.fromNem(500000));
			final Account remote = context.addAccount(Amount.ZERO);
			final Transaction t1 = createImportanceTransfer(sender, remote, 10);
			context.state.addNew(t1);

			// Assert:
			Assert.assertThat(context.state.getUnconfirmedBalance(sender), IsEqual.equalTo(Amount.fromNem(499990)));
		}
	}

	//endregion

	//region getUnconfirmedMosaicBalance

	public static class UnconfirmedMosaicBalanceTest {

		@Test
		public void getUnconfirmedMosaicBalanceReturnsConfirmedMosaicBalanceWhenNoPendingTransactionsImpactAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final MosaicId mosaicId1 = Utils.createMosaicId(1);
			final MosaicId mosaicId2 = Utils.createMosaicId(2);
			final Account account1 = context.addAccount(Amount.fromNem(100));
			final Account account2 = context.addAccount(Amount.fromNem(100));
			context.addMosaic(account1, mosaicId1, Supply.fromValue(12));
			context.addMosaic(account2, mosaicId2, Supply.fromValue(21));

			// Assert:
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId1), IsEqual.equalTo(Quantity.fromValue(12_000)));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId2), IsEqual.equalTo(Quantity.ZERO));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId1), IsEqual.equalTo(Quantity.ZERO));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId2), IsEqual.equalTo(Quantity.fromValue(21_000)));
		}

		@Test
		public void getUnconfirmedMosaicBalanceReturnsConfirmedMosaicBalanceAdjustedByAllPendingTransferTransactionsImpactingAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final MosaicId mosaicId1 = Utils.createMosaicId(1);
			final MosaicId mosaicId2 = Utils.createMosaicId(2);
			final Account account1 = context.addAccount(Amount.fromNem(500));
			final Account account2 = context.addAccount(Amount.fromNem(500));
			context.addMosaic(account1, mosaicId1, Supply.fromValue(12));
			context.addMosaic(account2, mosaicId2, Supply.fromValue(21));

			final List<Transaction> transactions = Arrays.asList(
					createTransferWithMosaicTransfer(account2, account1, 1, 200, mosaicId2, 5_000),
					createTransferWithMosaicTransfer(account1, account2, 1, 200, mosaicId1, 3_000));
			transactions.forEach(context.state::addNew);

			// Assert:
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId1), IsEqual.equalTo(Quantity.fromValue(9_000)));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId2), IsEqual.equalTo(Quantity.fromValue(5_000)));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId2), IsEqual.equalTo(Quantity.fromValue(16_000)));
			Assert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId1), IsEqual.equalTo(Quantity.fromValue(3_000)));
		}
	}

	//endregion

	public static class AddNewBatchTest {

		@Test
		public void addNewBatchReturnsSuccessIfAllTransactionsCanBeSuccessfullyAdded() {
			// Arrange:
			final TestContext context = new TestContext();
			final Collection<Transaction> transactions = createMockTransactions(context, 3, 5);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertNumDelegations(1, 1, 0);
			context.assertTransactionsAdded(transactions);
		}

		@Test
		public void addNewBatchDoesNotShortCircuitButReturnsFirstFailureIfAnyTransactionFailsSingleValidation() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setAddResult(
					ValidationResult.SUCCESS,
					ValidationResult.FAILURE_FUTURE_DEADLINE,
					ValidationResult.SUCCESS,
					ValidationResult.FAILURE_UNKNOWN);
			final List<Transaction> transactions = createMockTransactions(context, 3, 5);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert: all successfully validated state were added and the first failure was returned
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
			context.assertNumDelegations(1, 1, 0);
			context.assertTransactionsAdded(transactions);
		}

		@Test
		public void addNewBatchReturnsFailureIfBatchValidationFails() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setBatchValidationResult(ValidationResult.FAILURE_MESSAGE_TOO_LARGE);
			final List<Transaction> transactions = createMockTransactions(context, 3, 5);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
			context.assertNumDelegations(1, 1, 0);
			context.assertNoTransactionsAdded();
		}

		@Test
		public void addNewBatchAppliesSpamFilter() {
			// Arrange:
			final TestContext context = new TestContext();
			final List<Transaction> transactions = createMockTransactions(context, 3, 9);
			final List<Transaction> filteredTransactions = Arrays.asList(transactions.get(2), transactions.get(3), transactions.get(5));
			context.setSpamFilterResult(filteredTransactions);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertNumDelegations(1, 1, 0);
			context.assertTransactionsAdded(filteredTransactions);
		}
	}

	public static class AddNewTest {

		@Test
		public void addSucceedsIfTransactionWithSameHashHasNotAlreadyBeenAdded() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = context.state.addNew(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertNumDelegations(1, 1, 0);
			context.assertTransactionAdded(transaction);
		}

		@Test
		public void addFailsIfTransactionWithSameHashHasAlreadyBeenAdded() {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.transactions.contains(Mockito.any())).thenReturn(true);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = context.state.addNew(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
			context.assertNumDelegations(0, 0, 0);
			context.assertNoTransactionsAdded();
		}

		@Test
		public void multipleTransactionsWithDifferentHashesCanBeAdded() {
			// Arrange:
			final TestContext context = new TestContext();
			final List<Transaction> transactions = createMockTransactions(context, 7, 8);
			context.state.addNew(transactions.get(0));

			// Act:
			final ValidationResult result = context.state.addNew(transactions.get(1));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertNumDelegations(2, 2, 0);
			context.assertTransactionsAdded(transactions);
		}

		@Test
		public void addReturnsFailureIfCacheIsTooFull() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			context.setSpamFilterResult(Collections.emptyList());

			// Act:
			final ValidationResult result = context.state.addNew(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL));
			context.assertNumDelegations(1, 0, 0);
			context.assertNoTransactionsAdded();
		}
	}

	//region create transaction helpers

	private static Transaction createMockTransaction(final TestContext context, final int customField) {
		final Account account = context.addAccount(Amount.fromNem(1_000));
		return new MockTransaction(account, customField, new TimeInstant(customField));
	}

	private static List<Transaction> createMockTransactions(final TestContext context, final int startCustomField, final int endCustomField) {
		final List<Transaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			transactions.add(createMockTransaction(context, i));
		}

		return transactions;
	}

	private static Transaction createTransfer(final Account sender, final Account recipient, final int amount, final int fee) {
		final Transaction t = new TransferTransaction(1, TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), null);
		t.setFee(Amount.fromNem(fee));
		return t;
	}

	private static Transaction createTransferWithMosaicTransfer(
			final Account sender,
			final Account recipient,
			final int amount,
			final int fee,
			final MosaicId mosaicId,
			final int quantity) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, new Quantity(quantity));
		final Transaction t = new TransferTransaction(1, TimeInstant.ZERO, sender, recipient, Amount.fromNem(amount), attachment);
		t.setFee(Amount.fromNem(fee));
		return t;
	}

	private static Transaction createImportanceTransfer(final Account sender, final Account remote, final int fee) {
		final Transaction t = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferMode.Activate, remote);
		t.setFee(Amount.fromNem(fee));
		return t;
	}

	//endregion

	private static void setFeeAndDeadline(final Transaction transaction, final Amount fee) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
		transaction.setFee(fee);
	}

	public static class Legacy {

		//region add[Batch/New/Existing]

		//region validation

		@Test
		public void addExistingSucceedsIfBatchValidationFails() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.SUCCESS);
			context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			final MockTransaction transaction = new MockTransaction(sender, 7);
			final ValidationResult result = context.signAndAddExisting(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(1));
		}

		@Test
		public void addNewFailsIfBatchValidationFails() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.SUCCESS);
			context.setBatchValidationResult(ValidationResult.FAILURE_HASH_EXISTS);
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			final MockTransaction transaction = new MockTransaction(sender, 7);
			final ValidationResult result = context.signAndAddNew(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_HASH_EXISTS));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(0));
		}

		@Test
		public void addNewFailsIfValidationReturnsNeutral() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.NEUTRAL);
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			final MockTransaction transaction = new MockTransaction(sender, 7, new TimeInstant(30));
			final ValidationResult result = context.signAndAddNew(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(0));
		}

		@Test
		public void addFailsIfTransactionValidationFails() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.FAILURE_PAST_DEADLINE);
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			final MockTransaction transaction = new MockTransaction(sender, 7);
			final ValidationResult result = context.signAndAddExisting(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(0));
			Mockito.verify(context.singleValidator, Mockito.times(1)).validate(Mockito.eq(transaction), Mockito.any());
		}

		@Test
		public void addAllowsConflictingImportanceTransferTransactions() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account sender = context.addAccount(Amount.fromNem(50000));
			final Account remote = context.addAccount(Amount.fromNem(100));

			final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferMode.Activate, remote);
			final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferMode.Activate, remote);
			context.signAndAddExisting(t1);

			// Act:
			final ValidationResult result = context.signAndAddExisting(t2);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(2));
		}

		@Test
		public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
			// Arrange:
			final TestContext2 context = new TestContext2(createBalanceValidator());
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
			Assert.assertThat(context.state.size(), IsEqual.equalTo(1));
		}

		@Test
		public void addFailsIfSenderHasInsufficientUnconfirmedMosaicBalance() {
			// Arrange:
			final TestContext2 context = new TestContext2(createMosaicBalanceValidator());
			final MosaicId mosaicId1 = Utils.createMosaicId(1);
			final Account sender = context.addAccount(Amount.fromNem(100), mosaicId1, Supply.fromValue(10));
			final Account recipient = context.addAccount(Amount.fromNem(100));
			final TimeInstant currentTime = new TimeInstant(11);
			final Transaction t1 = new TransferTransaction(currentTime, sender, recipient, Amount.fromNem(1), createAttachment(mosaicId1, new Quantity(5_000)));
			final Transaction t2 = new TransferTransaction(currentTime, sender, recipient, Amount.fromNem(1), createAttachment(mosaicId1, new Quantity(6_000)));
			setFeeAndDeadline(t1, Amount.fromNem(20));
			setFeeAndDeadline(t2, Amount.fromNem(20));
			context.signAndAddExisting(t1);

			// Act:
			final ValidationResult result = context.signAndAddExisting(t2);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
			Assert.assertThat(context.state.size(), IsEqual.equalTo(1));
		}

		@Test
		public void addFailsIfTransactionHasExpired() {
			// Arrange:
			final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
			Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(1122450));
			final TestContext2 context = new TestContext2(new TransactionDeadlineValidator(timeProvider));
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
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.SUCCESS);
			final Account sender = context.addAccount(Amount.fromNem(10));
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.sign();

			// Act (ruin signature by altering the deadline):
			transaction.setDeadline(transaction.getDeadline().addMinutes(1));
			final ValidationResult result = context.state.addExisting(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
		}

		@Test
		public void addExistingFailsIfTransactionDoesNotVerify() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.setSingleValidationResult(ValidationResult.SUCCESS);
			final Account sender = context.addAccount(Amount.fromNem(10));
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.sign();

			// Act (ruin signature by altering the deadline):
			transaction.setDeadline(transaction.getDeadline().addMinutes(1));
			final ValidationResult result = context.state.addExisting(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
		}

		@Test
		public void addExistingDelegatesToSingleTransactionValidatorButNotBatchTransactionValidatorForValidation() {
			// Arrange:
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
			Assert.assertThat(context.state.size(), IsEqual.equalTo(1));
		}

		//endregion

		//region remove

		@Test
		public void canRemoveKnownTransaction() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account sender = context.addAccount(Amount.fromNem(100));

			context.signAndAddExisting(new MockTransaction(sender, 7));
			final Transaction toRemove = new MockTransaction(sender, 8);
			context.signAndAddExisting(toRemove);
			context.signAndAddExisting(new MockTransaction(sender, 9));

			// Act:
			final boolean isRemoved = context.state.remove(toRemove);
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(isRemoved, IsEqual.equalTo(true));
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(9, 7)));
		}

		@Test
		public void removeReturnsFalseWhenAttemptingToRemoveUnknownTransaction() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			context.signAndAddExisting(new MockTransaction(sender, 7));
			final Transaction toRemove = new MockTransaction(sender, 8); // never added
			context.signAndAddExisting(new MockTransaction(sender, 9));

			final boolean isRemoved = context.state.remove(toRemove);
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(isRemoved, IsEqual.equalTo(false));
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(9, 7)));
		}

		@Test
		public void removeSuccessUndoesTransaction() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			// (for some reason passing the spied transaction to both remove and add does not work)
			final MockTransaction transaction = new MockTransaction(sender, 7);
			final Transaction spiedTransaction = Mockito.spy(transaction);
			context.signAndAddExisting(transaction);
			context.state.remove(spiedTransaction);

			// Assert:
			Mockito.verify(spiedTransaction, Mockito.times(1)).undo(Mockito.any());
			Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		}

		@Test
		public void removeFailureDoesNotUndoTransaction() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account sender = context.addAccount(Amount.fromNem(100));

			// Act:
			final MockTransaction transaction = new MockTransaction(sender, 7);
			context.state.remove(transaction);

			// Assert:
			Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
		}

		//endregion

		//region removeAll

		@Test
		public void removeAllRemovesAllTransactionsInBlock() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.addMockTransactions(context.state, 6, 9);

			final Block block = NisUtils.createRandomBlock();
			block.addTransaction(transactions.get(1));
			block.addTransaction(transactions.get(3));

			// Act:
			context.state.removeAll(block.getTransactions());
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 8)));
		}

		@Test
		public void removeAllDoesUndoTransactions() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.addMockTransactions(context.state, 6, 9);

			final Block block = NisUtils.createRandomBlock();
			block.addTransaction(transactions.get(1));
			block.addTransaction(transactions.get(3));

			// Act:
			context.state.removeAll(block.getTransactions());

			// Assert:
			// not the greatest test, but the count is 2 for the removed state and 1 for the others
			Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(1));
			Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(1));
			Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(2));
			Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(2));
		}

		@Test
		public void removeAllRebuildsCacheIfIllegalArgumentExceptionOccurs() {
			// Arrange:
			// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
			final TestContext2 context = new TestContext2(createBalanceValidator());
			final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);
			context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(50));

			final Block block = NisUtils.createRandomBlock();
			block.addTransaction(transactions.get(0));

			// Act:
			final int numTransactions = context.state.size();
			context.state.removeAll(block.getTransactions());

			// Assert:
			// - removing the first transaction triggers an exception and forces a cache rebuild
			// - first transaction cannot be added - account1 balance (50) < 80 + 2
			// - second transaction cannot be added - account2 balance (12) < 50 + 2
			// - third transaction can be added - account2 balance (12) == 10 + 2
			Assert.assertThat(numTransactions, IsEqual.equalTo(3));
			Assert.assertThat(context.state.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
		}

		@Test
		public void removeAllRebuildsCacheIfInvalidXemTransferInCacheIsDetected() {
			// Arrange:
			// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
			final TestContext2 context = new TestContext2(createBalanceValidator());
			final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 20, 0);

			final Block block = NisUtils.createRandomBlock();
			final TransferTransaction transaction = context.createTransferTransaction(
					transactions.get(0).getSigner(),
					transactions.get(0).getRecipient(),
					Amount.fromNem(8),
					new TimeInstant(8));
			block.addTransaction(transaction);

			// Act:
			final int numTransactions = context.state.size();

			// Before the call to removeAll the transaction contained in the block is usually executed (which
			// will change the confirmed balance) and thus account1 is debited 80 + 2 NEM and account2 is credited 80 NEM
			context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(18));
			context.setBalance(transactions.get(1).getSigner(), Amount.fromNem(100));
			context.state.removeAll(block.getTransactions());

			// Assert:
			// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
			// - first transaction cannot be added - account1 balance (18) < 80 + 2
			// - second transaction can be added - account2 balance (100) >= 50 + 2
			// - third transaction can be added - account2 balance (48) >= 10 + 2
			Assert.assertThat(numTransactions, IsEqual.equalTo(3));
			Assert.assertThat(context.state.asFilter().getAll(), IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
		}

		@Test
		public void removeAllRebuildsCacheIfInvalidMosaicTransferInCacheIsDetected() {
			// Arrange:
			// 1 -> 2 80 mosaic1 | 2 -> 3 50 mosaic2 | 2 -> 3 10 mosaic2
			final TestContext2 context = new TestContext2(createMosaicBalanceValidator());
			final List<TransferTransaction> transactions = context.createThreeMosaicTransferTransactions(100, 60);

			final Block block = NisUtils.createRandomBlock();
			final TransferTransaction transaction = context.createTransferTransaction(
					transactions.get(0).getSigner(),
					transactions.get(0).getRecipient(),
					Amount.fromNem(8),
					new TimeInstant(8));
			block.addTransaction(transaction);

			// Act:
			final int numTransactions = context.state.size();

			// Decreasing the supply makes first transaction invalid
			context.decreaseSupply(Utils.createMosaicId(1), Supply.fromValue(25));
			context.state.removeAll(block.getTransactions());

			// Assert:
			// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
			// - first transaction cannot be added - account1 mosaic 1 balance (75) < 80
			// - second transaction can be added - account2 mosaic 2 balance (60) >= 50
			// - third transaction can be added - account2 mosaic 2 balance (10) >= 10
			Assert.assertThat(numTransactions, IsEqual.equalTo(3));
			Assert.assertThat(context.state.asFilter().getAll(), IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
		}

		//endregion

		//region getAll

		@Test
		public void getAllReturnsAllTransactions() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			context.addMockTransactions(context.state, 6, 9);

			// Act:
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
		}

		@Test
		public void getAllReturnsAllTransactionsInSortedOrder() {

			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
			transactions.get(2).setFee(Amount.fromNem(11));
			transactions.forEach(context::signAndAddExisting);

			// Act:
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
		}

		//endregion

		// region getUnknownTransactions

		@Test
		public void getUnknownTransactionsReturnsAllTransactionsIfHashShortIdListIsEmpty() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 3);
			context.signAndAddNewBatch(transactions);

			// Act:
			final Collection<Transaction> unknownTransactions = context.state.asFilter().getUnknownTransactions(new ArrayList<>());

			// Assert:
			Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(transactions));
		}

		@Test
		public void getUnknownTransactionsFiltersKnownTransactions() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 6);
			context.signAndAddNewBatch(transactions);
			final List<HashShortId> hashShortIds = new ArrayList<>();
			hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(1)).getShortId()));
			hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(2)).getShortId()));
			hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(4)).getShortId()));

			// Act:
			final Collection<Transaction> unknownTransactions = context.state.asFilter().getUnknownTransactions(hashShortIds);

			// Assert:
			Assert.assertThat(
					unknownTransactions,
					IsEquivalent.equivalentTo(Arrays.asList(transactions.get(0), transactions.get(3), transactions.get(5))));
		}

		@Test
		public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 6);
			context.signAndAddNewBatch(transactions);
			final List<HashShortId> hashShortIds = transactions.stream()
					.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
					.collect(Collectors.toList());

			// Act:
			final Collection<Transaction> unknownTransactions = context.state.asFilter().getUnknownTransactions(hashShortIds);

			// Assert:
			Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
		}

		// endregion

		//region getMostRecentTransactionsForAccount

		@Test
		public void getMostRecentTransactionsReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
			context.signAndAddNewBatch(transactions);

			// Act:
			final Collection<Transaction> mostRecentTransactions = context.state.asFilter().getMostRecentTransactionsForAccount(account.getAddress(), 20);

			// Assert:
			Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
		}

		@Test
		public void getMostRecentTransactionsReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 20);
			context.signAndAddNewBatch(transactions);

			// Act:
			final Collection<Transaction> mostRecentTransactions = context.state.asFilter().getMostRecentTransactionsForAccount(account.getAddress(), 10);

			// Assert:
			Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
		}

		@Test
		public void getMostRecentTransactionsReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
			context.signAndAddNewBatch(transactions);

			// Act:
			final Collection<Transaction> mostRecentTransactions = context.state.asFilter().getMostRecentTransactionsForAccount(account.getAddress(), 10);

			// Assert:
			Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
		}

		@Test
		public void getMostRecentTransactionsReturnsTransactionsSortedByTimeInDescendingOrder() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final Account account = Utils.generateRandomAccount();
			final List<Transaction> transactions = context.createMockTransactionsWithRandomTimeStamp(account, 10);
			context.signAndAddNewBatch(transactions);

			// Act:
			final Collection<Transaction> mostRecentTransactions = context.state.asFilter().getMostRecentTransactionsForAccount(account.getAddress(), 25);

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
			final TestContext2 context = new TestContext2();
			context.addMockTransactions(context.state, 6, 9);

			// Act:
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getTransactionsBefore(new TimeInstant(
					8)));

			// Assert:
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
		}

		@Test
		public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {

			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
			transactions.get(1).setFee(Amount.fromNem(11));
			transactions.forEach(context::signAndAddExisting);

			// Act:
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getTransactionsBefore(new TimeInstant(
					8)));

			// Assert:
			Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(7, 6)));
		}

		//endregion

		//region dropExpiredTransactions

		@Test
		public void dropExpiredTransactionsRemovesAllTransactionsBeforeSpecifiedTimeInstant() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
			transactions.get(0).setDeadline(new TimeInstant(5));
			transactions.get(1).setDeadline(new TimeInstant(7));
			transactions.get(2).setDeadline(new TimeInstant(6));
			transactions.get(3).setDeadline(new TimeInstant(8));
			transactions.forEach(context::signAndAddExisting);

			// Act:
			context.state.dropExpiredTransactions(new TimeInstant(7));
			final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.state.asFilter().getAll());

			// Assert:
			Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(7, 9)));
		}

		@Test
		public void dropExpiredTransactionsExecutesAllNonExpiredTransactions() {
			// Arrange:
			final TestContext2 context = new TestContext2();
			final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
			transactions.get(0).setDeadline(new TimeInstant(5));
			transactions.get(1).setDeadline(new TimeInstant(7));
			transactions.get(2).setDeadline(new TimeInstant(6));
			transactions.get(3).setDeadline(new TimeInstant(8));
			transactions.forEach(context::signAndAddExisting);

			// Act:
			context.state.dropExpiredTransactions(new TimeInstant(7));

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
			final TestContext2 context = new TestContext2(createBalanceValidator());
			final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);

			// Act:
			final int numTransactions = context.state.size();
			context.state.dropExpiredTransactions(new TimeInstant(7));

			// Assert:
			// - first transaction was dropped because it expired
			// - second was dropped because it was dependent on the first - account2 balance (12) < 50 + 2
			// - third transaction can be added - account2 balance (12) == 10 + 2
			Assert.assertThat(numTransactions, IsEqual.equalTo(3));
			Assert.assertThat(context.state.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
		}

		//endregion

		//region tests with real validator

		@Test
		public void checkingUnconfirmedTransactionsDisallowsAddingDoubleSpendTransactions() {
			// Arrange:
			final TestContext2 context = createUnconfirmedTransactionsWithRealValidator(CONFIRMED_BLOCK_HEIGHT);
			final UnconfirmedTransactions transactions = context.state;
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
			Assert.assertThat(transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(t1)));
		}

		@Test
		public void checkingUnconfirmedMosaicTransactionsDisallowsAddingDoubleSpendTransactions() {
			// Arrange:
			final TestContext2 context = createUnconfirmedTransactionsWithRealValidator(MOSAIC_CONFIRMED_BLOCK_HEIGHT);
			final UnconfirmedTransactions transactions = context.state;
			final Account sender = context.addAccount(Amount.fromNem(500), Utils.createMosaicId(1), Supply.fromValue(10));
			final Account recipient = context.addAccount();
			final TimeInstant currentTime = new TimeInstant(11);

			// Act:
			final Transaction t1 = createTransferTransaction(
					2,
					currentTime,
					sender,
					recipient,
					Amount.fromNem(1),
					createAttachment(Utils.createMosaicId(1), Quantity.fromValue(7_000)));
			t1.setFee(Amount.fromNem(200));
			t1.sign();
			final ValidationResult result1 = transactions.addExisting(t1);
			final Transaction t2 = createTransferTransaction(
					2,
					currentTime.addSeconds(-1),
					sender,
					recipient,
					Amount.fromNem(1),
					createAttachment(Utils.createMosaicId(1), Quantity.fromValue(7_000)));
			t2.setFee(Amount.fromNem(200));
			t2.sign();
			final ValidationResult result2 = transactions.addExisting(t2);

			// Assert:
			Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
			Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
			Assert.assertThat(transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(t1)));
		}

		//endregion

		//region validation context heights

		@Test
		public void singleValidationContextHeightsAreSetCorrectly() {
			// Arrange:
			final TestContext2 context = new TestContext2();
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
			final TestContext2 context = new TestContext2();
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
	}

	private static void assertCapturedValidationContext(final ValidationContext context) {
		// Assert:
		Assert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT + 1)));
		Assert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT)));
	}

	private static TestContext2 createUnconfirmedTransactionsWithRealValidator(final int height) {
		return createUnconfirmedTransactionsWithRealValidator(Mockito.mock(AccountStateCache.class), height);
	}

	private static TestContext2 createUnconfirmedTransactionsWithRealValidator(final AccountStateCache stateCache, final int height) {
		final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory(new SystemTimeProvider());
		return new TestContext2(
				factory::createSingleBuilder,
				null,
				factory.createBatch(Mockito.mock(DefaultHashCache.class)),
				stateCache,
				Mockito.mock(ReadOnlyPoiFacade.class),
				Mockito.mock(ReadOnlyNamespaceCache.class),
				height);
	}

	private static BalanceValidator createBalanceValidator() {
		return new BalanceValidator();
	}

	private static MosaicBalanceValidator createMosaicBalanceValidator() {
		return new MosaicBalanceValidator();
	}

	public static TransferTransaction createTransferTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount) {
		return createTransferTransaction(1, timeStamp, sender, recipient, amount, null);
	}

	public static TransferTransaction createTransferTransaction(
			final int version,
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount,
			final TransferTransactionAttachment attachment) {
		final TransferTransaction transferTransaction = new TransferTransaction(version, timeStamp, sender, recipient, amount, attachment);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	private static TransferTransactionAttachment createAttachment(final MosaicId mosaicId, final Quantity quantity) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, quantity);
		return attachment;
	}

	private static class TestContext {
		private final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();
		private final UnconfirmedTransactionsCache transactions = Mockito.mock(UnconfirmedTransactionsCache.class);
		private final UnconfirmedBalancesObserver unconfirmedBalances = new UnconfirmedBalancesObserver(this.nisCache.getAccountStateCache());
		private final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances = new UnconfirmedMosaicBalancesObserver(this.nisCache.getNamespaceCache());
		private final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
		private final TransactionObserver transferObserver = Mockito.spy(this.createRealUnconfirmedObservers());
		private final TransactionSpamFilter spamFilter = Mockito.mock(TransactionSpamFilter.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final Supplier<BlockHeight> blockHeightSupplier = () -> new BlockHeight(1235);
		private final SingleTransactionValidator singleValidator = Mockito.mock(SingleTransactionValidator.class);
		private final BatchTransactionValidator batchValidator = Mockito.mock(BatchTransactionValidator.class);
		private final DefaultUnconfirmedState state;

		public TestContext() {
			// by default, have all mocks succeed and not flag any validation errors
			Mockito.when(this.transactions.add(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
			Mockito.when(this.transactions.contains(Mockito.any())).thenReturn(false);

			Mockito.when(this.spamFilter.filter(Mockito.any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

			this.setSingleValidationResult(ValidationResult.SUCCESS);
			this.setBatchValidationResult(ValidationResult.SUCCESS);

			final AggregateSingleTransactionValidatorBuilder singleValidatorBuilder = new AggregateSingleTransactionValidatorBuilder();
			singleValidatorBuilder.add(this.singleValidator);
			Mockito.when(this.validatorFactory.createIncompleteSingleBuilder(Mockito.any())).thenReturn(singleValidatorBuilder);
			Mockito.when(this.validatorFactory.createBatch(Mockito.any())).thenReturn(this.batchValidator);

			this.state = new DefaultUnconfirmedState(
					this.transactions,
					this.unconfirmedBalances,
					this.unconfirmedMosaicBalances,
					this.validatorFactory,
					this.transferObserver,
					this.spamFilter,
					this.nisCache,
					this.timeProvider,
					this.blockHeightSupplier);
		}

		private TransactionObserver createRealUnconfirmedObservers() {
			final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();
			builder.add(this.unconfirmedBalances);
			builder.add(this.unconfirmedMosaicBalances);
			return builder.build();
		}

		public Account addAccount(final Amount amount) {
			final Account account = Utils.generateRandomAccount();
			this.modifyCache(copyCache -> copyCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo().incrementBalance(amount));
			return account;
		}

		public void addMosaic(final Account account, final MosaicId mosaicId, final Supply supply) {
			this.modifyCache(copyCache -> {
				copyCache.getNamespaceCache().add(new Namespace(mosaicId.getNamespaceId(), account, new BlockHeight(11)));
				final MosaicEntry entry = copyCache.getNamespaceCache().get(mosaicId.getNamespaceId())
						.getMosaics()
						.add(Utils.createMosaicDefinition(account, mosaicId, Utils.createMosaicProperties()));
				entry.increaseSupply(supply);
			});
		}

		private void modifyCache(final Consumer<NisCache> modify) {
			final NisCache copyCache = this.nisCache.copy();
			modify.accept(copyCache);
			copyCache.commit();
		}

		public void setAddResult(final ValidationResult result1, final ValidationResult... result2) {
			Mockito.when(this.transactions.add(Mockito.any())).thenReturn(result1, result2);
		}

		public void setSingleValidationResult(final ValidationResult result1, final ValidationResult... result2) {
			Mockito.when(this.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(result1, result2);
		}

		public void setBatchValidationResult(final ValidationResult result) {
			Mockito.when(this.batchValidator.validate(Mockito.any())).thenReturn(result);
		}

		public void setSpamFilterResult(final Collection<Transaction> transactions) {
			Mockito.when(this.spamFilter.filter(Mockito.any())).thenReturn(transactions);
		}

		public void assertTransactionsAdded(final Collection<Transaction> transactions) {
			Mockito.verify(this.transactions, Mockito.times(transactions.size())).add(Mockito.any());
			transactions.forEach(t -> Mockito.verify(this.transactions, Mockito.times(1)).add(t));
		}

		public void assertTransactionAdded(final Transaction transaction) {
			this.assertTransactionsAdded(Collections.singletonList(transaction));
		}

		public void assertNoTransactionsAdded() {
			Mockito.verify(this.transactions, Mockito.never()).add(Mockito.any());
		}

		public void assertNumDelegations(final int numSpamFilters, final int numBatchValidations, final int numSingleValidations) {
			Mockito.verify(this.spamFilter, Mockito.times(numSpamFilters)).filter(Mockito.any());
			Mockito.verify(this.batchValidator, Mockito.times(numBatchValidations)).validate(Mockito.any());
			Mockito.verify(this.singleValidator, Mockito.times(numSingleValidations)).validate(Mockito.any(), Mockito.any());
		}
	}

	private static class TestContext2 {
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions state;
		private final ReadOnlyNisCache nisCache;
		private final TimeProvider timeProvider;
		private final Map<MosaicId, MosaicEntry> mosaicMap = new HashMap<>();

		private TestContext2() {
			this(Mockito.mock(SingleTransactionValidator.class), Mockito.mock(BatchTransactionValidator.class));
			this.setSingleValidationResult(ValidationResult.SUCCESS);
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext2(final SingleTransactionValidator singleValidator) {
			this(singleValidator, Mockito.mock(BatchTransactionValidator.class));
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext2(final SingleTransactionValidator singleValidator, final BatchTransactionValidator batchValidator) {
			this(
					null,
					singleValidator,
					batchValidator,
					Mockito.mock(ReadOnlyAccountStateCache.class),
					Mockito.mock(ReadOnlyPoiFacade.class),
					Mockito.mock(ReadOnlyNamespaceCache.class),
					CONFIRMED_BLOCK_HEIGHT);
		}

		private TestContext2(
				final Function<ReadOnlyNisCache, AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier,
				final SingleTransactionValidator singleValidator,
				final BatchTransactionValidator batchValidator,
				final ReadOnlyAccountStateCache accountStateCache,
				final ReadOnlyPoiFacade poiFacade,
				final ReadOnlyNamespaceCache namespaceCache,
				final int validationHeight) {
			this.singleValidator = singleValidator;
			this.batchValidator = batchValidator;
			this.timeProvider = Mockito.mock(TimeProvider.class);

			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);

			this.nisCache = NisCacheFactory.createReadOnly(accountStateCache, transactionHashCache, poiFacade, namespaceCache);

			if (null != singleTransactionBuilderSupplier) {
				this.setSingleTransactionBuilderSupplier(validatorFactory, singleTransactionBuilderSupplier);
			} else {
				this.setSingleTransactionBuilderSupplier(validatorFactory, nisCache -> {
					final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
					builder.add(this.singleValidator);
					return builder;
				});
			}

			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(TimeInstant.ZERO);
			this.state = new DefaultUnconfirmedTransactions(
					validatorFactory,
					this.nisCache,
					this.timeProvider,
					() -> new BlockHeight(validationHeight));
		}

		private void setSingleTransactionBuilderSupplier(
				final TransactionValidatorFactory validatorFactory,
				final Function<ReadOnlyNisCache, AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier) {
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.apply(this.nisCache));
			Mockito.when(validatorFactory.createIncompleteSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.apply(this.nisCache));
		}

		private void setSingleValidationResult(final ValidationResult result) {
			Mockito.when(this.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		}

		private void setBatchValidationResult(final ValidationResult result) {
			Mockito.when(this.batchValidator.validate(Mockito.any())).thenReturn(result);
		}

		private ValidationResult signAndAddExisting(final Transaction transaction) {
			transaction.sign();
			return this.state.addExisting(transaction);
		}

		private ValidationResult signAndAddNew(final Transaction transaction) {
			transaction.sign();
			return this.state.addNew(transaction);
		}

		private ValidationResult signAndAddNewBatch(final Collection<Transaction> transactions) {
			transactions.forEach(Transaction::sign);
			return this.state.addNewBatch(transactions);
		}

		private Account addAccount() {
			return this.addAccount(Amount.ZERO);
		}

		private Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		private Account addAccount(final Amount amount, final MosaicId mosaicId, final Supply supply) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount, mosaicId, supply);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

		public Account prepareAccount(final Account account, final Amount amount, final MosaicId mosaicId, final Supply supply) {
			this.prepareAccount(account, amount);
			final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
			final Mosaics mosaics = Mockito.mock(Mosaics.class);
			final MosaicEntry mosaicEntry = new MosaicEntry(Utils.createMosaicDefinition(account, mosaicId, Utils.createMosaicProperties()), supply);
			this.mosaicMap.put(mosaicId, mosaicEntry);
			Mockito.when(this.nisCache.getNamespaceCache().isActive(Mockito.any(), Mockito.any())).thenReturn(true);
			Mockito.when(this.nisCache.getNamespaceCache().get(mosaicId.getNamespaceId())).thenReturn(namespaceEntry);
			Mockito.when(namespaceEntry.getMosaics()).thenReturn(mosaics);
			Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);
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
			return this.createTransferTransaction(sender, recipient, amount, deadline, null);
		}

		public TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TimeInstant deadline,
				final TransferTransactionAttachment attachment) {
			final TransferTransaction transaction = new TransferTransaction(deadline, sender, recipient, amount, attachment);
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

		public List<TransferTransaction> createThreeMosaicTransferTransactions(final int supply1, final int supply2) {
			final MosaicId[] mosaicIds = new MosaicId[] { Utils.createMosaicId(1), Utils.createMosaicId(2) };
			final Account account1 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100), mosaicIds[0], Supply.fromValue(supply1));
			final Account account2 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100), mosaicIds[1], Supply.fromValue(supply2));
			final Account account3 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100));
			final List<TransferTransaction> transactions = new ArrayList<>();
			transactions.add(this.createTransferTransaction(
					account1,
					account2,
					Amount.fromNem(1),
					new TimeInstant(5),
					createAttachment(mosaicIds[0], Quantity.fromValue(80_000))));
			transactions.add(this.createTransferTransaction(
					account2,
					account3,
					Amount.fromNem(1),
					new TimeInstant(8),
					createAttachment(mosaicIds[1], Quantity.fromValue(50_000))));
			transactions.add(this.createTransferTransaction(
					account2,
					account3,
					Amount.fromNem(1),
					new TimeInstant(9),
					createAttachment(mosaicIds[1], Quantity.fromValue(10_000))));
			transactions.forEach(this::signAndAddExisting);
			return transactions;
		}

		private void decreaseSupply(final MosaicId mosaicId, final Supply supply) {
			final MosaicEntry mosaicEntry = this.mosaicMap.get(mosaicId);
			mosaicEntry.decreaseSupply(supply);
		}
	}
}
