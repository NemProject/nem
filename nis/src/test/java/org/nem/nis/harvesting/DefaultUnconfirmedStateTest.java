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

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class DefaultUnconfirmedStateTest {
	private static final int CONFIRMED_BLOCK_HEIGHT = 3452;
	private static final int CURRENT_TIME = 10_000;

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

	private abstract static class AbstractAddTest {

		protected abstract ValidationResult add(final UnconfirmedState state, final Transaction transaction);

		//region cache add success

		@Test
		public void addSucceedsIfAllValidationsSucceed() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertTransactionAdded(transaction);
		}

		@Test
		public void addSucceedsForMultipleTransactions() {
			// Arrange:
			final TestContext context = new TestContext();
			final Collection<Transaction> transactions = createMockTransactions(context, 7, 9);

			// Act:
			final Collection<ValidationResult> results = transactions.stream().map(t -> this.add(context.state, t)).collect(Collectors.toList());

			// Assert:
			results.forEach(result -> Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS)));
			context.assertTransactionsAdded(transactions);
		}

		@Test
		public void addExecutesTransactionIfCacheAddSucceeds() {
			// Arrange:
			final TestContext context = new TestContext();
			final MockTransaction transaction = Mockito.spy(createMockTransaction(context, 7));

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			Mockito.verify(transaction, Mockito.times(1)).execute(context.transferObserver);
			Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		}

		//endregion

		//region cache add failure

		@Test
		public void addFailsIfCacheAddFails() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setAddResult(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
			context.assertTransactionAdded(transaction);
		}

		@Test
		public void addDoesNotExecuteTransactionIfCacheAddFails() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setAddResult(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
			final MockTransaction transaction = Mockito.spy(createMockTransaction(context, 7));

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
			Mockito.verify(transaction, Mockito.never()).execute(context.transferObserver);
			Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
		}

		//endregion

		//region verify / single validation

		@Test
		public void addFailsIfTransactionDoesNotVerify() {
			// Arrange: ruin signature by altering the deadline
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			transaction.setDeadline(transaction.getDeadline().addMinutes(1));

			// Act:
			final ValidationResult result = context.state.addExisting(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
			context.assertNoTransactionsAdded();
		}

		@Test
		public void addFailsIfTransactionValidationReturnsNeutral() {
			// Assert:
			this.assertSingleValidationFailure(ValidationResult.NEUTRAL);
		}

		@Test
		public void addFailsIfTransactionValidationFails() {
			// Assert:
			this.assertSingleValidationFailure(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
		}

		private void assertSingleValidationFailure(final ValidationResult validationResult) {
			// Arrange:
			final TestContext context = new TestContext();
			context.setSingleValidationResult(validationResult);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(validationResult));
			context.assertNoTransactionsAdded();
		}

		//endregion

		//region insufficient balance

		@Test
		public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
			// Arrange:
			final TestContext context = new TestContext(new BalanceValidator());
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			final List<Transaction> transactions = Arrays.asList(
					createTransfer(account1, account2, 5, 1),
					createTransfer(account1, account2, 8, 2));
			this.add(context.state, transactions.get(0));

			// Act:
			final ValidationResult result = this.add(context.state, transactions.get(1));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
			context.assertTransactionAdded(transactions.get(0));
		}

		@Test
		public void addFailsIfSenderHasInsufficientUnconfirmedMosaicBalance() {
			// Arrange:
			final TestContext context = new TestContext(new MosaicBalanceValidator());
			final MosaicId mosaicId1 = Utils.createMosaicId(1);
			final MosaicId mosaicId2 = Utils.createMosaicId(2);
			final Account account1 = context.addAccount(Amount.fromNem(500));
			final Account account2 = context.addAccount(Amount.fromNem(500));
			context.addMosaic(account1, mosaicId1, Supply.fromValue(12));
			context.addMosaic(account2, mosaicId2, Supply.fromValue(21));

			final List<Transaction> transactions = Arrays.asList(
					createTransferWithMosaicTransfer(account1, account2, 1, 10, mosaicId1, 5_000),
					createTransferWithMosaicTransfer(account1, account2, 1, 10, mosaicId1, 8_000));
			this.add(context.state, transactions.get(0));


			// Act:
			final ValidationResult result = this.add(context.state, transactions.get(1));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
			context.assertTransactionAdded(transactions.get(0));
		}

		//endregion

		//region expiry

		@Test
		public void addFailsIfTransactionHasExpired() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			transaction.setDeadline(new TimeInstant(CURRENT_TIME - 10));
			transaction.sign();

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
			context.assertNoTransactionsAdded();
		}

		//endregion

		protected void assertResultWhenCacheContainsTransaction(final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.transactions.contains(Mockito.any())).thenReturn(true);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			assertTransactionAddition(context, expectedResult, transaction);
		}

		protected void assertResultWhenBatchValidationFails(final ValidationResult batchResult, final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext();
			context.setBatchValidationResult(batchResult);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			assertTransactionAddition(context, expectedResult, transaction);
		}

		protected void assertResultWhenSpamFilterBlocksTransasction(final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			context.setSpamFilterResult(Collections.emptyList());

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
			assertTransactionAddition(context, expectedResult, transaction);
		}

		private static void assertTransactionAddition(
				final TestContext context,
				final ValidationResult expectedResult,
				final Transaction transaction) {
			if (expectedResult.isSuccess()) {
				context.assertTransactionAdded(transaction);
			} else {
				context.assertNoTransactionsAdded();
			}
		}
	}

	//region AddExistingTest

	public static class AddExistingTest extends AbstractAddTest {

		@Override
		protected ValidationResult add(final UnconfirmedState state, final Transaction transaction) {
			return state.addExisting(transaction);
		}

		@Test
		public void addSucceedsWhenCacheContainsTransaction() {
			this.assertResultWhenCacheContainsTransaction(ValidationResult.SUCCESS);
		}

		@Test
		public void addSucceedsWhenBatchValidationFails() {
			this.assertResultWhenBatchValidationFails(ValidationResult.FAILURE_FUTURE_DEADLINE, ValidationResult.SUCCESS);
		}

		@Test
		public void addSucceedsWhenSpamFilterBlocksTransaction() {
			this.assertResultWhenSpamFilterBlocksTransasction(ValidationResult.SUCCESS);
		}
	}

	//endregion

	//region AbstractAddNewTest / AddNewTest

	private abstract static class AbstractAddNewTest extends AbstractAddTest {

		@Test
		public void addFailsWhenBatchValidationFails() {
			this.assertResultWhenBatchValidationFails(ValidationResult.FAILURE_FUTURE_DEADLINE, ValidationResult.FAILURE_FUTURE_DEADLINE);
		}

		@Test
		public void addFailsWhenSpamFilterBlocksTransaction() {
			this.assertResultWhenSpamFilterBlocksTransasction(ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL);
		}
	}

	public static class AddNewTest extends AbstractAddNewTest {

		@Override
		protected ValidationResult add(final UnconfirmedState state, final Transaction transaction) {
			return state.addNew(transaction);
		}

		@Test
		public void addIsNeutralWhenCacheContainsTransaction() {
			this.assertResultWhenCacheContainsTransaction(ValidationResult.NEUTRAL);
		}
	}

	//endregion

	//region AddNewBatchTest

	public static class AddNewBatchTest extends AbstractAddNewTest {

		@Override
		protected ValidationResult add(final UnconfirmedState state, final Transaction transaction) {
			return state.addNewBatch(Collections.singletonList(transaction));
		}

		@Test
		public void addSucceedsWhenCacheContainsTransaction() {
			this.assertResultWhenCacheContainsTransaction(ValidationResult.SUCCESS);
		}

		@Test
		public void addNewBatchReturnsSuccessIfAllTransactionsCanBeSuccessfullyAdded() {
			// Arrange:
			final TestContext context = new TestContext();
			final Collection<Transaction> transactions = createMockTransactions(context, 3, 5);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
			context.assertTransactionsAdded(filteredTransactions);
		}
	}

	//endregion

	//region create transaction helpers

	private static MockTransaction createMockTransaction(final TestContext context, final int customField) {
		final Account account = context.addAccount(Amount.fromNem(1_000));
		return prepare(new MockTransaction(account, customField, new TimeInstant(customField)));
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
		return prepare(t);
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
		return prepare(t);
	}

	private static Transaction createImportanceTransfer(final Account sender, final Account remote, final int fee) {
		final Transaction t = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferMode.Activate, remote);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	private static <T extends Transaction> T prepare(final T transaction) {
		transaction.setDeadline(new TimeInstant(CURRENT_TIME + 10));
		transaction.sign();
		return transaction;
	}

	//endregion

	public static class Legacy {

		//region add[Batch/New/Existing]

		//region validation

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
		private final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME);
		private final Supplier<BlockHeight> blockHeightSupplier = () -> new BlockHeight(1235);
		private final SingleTransactionValidator singleValidator = Mockito.mock(SingleTransactionValidator.class);
		private final BatchTransactionValidator batchValidator = Mockito.mock(BatchTransactionValidator.class);
		private final DefaultUnconfirmedState state;

		public TestContext(final SingleTransactionValidator... additionalValidators) {
			// by default, have all mocks succeed and not flag any validation errors
			Mockito.when(this.transactions.add(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
			Mockito.when(this.transactions.contains(Mockito.any())).thenReturn(false);

			Mockito.when(this.spamFilter.filter(Mockito.any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

			this.setSingleValidationResult(ValidationResult.SUCCESS);
			this.setBatchValidationResult(ValidationResult.SUCCESS);

			final AggregateSingleTransactionValidatorBuilder singleValidatorBuilder = new AggregateSingleTransactionValidatorBuilder();
			singleValidatorBuilder.add(this.singleValidator);
			for (final SingleTransactionValidator validator : additionalValidators) {
				singleValidatorBuilder.add(validator);
			}

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
