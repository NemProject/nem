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

import java.util.*;
import java.util.function.*;

@RunWith(Enclosed.class)
public class DefaultUnconfirmedStateTest {
	private static final int CONFIRMED_BLOCK_HEIGHT = 3452;

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
			Mockito.when(this.nisCache.getNamespaceCache().isActive(Mockito.any(), Mockito.any())).thenReturn(true);
			Mockito.when(this.nisCache.getNamespaceCache().get(mosaicId.getNamespaceId())).thenReturn(namespaceEntry);
			Mockito.when(namespaceEntry.getMosaics()).thenReturn(mosaics);
			Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);
			return account;
		}
	}
}
