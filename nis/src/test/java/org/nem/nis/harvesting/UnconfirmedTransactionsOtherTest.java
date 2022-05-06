package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.ReadOnlyAccountInfo;
import org.nem.nis.test.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.function.*;

import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.*;

public abstract class UnconfirmedTransactionsOtherTest implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTest {
	private static final int CURRENT_TIME = UnconfirmedTransactionsTestUtils.CURRENT_TIME;

	// region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(1000));

		// Act:
		for (int i = 0; i < 17; ++i) {
			context.add(prepare(new MockTransaction(account, i, new TimeInstant(CURRENT_TIME + i % 7))));
		}

		// Assert:
		MatcherAssert.assertThat(context.transactions.size(), IsEqual.equalTo(17));
	}

	// endregion

	// region multilevel existence checks

	@Test
	public void cannotAddChildTransactionIfParentHasBeenAdded() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = createMockTransaction(sender, 7);
		final MockTransaction outer = createMockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));
		context.add(prepare(outer));

		// Act
		final ValidationResult result = context.add(prepare(inner));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void cannotAddParentTransactionIfChildHasBeenAdded() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = createMockTransaction(sender, 7);
		final MockTransaction outer = createMockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));
		context.add(inner);

		// Act
		final ValidationResult result = context.add(outer);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	// endregion

	// region validation (unconfirmed conflicts)

	@Test
	public void addFailsIfImportanceTransferIsConflicting() {
		// Assert:
		this.assertLastTransactionCannotBeAdded(context -> {
			final Account sender = context.addAccount(Amount.fromNem(50000));
			final Account remote = Utils.generateRandomAccount();
			return Arrays.asList(createImportanceTransfer(sender, remote, 1000), createImportanceTransfer(sender, remote, 2000));
		}, ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Assert:
		this.assertLastTransactionCannotBeAdded(context -> {
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			return Arrays.asList(createTransfer(account1, account2, 5, 5), createTransfer(account1, account2, 8, 2));
		}, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private void assertLastTransactionCannotBeAdded(final Function<TestContext, List<Transaction>> createTransactions,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createTransactions.apply(context);

		// Act:
		final ValidationResult result1 = context.add(prepare(transactions.get(0)));
		final ValidationResult result2 = context.add(prepare(transactions.get(1)));

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(expectedResult));
		MatcherAssert.assertThat(context.getFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(0))));
	}

	// endregion

	// region removeAll

	@Test
	public void removeAllRemovesAllSpecifiedTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createMockTransactions(context, 6, 9);
		context.addAll(transactions);

		// Act:
		context.transactions.removeAll(Arrays.asList(transactions.get(1), transactions.get(3)));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.asFilter().getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 8)));
	}

	@Test
	public void removeAllIgnoresUnknownTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createMockTransactions(context, 6, 9);
		context.addAll(transactions);

		// Act:
		context.transactions.removeAll(createMockTransactions(context, 1, 3));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.asFilter().getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void removeAllRebuildsCacheWhenTransactionInCacheIsRemoved() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = this.createTestContext();
		final List<TransferTransaction> transactions = createThreeTransferTransactions(context, 100, 12, 0);
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(50));

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.removeAll(Collections.singletonList(transactions.get(0)));

		// Assert:
		// - removing the first transaction triggers an exception and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (50) < 80 + 2
		// - second transaction cannot be added - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		MatcherAssert.assertThat(numTransactions, IsEqual.equalTo(3));
		MatcherAssert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
		assertThreeTransactionBalances(context, transactions, 50, 0, 10);
	}

	@Test
	public void removeAllRebuildsCacheWhenExternalTransactionIsRemoved() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = this.createTestContext();
		final List<TransferTransaction> transactions = createThreeTransferTransactions(context, 100, 20, 0);

		final Transaction transaction = createTransferWithTimeStamp(transactions.get(0).getSigner(), transactions.get(0).getRecipient(), 8,
				8);

		// Act:
		final int numTransactions = context.transactions.size();

		// Before the call to removeAll the transaction contained in the block is usually executed (which
		// will change the confirmed balance) and thus account1 is debited 80 + 2 NEM and account2 is credited 80 NEM
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(18));
		context.setBalance(transactions.get(1).getSigner(), Amount.fromNem(100));
		context.transactions.removeAll(Collections.singletonList(transaction));

		// Assert:
		// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (18) < 80 + 2
		// - second transaction can be added - account2 balance (100) >= 50 + 2
		// - third transaction can be added - account2 balance (48) >= 10 + 2
		MatcherAssert.assertThat(numTransactions, IsEqual.equalTo(3));
		MatcherAssert.assertThat(context.transactions.asFilter().getAll(),
				IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
		assertThreeTransactionBalances(context, transactions, 18, 36, 60);
	}

	@Test
	public void removeAllRebuildsCacheWhenNoTransactionsAreRemoved() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = this.createTestContext();
		final List<TransferTransaction> transactions = createThreeTransferTransactions(context, 100, 12, 0);
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(50));

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.removeAll(Collections.emptyList());

		// Assert:
		// - removing the first transaction triggers an exception and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (50) < 80 + 2
		// - second transaction cannot be added - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		MatcherAssert.assertThat(numTransactions, IsEqual.equalTo(3));
		MatcherAssert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
		assertThreeTransactionBalances(context, transactions, 50, 0, 10);
	}

	// endregion

	// region dropExpiredTransactions

	@Test
	public void dropExpiredTransactionsRemovesAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createMockTransactions(context, 4, 7);
		transactions.get(0).setDeadline(new TimeInstant(CURRENT_TIME + 5));
		transactions.get(1).setDeadline(new TimeInstant(CURRENT_TIME + 7));
		transactions.get(2).setDeadline(new TimeInstant(CURRENT_TIME + 6));
		transactions.get(3).setDeadline(new TimeInstant(CURRENT_TIME + 8));
		transactions.forEach(t -> {
			t.sign();
			context.add(t);
		});

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(CURRENT_TIME + 7));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.asFilter().getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(5, 7)));
	}

	@Test
	public void dropExpiredTransactionsDropsAllTransactionsThatAreDependentOnTheDroppedTransactions() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = this.createTestContext();
		final List<TransferTransaction> transactions = createThreeTransferTransactions(context, 100, 12, 0);
		context.addAll(transactions);

		// Act: add 17s because the deadline is 10s past the timestamp
		final int numTransactions = context.transactions.size();
		context.transactions.dropExpiredTransactions(new TimeInstant(CURRENT_TIME + 17));

		// Assert:
		// - first transaction was dropped because it expired
		// - second was dropped because it was dependent on the first - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		MatcherAssert.assertThat(numTransactions, IsEqual.equalTo(3));
		MatcherAssert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
		assertThreeTransactionBalances(context, transactions, 100, 0, 10);
	}

	// endregion

	// region create transactions

	private static MockTransaction createMockTransaction(final Account account, final int customField) {
		return prepare(new MockTransaction(account, customField, new TimeInstant(CURRENT_TIME + customField)));
	}

	private static TransferTransaction createTransferWithTimeStamp(final Account sender, final Account recipient, final int amount,
			final int timeStamp) {
		final TransferTransaction t = new TransferTransaction(1, new TimeInstant(CURRENT_TIME + timeStamp), sender, recipient,
				Amount.fromNem(amount), null);
		t.setFee(Amount.fromNem(2));
		return prepare(t);
	}

	private static List<TransferTransaction> createThreeTransferTransactions(final TestContext context, final int amount1,
			final int amount2, final int amount3) {
		final Account account1 = context.addAccount(Amount.fromNem(amount1));
		final Account account2 = context.addAccount(Amount.fromNem(amount2));
		final Account account3 = context.addAccount(Amount.fromNem(amount3));
		final List<TransferTransaction> transactions = Arrays.asList(createTransferWithTimeStamp(account1, account2, 80, 5),
				createTransferWithTimeStamp(account2, account3, 50, 8), createTransferWithTimeStamp(account2, account3, 10, 9));
		context.addAll(transactions);
		return transactions;
	}

	private static void assertThreeTransactionBalances(final TestContext context, final List<TransferTransaction> transactions,
			final int amount1, final int amount2, final int amount3) {
		MatcherAssert.assertThat(context.transactions.getUnconfirmedBalance(transactions.get(0).getSigner()),
				IsEqual.equalTo(Amount.fromNem(amount1)));
		MatcherAssert.assertThat(context.transactions.getUnconfirmedBalance(transactions.get(1).getSigner()),
				IsEqual.equalTo(Amount.fromNem(amount2)));
		MatcherAssert.assertThat(context.transactions.getUnconfirmedBalance(transactions.get(1).getRecipient()),
				IsEqual.equalTo(Amount.fromNem(amount3)));
	}

	// endregion

	// region TestContext

	private TestContext createTestContext() {
		return new TestContext(this::createUnconfirmedTransactions);
	}

	private static class TestContext implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTestContext {
		private final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();
		private final UnconfirmedTransactions transactions;

		public TestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME);
			final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(NisUtils.createTransactionValidatorFactory(timeProvider),
					NisUtils.createBlockTransactionObserverFactory()::createExecuteCommitObserver, timeProvider,
					() -> new BlockHeight(1234), NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK, new ForkConfiguration());
			this.transactions = creator.apply(factory, this.nisCache);
		}

		public UnconfirmedTransactionsFilter getFilter() {
			return this.transactions.asFilter();
		}

		public ValidationResult add(final Transaction transaction) {
			return this.transactions.addNew(transaction);
		}

		public void addAll(final Collection<? extends Transaction> transactions) {
			transactions.forEach(t -> this.transactions.addNew(prepare(t)));
		}

		@Override
		public Account addAccount(final Amount amount) {
			final Account account = Utils.generateRandomAccount();
			this.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount));
			this.transactions.removeAll(Collections.emptyList());
			return account;
		}

		public void notify(final Notification notification) {
			this.modifyCache(copyCache -> NisUtils.createBlockTransactionObserverFactory().createExecuteCommitObserver(copyCache)
					.notify(notification, new BlockNotificationContext(new BlockHeight(2), TimeInstant.ZERO, NotificationTrigger.Execute)));
		}

		private void modifyCache(final Consumer<NisCache> modify) {
			final NisCache nisCacheCopy = this.nisCache.copy();
			modify.accept(nisCacheCopy);
			nisCacheCopy.commit();
		}

		private void setBalance(final Account account, final Amount amount) {
			final ReadOnlyAccountInfo accountInfo = this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress())
					.getAccountInfo();
			this.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, accountInfo.getBalance()));
			this.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount));
		}
	}

	// endregion
}
