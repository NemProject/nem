package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.test.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public abstract class UnconfirmedTransactionsFilterTest {
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

	//region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		context.addAll(createMockTransactions(context, 6, 9));

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.getFilter().getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getAllReturnsAllTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createMockTransactions(context, 6, 9);
		transactions.get(2).setFee(Amount.fromNem(11));
		context.addAll(transactions);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.getFilter().getAll());

		// Assert:
		// TODO 20150827 J-B: seems like something was broken in the test before as this ordering seems to be correct now
		// > (given same fee, older transactions are preferred)
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 6, 7, 9)));
	}

	//endregion

	// region getUnknownTransactions

	@Test
	public void getUnknownTransactionsReturnsAllTransactionsIfHashShortIdListIsEmpty() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = createMockTransactionsWithRandomTimeStamp(account, 3);
		context.addAll(transactions);

		// Act:
		final Collection<Transaction> unknownTransactions = context.getFilter().getUnknownTransactions(new ArrayList<>());

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(transactions));
	}

	@Test
	public void getUnknownTransactionsFiltersKnownTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = createMockTransactionsWithRandomTimeStamp(account, 6);
		context.addAll(transactions);
		final List<HashShortId> hashShortIds = new ArrayList<>();
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(1)).getShortId()));
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(2)).getShortId()));
		hashShortIds.add(new HashShortId(HashUtils.calculateHash(transactions.get(4)).getShortId()));

		// Act:
		final Collection<Transaction> unknownTransactions = context.getFilter().getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(
				unknownTransactions,
				IsEquivalent.equivalentTo(Arrays.asList(transactions.get(0), transactions.get(3), transactions.get(5))));
	}

	@Test
	public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = createMockTransactionsWithRandomTimeStamp(account, 6);
		context.addAll(transactions);
		final List<HashShortId> hashShortIds = transactions.stream()
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());

		// Act:
		final Collection<Transaction> unknownTransactions = context.getFilter().getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
	}

	// endregion

	//region getMostRecentTransactionsForAccount

	@Test
	public void getMostRecentTransactionsReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(), 20);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 20));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(), 10);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(), 10);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsTransactionsSortedByTimeInDescendingOrder() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(), 25);

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
		final TestContext context = this.createTestContext();
		context.addAll(createMockTransactions(context, 6, 9));

		// Act:
		final TimeInstant timeThreshold = new TimeInstant(CURRENT_TIME + 8);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.getFilter().getTransactionsBefore(timeThreshold));

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final List<Transaction> transactions = createMockTransactions(context, 6, 9);
		transactions.get(2).setFee(Amount.fromNem(11));
		context.addAll(transactions);

		// Act:
		final TimeInstant timeThreshold = new TimeInstant(CURRENT_TIME + 9);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.getFilter().getTransactionsBefore(timeThreshold));

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 6, 7)));
	}

	//endregion

	//region create transactions

	private static MockTransaction createMockTransaction(final TestContext context, final int customField) {
		final Account account = context.addAccount(Amount.fromNem(1_000));
		return prepare(new MockTransaction(account, customField, new TimeInstant(CURRENT_TIME + customField)));
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
					cache -> (notification, context) -> { },
					timeProvider,
					BlockHeight.MAX::prev);
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
			this.modifyCache(accountStateCache ->
					accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo().incrementBalance(amount));
			this.transactions.removeAll(Collections.emptyList());
			return account;
		}

		private void modifyCache(final Consumer<AccountStateCache> modify) {
			final NisCache nisCacheCopy = this.nisCache.copy();
			modify.accept(nisCacheCopy.getAccountStateCache());
			nisCacheCopy.commit();
		}

		//endregion
	}

	//endregion
}
