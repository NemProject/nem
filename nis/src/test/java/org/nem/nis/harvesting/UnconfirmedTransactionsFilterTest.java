package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.test.UnconfirmedTransactionsTestUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.createMockTransactions;
import static org.nem.nis.test.UnconfirmedTransactionsTestUtils.createMockTransactionsWithRandomTimeStamp;

public abstract class UnconfirmedTransactionsFilterTest implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTest {
	private static final int CURRENT_TIME = UnconfirmedTransactionsTestUtils.CURRENT_TIME;

	// region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = this.createTestContext();
		context.addAll(createMockTransactions(context, 6, 9));

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.getFilter().getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
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
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 6, 7, 9)));
	}

	// endregion

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
		MatcherAssert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(transactions));
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
		MatcherAssert.assertThat(unknownTransactions,
				IsEquivalent.equivalentTo(Arrays.asList(transactions.get(0), transactions.get(3), transactions.get(5))));
	}

	@Test
	public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = createMockTransactionsWithRandomTimeStamp(account, 6);
		context.addAll(transactions);
		final List<HashShortId> hashShortIds = transactions.stream().map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId()))
				.collect(Collectors.toList());

		// Act:
		final Collection<Transaction> unknownTransactions = context.getFilter().getUnknownTransactions(hashShortIds);

		// Assert:
		MatcherAssert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
	}

	// endregion

	// region getMostRecentTransactionsForAccount

	@Test
	public void getMostRecentTransactionsReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(),
				20);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 20));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(),
				10);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(),
				10);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsReturnsTransactionsSortedByTimeInDescendingOrder() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = context.addAccount(Amount.fromNem(100));
		context.addAll(createMockTransactionsWithRandomTimeStamp(account, 10));

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.getFilter().getMostRecentTransactionsForAccount(account.getAddress(),
				25);

		// Assert:
		TimeInstant curTimeStamp = new TimeInstant(Integer.MAX_VALUE);
		for (final Transaction tx : mostRecentTransactions) {
			MatcherAssert.assertThat(tx.getTimeStamp().compareTo(curTimeStamp) <= 0, IsEqual.equalTo(true));
			curTimeStamp = tx.getTimeStamp();
		}
	}

	// endregion

	// region getTransactionsBefore

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = this.createTestContext();
		context.addAll(createMockTransactions(context, 6, 9));

		// Act:
		final TimeInstant timeThreshold = new TimeInstant(CURRENT_TIME + 8);
		final List<Integer> customFieldValues = MockTransactionUtils
				.getCustomFieldValues(context.getFilter().getTransactionsBefore(timeThreshold));

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
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
		final List<Integer> customFieldValues = MockTransactionUtils
				.getCustomFieldValues(context.getFilter().getTransactionsBefore(timeThreshold));

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 6, 7)));
	}

	// endregion

	// region TestContext

	private TestContext createTestContext() {
		return new TestContext(this::createUnconfirmedTransactions);
	}

	private static class TestContext extends UnconfirmedTransactionsTestUtils.NonExecutingUnconfirmedTransactionsTestContext {

		public TestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			super(creator);
		}
	}

	// endregion
}
