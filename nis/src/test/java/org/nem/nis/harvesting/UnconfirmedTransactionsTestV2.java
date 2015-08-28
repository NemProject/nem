package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.*;

public abstract class UnconfirmedTransactionsTestV2 {
	private static final int CURRENT_TIME = 10_000;

	/**
	 * Creates the unconfirmed transactions cache.
	 *
	 * @param unconfirmedStateFactory The unconfirmed state factory to use.
	 * @param nisCache The NIS cache to use.
	 * @return The unconfirmed transactions cache.
	 */
	public abstract UnconfirmedTransactions createUnconfirmedTransactions(
			final UnconfirmedStateFactory unconfirmedStateFactory,
			final ReadOnlyNisCache nisCache);

	//region size

	@Test
	public void sizeReturnsTheNumberOfTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(1000));

		// Act:
		for (int i = 0; i < 17; ++i) {
			context.transactions.addNew(prepare(new MockTransaction(account, i, new TimeInstant(CURRENT_TIME + i % 7))));
		}

		// Assert:
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(17));
	}

	//endregion

	//region multilevel existence checks

	@Test
	public void cannotAddChildTransactionIfParentHasBeenAdded() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = createMockTransaction(sender, 7);
		final MockTransaction outer = createMockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));
		context.transactions.addExisting(prepare(outer));

		// Act
		final ValidationResult result = context.transactions.addNew(prepare(inner));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void cannotAddParentTransactionIfChildHasBeenAdded() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account sender = context.addAccount(Amount.fromNem(100));

		final Transaction inner = createMockTransaction(sender, 7);
		final MockTransaction outer = createMockTransaction(sender, 8);
		outer.setChildTransactions(Collections.singletonList(inner));
		context.transactions.addExisting(inner);

		// Act
		final ValidationResult result = context.transactions.addNew(outer);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	//endregion

	//region validation (unconfirmed conflicts)

	@Test
	public void addFailsIfImportanceTransferIsConflicting() {
		// Assert:
		this.assertLastTransactionCannotBeAdded(context -> {
			final Account sender = context.addAccount(Amount.fromNem(50000));
			final Account remote = Utils.generateRandomAccount();
			return Arrays.asList(
					createImportanceTransfer(sender, remote, 1000),
					createImportanceTransfer(sender, remote, 2000));
		}, ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS);
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Assert:
		this.assertLastTransactionCannotBeAdded(context -> {
			final Account account1 = context.addAccount(Amount.fromNem(14));
			final Account account2 = context.addAccount(Amount.fromNem(110));
			return Arrays.asList(
					createTransfer(account1, account2, 5, 5),
					createTransfer(account1, account2, 8, 2));
		}, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private void assertLastTransactionCannotBeAdded(final Function<TestContext, List<Transaction>> createTransactions, final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createTransactions.apply(context);
		context.transactions.addExisting(prepare(transactions.get(0)));

		// Act:
		final ValidationResult result = context.transactions.addExisting(prepare(transactions.get(1)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		Assert.assertThat(context.getFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(0))));
	}

	//endregion

	//region create transactions

	// TODO 20150827 J-J: refactor some of these helpers

	private static MockTransaction createMockTransaction(final Account account, final int customField) {
		return prepare(new MockTransaction(account, customField, new TimeInstant(CURRENT_TIME + customField)));
	}

	private static MockTransaction createMockTransaction(final TestContext context, final int customField) {
		final Account account = context.addAccount(Amount.fromNem(1_000));
		return createMockTransaction(account, customField);
	}

	private static List<Transaction> createMockTransactions(final TestContext context, final int startCustomField, final int endCustomField) {
		final List<Transaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			transactions.add(createMockTransaction(context, i));
		}

		return transactions;
	}

	private static List<Transaction> createMockTransactionsWithRandomTimeStamp(final Account account, final int count) {
		final List<Transaction> transactions = new ArrayList<>();
		final SecureRandom random = new SecureRandom();

		for (int i = 0; i < count; ++i) {
			final TimeInstant timeStamp = new TimeInstant(CURRENT_TIME + random.nextInt(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME));
			transactions.add(prepare(new MockTransaction(account, i, timeStamp)));
		}

		return transactions;
	}

	private static Transaction createTransfer(final Account sender, final Account recipient, final int amount, final int fee) {
		final Transaction t = new TransferTransaction(1, new TimeInstant(CURRENT_TIME), sender, recipient, Amount.fromNem(amount), null);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	private static Transaction createImportanceTransfer(final Account sender, final Account remote, final int fee) {
		final Transaction t = new ImportanceTransferTransaction(new TimeInstant(CURRENT_TIME), sender, ImportanceTransferMode.Activate, remote);
		t.setFee(Amount.fromNem(fee));
		return prepare(t);
	}

	private static <T extends Transaction> T prepare(final T transaction) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
		transaction.sign();
		return transaction;
	}

	//endregion

	//region TestContext

	private TestContext createTestContext() {
		return new TestContext(this::createUnconfirmedTransactions);
	}

	private static class TestContext {
		private final ReadOnlyNisCache nisCache = NisCacheFactory.createReal();
		private final UnconfirmedTransactions transactions;

		public TestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			final TimeProvider timeProvider = Utils.createMockTimeProvider(CURRENT_TIME);
			final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
					NisUtils.createTransactionValidatorFactory(timeProvider),
					NisUtils.createBlockTransactionObserverFactory()::createExecuteCommitObserver,
					timeProvider,
					() -> new BlockHeight(1234));
			this.transactions = creator.apply(factory, this.nisCache);
		}

		public UnconfirmedTransactionsFilter getFilter() {
			return this.transactions.asFilter();
		}

		public void addAll(final Collection<Transaction> transactions) {
			transactions.forEach(t -> this.transactions.addNew(prepare(t)));
		}

		//region modify state

		public Account addAccount(final Amount amount) {
			final Account account = Utils.generateRandomAccount();
			this.modifyCache(copyCache ->
					NisUtils.createBlockTransactionObserverFactory().createExecuteCommitObserver(copyCache).notify(
							new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount),
							new BlockNotificationContext(BlockHeight.ONE, TimeInstant.ZERO, NotificationTrigger.Execute)));
			this.transactions.removeAll(Collections.emptyList());
			return account;
		}

		private void modifyCache(final Consumer<NisCache> modify) {
			final NisCache nisCacheCopy = this.nisCache.copy();
			modify.accept(nisCacheCopy);
			nisCacheCopy.commit();
		}

		//endregion
	}

	//endregion
}
