package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsFilterTest {
	private final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 64;

	//region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.filter.getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getAllReturnsAllTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.transactions.get(2).setFee(Amount.fromNem(11));

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.filter.getAll());

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
		context.addMockTransactionsWithRandomTimeStamp(account, 3);

		// Act:
		final List<Transaction> unknownTransactions = context.filter.getUnknownTransactions(new ArrayList<>());

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(context.transactions));
	}

	@Test
	public void getUnknownTransactionsFiltersKnownTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 6);
		final List<HashShortId> hashShortIds = Arrays.asList(1, 2, 4).stream()
				.map(i -> new HashShortId(HashUtils.calculateHash(context.transactions.get(i)).getShortId()))
				.collect(Collectors.toList());

		// Act:
		final List<Transaction> unknownTransactions = context.filter.getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(
				unknownTransactions,
				IsEquivalent.equivalentTo(Arrays.asList(0, 3, 5).stream().map(context.transactions::get).collect(Collectors.toList())));
	}

	@Test
	public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 6);
		final List<HashShortId> hashShortIds = context.transactions.stream()
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());

		// Act:
		final List<Transaction> unknownTransactions = context.filter.getUnknownTransactions(hashShortIds);

		// Assert:
		Assert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
	}

	// endregion

	//region getMostRecentTransactionsForAccount

	@Test
	public void getMostRecentTransactionsReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactions(10, 20, 10);
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactions(20, 10, 10);
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactions(10, 10, 10);
	}

	private static void assertNumMostRecentTransactions(final int numTotal, final int numRequested, final int numExpected) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, numTotal);

		// Act:
		final List<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), numRequested);

		// Assert:
		Assert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(numExpected));
	}

	@Test
	public void getMostRecentTransactionsReturnsTransactionsSortedByTimeInDescendingOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 10);

		// Act:
		final List<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), 25);

		// Assert:
		TimeInstant curTimeStamp = new TimeInstant(Integer.MAX_VALUE);
		for (final Transaction tx : mostRecentTransactions) {
			Assert.assertThat(tx.getTimeStamp().compareTo(curTimeStamp) <= 0, IsEqual.equalTo(true));
			curTimeStamp = tx.getTimeStamp();
		}
	}

	//endregion

	//region getMostImportantTransactions

	@Test
	public void getMostImportantTransactionsReturnsAllTransactionsWhenLessThanMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
		final List<Integer> customFieldValues = getCustomFieldValues(transactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsWhenMoreThanMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsWhenExactlyMaximumTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 6 + MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1);

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsLessThanMaximumTransactionsWhenLastTransactionAndChildrenCannotFit() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactionsWithChildren(6, 2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, 6);

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		// 6 child transactions per transaction in the list, 64 is not divisible by 7
		final int count = transactions.stream().mapToInt(t -> 1 + t.getChildTransactions().size()).sum();
		Assert.assertThat(count <= MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, IsEqual.equalTo(true));
		Assert.assertThat(count, IsEqual.equalTo(63));
	}

	@Test
	public void getMostImportantTransactionsReturnsMaximumTransactionsWhenLastTransactionAndChildrenCanFit() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactionsWithChildren(6, 2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, 7);

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);

		// Assert:
		// 7 child transactions per transaction in the list, 64 is divisible by 8
		final int count = transactions.stream().mapToInt(t -> 1 + t.getChildTransactions().size()).sum();
		Assert.assertThat(count, IsEqual.equalTo(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK));
	}

	@Test
	public void getMostImportantTransactionsReturnsTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.transactions.get(2).setFee(Amount.fromNem(11));

		// Act:
		final List<Transaction> transactions = context.filter.getMostImportantTransactions(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
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
		context.addMockTransactions(6, 9);

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.filter.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.transactions.get(1).setFee(Amount.fromNem(11));

		// Act:
		final List<Integer> customFieldValues = getCustomFieldValues(context.filter.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		Assert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(7, 6)));
	}

	//endregion

	private static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(transaction -> ((MockTransaction)transaction).getCustomField())
				.collect(Collectors.toList());
	}

	private static class TestContext {
		private final List<Transaction> transactions = new ArrayList<>();
		private final UnconfirmedTransactionsCache cache = Mockito.mock(UnconfirmedTransactionsCache.class);
		private final UnconfirmedTransactionsFilter filter = new UnconfirmedTransactionsFilter(this.cache);

		public TestContext() {
			Mockito.when(this.cache.stream()).thenReturn(this.transactions.stream());
		}

		private void addMockTransactions(final int startCustomField, final int endCustomField) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				this.transactions.add(createMockTransaction(Utils.generateRandomAccount(), new TimeInstant(i), i));
			}
		}

		private void addMockTransactionsWithRandomTimeStamp(final Account account, final int count) {
			final SecureRandom random = new SecureRandom();
			for (int i = 0; i < count; ++i) {
				this.transactions.add(createMockTransaction(account, new TimeInstant(random.nextInt(1_000_000)), i));
			}
		}

		private void addMockTransactionsWithChildren(final int startCustomField, final int endCustomField, final int numChildren) {
			createMockTransactions(startCustomField, endCustomField).forEach(t -> {
				final MockTransaction mockTransaction = (MockTransaction)t;
				final int customField = mockTransaction.getCustomField();
				mockTransaction.setChildTransactions(createMockTransactions(customField * 100, customField * 100 + numChildren - 1));
				this.transactions.add(mockTransaction);
			});
		}
	}

	private static Transaction createMockTransaction(final Account account, final TimeInstant timeStamp, final int customField) {
		final MockTransaction transaction = new MockTransaction(account, customField, timeStamp);
		transaction.setFee(Amount.fromNem(customField));
		return transaction;
	}

	private static List<Transaction> createMockTransactions(final int startCustomField, final int endCustomField) {
		final List<Transaction> transactions = new ArrayList<>();
		for (int i = startCustomField; i <= endCustomField; ++i) {
			transactions.add(createMockTransaction(Utils.generateRandomAccount(), new TimeInstant(i), i));
		}

		return transactions;
	}
}