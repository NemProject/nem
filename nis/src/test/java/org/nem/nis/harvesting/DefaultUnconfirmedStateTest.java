package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.NisCache;
import org.nem.nis.secret.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.*;

@RunWith(Enclosed.class)
public class DefaultUnconfirmedStateTest {
	private static final int CONFIRMED_BLOCK_HEIGHT = 3452;
	private static final int CURRENT_TIME = UnconfirmedTransactionsTestUtils.CURRENT_TIME;

	// region getUnconfirmedBalance

	public static class UnconfirmedBalanceTest {

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceWhenNoPendingTransactionsImpactAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account account1 = context.addAccount(Amount.fromNem(5));
			final Account account2 = context.addAccount(Amount.fromNem(100));

			// Assert:
			MatcherAssert.assertThat(context.state.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(5)));
			MatcherAssert.assertThat(context.state.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(100)));
		}

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingTransactionsImpactingAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			final List<Transaction> transactions = Arrays.asList(createTransfer(account2, account1, 15, 2),
					createTransfer(account1, account2, 14, 3));
			context.addAll(transactions);

			// Assert:
			MatcherAssert.assertThat(context.state.getUnconfirmedBalance(account1), IsEqual.equalTo(Amount.fromNem(12)));
			MatcherAssert.assertThat(context.state.getUnconfirmedBalance(account2), IsEqual.equalTo(Amount.fromNem(107)));
		}

		@Test
		public void getUnconfirmedBalanceReturnsConfirmedBalanceAdjustedByAllPendingImportanceTransactionsImpactingAccount() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account sender = context.addAccount(Amount.fromNem(500000));
			final Account remote = context.addAccount(Amount.ZERO);
			final Transaction t1 = createImportanceTransfer(sender, remote, 10);
			context.add(t1);

			// Assert:
			MatcherAssert.assertThat(context.state.getUnconfirmedBalance(sender), IsEqual.equalTo(Amount.fromNem(499990)));
		}
	}

	// endregion

	// region getUnconfirmedMosaicBalance

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
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId1),
					IsEqual.equalTo(Quantity.fromValue(12_000)));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId2), IsEqual.equalTo(Quantity.ZERO));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId1), IsEqual.equalTo(Quantity.ZERO));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId2),
					IsEqual.equalTo(Quantity.fromValue(21_000)));
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
			context.addAll(transactions);

			// Assert:
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId1),
					IsEqual.equalTo(Quantity.fromValue(9_000)));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account1, mosaicId2),
					IsEqual.equalTo(Quantity.fromValue(5_000)));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId2),
					IsEqual.equalTo(Quantity.fromValue(16_000)));
			MatcherAssert.assertThat(context.state.getUnconfirmedMosaicBalance(account2, mosaicId1),
					IsEqual.equalTo(Quantity.fromValue(3_000)));
		}
	}

	// endregion

	private abstract static class AbstractAddTest {

		protected abstract ValidationResult add(final UnconfirmedState state, final Transaction transaction);

		// region cache add success

		@Test
		public void addSucceedsIfAllValidationsSucceed() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertTransactionAdded(transaction);
		}

		@Test
		public void addSucceedsIfTreasuryReissuanceTransactionDoesNotVerify() {
			// Arrange: ruin signature by altering the deadline
			final Account senderAccount = Utils.generateRandomAccount();
			final Transaction transaction = prepare(new MockTransaction(senderAccount, 7, new TimeInstant(CURRENT_TIME + 7)));
			transaction.setDeadline(transaction.getDeadline().addMinutes(1));

			// - include its hash in allowed list
			final ArrayList<Hash> hashes = new ArrayList<Hash>();
			hashes.add(Utils.generateRandomHash());
			hashes.add(HashUtils.calculateHash(transaction));
			hashes.add(Utils.generateRandomHash());

			// - create test context and add account
			final TestContext context = new TestContext(new ForkConfiguration(new BlockHeight(1234), hashes, new ArrayList<Hash>()));
			context.prepareAccount(senderAccount, Amount.fromNem(1_000));

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertTransactionAdded(transaction);
		}

		@Test
		public void addExecutesTransactionIfCacheAddSucceeds() {
			// Arrange:
			final TestContext context = new TestContext();
			final MockTransaction transaction = Mockito.spy(createMockTransaction(context, 7));
			context.setHeightAndTime(CONFIRMED_BLOCK_HEIGHT + 10, CURRENT_TIME + 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert: the notification context should use the current (not creation) information
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			Mockito.verify(transaction, Mockito.times(1)).execute(Mockito.any(), Mockito.anyObject());
			Mockito.verify(context.blockTransferObserver, Mockito.only()).notify(Mockito.any(), Mockito.any());
			MatcherAssert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
			context.assertNotificationContext(CONFIRMED_BLOCK_HEIGHT + 10, CURRENT_TIME + 7);
		}

		@Test
		public void addSucceedsForMultipleTransactions() {
			// Arrange:
			final TestContext context = new TestContext();
			final Collection<Transaction> transactions = createMockTransactions(context, 7, 9);

			// Act:
			final Collection<ValidationResult> results = transactions.stream().map(t -> this.add(context.state, t))
					.collect(Collectors.toList());

			// Assert:
			results.forEach(result -> MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS)));
			context.assertTransactionsAdded(transactions);
		}

		// endregion

		// region cache add failure

		@Test
		public void addFailsIfCacheAddFails() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setAddResult(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
			Mockito.verify(transaction, Mockito.never()).execute(Mockito.any(), Mockito.anyObject());
			Mockito.verify(context.blockTransferObserver, Mockito.never()).notify(Mockito.any(), Mockito.any());
			MatcherAssert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(0));
		}

		// endregion

		// region verify / single validation

		@Test
		public void addFailsIfTransactionDoesNotVerify() {
			// Arrange: ruin signature by altering the deadline
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			transaction.setDeadline(transaction.getDeadline().addMinutes(1));

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(validationResult));
			context.assertNoTransactionsAdded();
		}

		@Test
		public void addSucceedsIfSingleValidationSucceedsAfterFailing() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setSingleValidationResult(ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT);
			final MockTransaction transaction = createMockTransaction(context, 7);

			final ValidationResult result1 = this.add(context.state, transaction);
			context.setSingleValidationResult(ValidationResult.SUCCESS);

			// Act:
			final ValidationResult result2 = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result1, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_CREATOR_CONFLICT));
			MatcherAssert.assertThat(result2, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertTransactionAdded(transaction);
		}

		// endregion

		// region insufficient balance

		@Test
		public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
			// Arrange:
			final TestContext context = new TestContext(new BalanceValidator());
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			final List<Transaction> transactions = Arrays.asList(createTransfer(account1, account2, 5, 1),
					createTransfer(account1, account2, 8, 2));
			this.add(context.state, transactions.get(0));

			// Act:
			final ValidationResult result = this.add(context.state, transactions.get(1));

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
			context.assertTransactionAdded(transactions.get(0));
		}

		// endregion

		// region expiry

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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_PAST_DEADLINE));
			context.assertNoTransactionsAdded();
		}

		// endregion

		// region validator delegation

		@Test
		public void addDelegatesToSingleValidator() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			final ArgumentCaptor<ValidationContext> validationContextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
			Mockito.verify(context.singleValidator, Mockito.only()).validate(Mockito.eq(transaction), validationContextCaptor.capture());
			assertCapturedValidationContext(validationContextCaptor.getValue());
		}

		protected static void assertCapturedValidationContext(final ValidationContext context) {
			// Assert:
			MatcherAssert.assertThat(context.getBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT + 1)));
			MatcherAssert.assertThat(context.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(CONFIRMED_BLOCK_HEIGHT)));
		}

		// endregion

		protected void assertResultWhenCacheContainsTransaction(final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext();
			Mockito.when(context.transactions.contains(Mockito.any())).thenReturn(true);
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
			assertTransactionAddition(context, expectedResult, transaction);
		}

		protected void assertResultWhenSpamFilterBlocksTransaction(final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);
			context.setSpamFilterResult(Collections.emptyList());

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
			assertTransactionAddition(context, expectedResult, transaction);

			if (expectedResult.isSuccess()) {
				Mockito.verify(context.spamFilter, Mockito.never()).filter(Mockito.any());
			} else {
				Mockito.verify(context.spamFilter, Mockito.only()).filter(Collections.singletonList(transaction));
			}
		}

		private static void assertTransactionAddition(final TestContext context, final ValidationResult expectedResult,
				final Transaction transaction) {
			if (expectedResult.isSuccess()) {
				context.assertTransactionAdded(transaction);
			} else {
				context.assertNoTransactionsAdded();
			}
		}
	}

	// region AddExistingTest

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
			this.assertResultWhenSpamFilterBlocksTransaction(ValidationResult.SUCCESS);
		}
	}

	// endregion

	// region AbstractAddNewTest / AddNewTest

	private abstract static class AbstractAddNewTest extends AbstractAddTest {

		@Test
		public void addFailsWhenBatchValidationFails() {
			this.assertResultWhenBatchValidationFails(ValidationResult.FAILURE_FUTURE_DEADLINE, ValidationResult.FAILURE_FUTURE_DEADLINE);
		}

		@Test
		public void addFailsWhenSpamFilterBlocksTransaction() {
			this.assertResultWhenSpamFilterBlocksTransaction(ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL);
		}

		@Test
		public void addDelegatesToBatchValidator() {
			// Arrange:
			final TestContext context = new TestContext();
			final Transaction transaction = createMockTransaction(context, 7);

			// Act:
			final ValidationResult result = this.add(context.state, transaction);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			final ArgumentCaptor<List<TransactionsContextPair>> pairsCaptor = createPairsCaptor();
			Mockito.verify(context.batchValidator, Mockito.only()).validate(pairsCaptor.capture());

			final TransactionsContextPair pair = pairsCaptor.getValue().get(0);
			MatcherAssert.assertThat(pair.getTransactions(), IsEqual.equalTo(Collections.singletonList(transaction)));
			assertCapturedValidationContext(pair.getContext());
		}

		@SuppressWarnings({
				"unchecked", "rawtypes"
		})
		private static ArgumentCaptor<List<TransactionsContextPair>> createPairsCaptor() {
			return ArgumentCaptor.forClass((Class) List.class);
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

	// endregion

	// region AddNewBatchTest

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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			Mockito.verify(context.spamFilter, Mockito.only()).filter(transactions);
			context.assertTransactionsAdded(transactions);
		}

		@Test
		public void addNewBatchDoesNotShortCircuitButReturnsFirstFailureIfAnyTransactionFailsSingleValidation() {
			// Arrange:
			final TestContext context = new TestContext();
			context.setAddResult(ValidationResult.SUCCESS, ValidationResult.FAILURE_FUTURE_DEADLINE, ValidationResult.SUCCESS,
					ValidationResult.FAILURE_UNKNOWN);
			final List<Transaction> transactions = createMockTransactions(context, 3, 5);

			// Act:
			final ValidationResult result = context.state.addNewBatch(transactions);

			// Assert: all successfully validated state were added and the first failure was returned
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MESSAGE_TOO_LARGE));
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
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
			context.assertTransactionsAdded(filteredTransactions);
		}
	}

	// endregion

	private static class TestContext implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTestContext {
		private final NisCache nisCache = NisCacheFactory.createReal().copy();
		private final UnconfirmedTransactionsCache transactions = Mockito.mock(UnconfirmedTransactionsCache.class);
		private final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
		private final BlockTransactionObserver blockTransferObserver = Mockito.spy(this.createRealUnconfirmedObservers());
		private final TransactionSpamFilter spamFilter = Mockito.mock(TransactionSpamFilter.class);
		private final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME);
		private final Supplier<BlockHeight> blockHeightSupplier = () -> new BlockHeight(this.blockHeight);
		private final SingleTransactionValidator singleValidator = Mockito.mock(SingleTransactionValidator.class);
		private final BatchTransactionValidator batchValidator = Mockito.mock(BatchTransactionValidator.class);
		private final DefaultUnconfirmedState state;

		private BlockNotificationContext lastNotificationContext;
		private long blockHeight = CONFIRMED_BLOCK_HEIGHT;

		public TestContext(final SingleTransactionValidator... additionalValidators) {
			this(new ForkConfiguration(), additionalValidators);
		}

		public TestContext(final ForkConfiguration forkConfiguration, final SingleTransactionValidator... additionalValidators) {
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

			this.state = new DefaultUnconfirmedState(this.transactions, this.validatorFactory, (notification, context) -> {
				this.lastNotificationContext = context;
				this.blockTransferObserver.notify(notification, context);
			}, this.spamFilter, this.nisCache, this.timeProvider, this.blockHeightSupplier, forkConfiguration);
		}

		public void add(final Transaction transaction) {
			this.state.addNew(transaction);
		}

		public void addAll(final Collection<Transaction> transactions) {
			transactions.forEach(this.state::addNew);
		}

		private BlockTransactionObserver createRealUnconfirmedObservers() {
			final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
			builder.add(new BalanceCommitTransferObserver(this.nisCache.getAccountStateCache()));
			builder.add(new MosaicTransferObserver(this.nisCache.getNamespaceCache()));
			return builder.build();
		}

		@Override
		public Account addAccount(final Amount amount) {
			final Account account = Utils.generateRandomAccount();
			this.prepareAccount(account, amount);
			return account;
		}

		public void prepareAccount(final Account account, final Amount amount) {
			this.modifyCache(copyCache -> copyCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo()
					.incrementBalance(amount));
		}

		public void addMosaic(final Account account, final MosaicId mosaicId, final Supply supply) {
			this.modifyCache(copyCache -> {
				copyCache.getNamespaceCache().add(new Namespace(mosaicId.getNamespaceId(), account, new BlockHeight(11)));
				final MosaicEntry entry = copyCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics()
						.add(Utils.createMosaicDefinition(account, mosaicId, Utils.createMosaicProperties()));
				entry.increaseSupply(supply);
			});
		}

		private void modifyCache(final Consumer<NisCache> modify) {
			modify.accept(this.nisCache);
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

		public void setHeightAndTime(final long blockHeight, final int time) {
			this.blockHeight = blockHeight;
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(time));
		}

		public void assertNotificationContext(final long blockHeight, final int time) {
			MatcherAssert.assertThat(this.lastNotificationContext, IsNull.notNullValue());
			MatcherAssert.assertThat(this.lastNotificationContext.getHeight(), IsEqual.equalTo(new BlockHeight(blockHeight)));
			MatcherAssert.assertThat(this.lastNotificationContext.getTimeStamp(), IsEqual.equalTo(new TimeInstant(time)));
			MatcherAssert.assertThat(this.lastNotificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Execute));
		}
	}
}
