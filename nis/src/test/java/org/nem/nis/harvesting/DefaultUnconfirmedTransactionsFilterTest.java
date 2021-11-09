package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class DefaultUnconfirmedTransactionsFilterTest {

	// region getAll

	@Test
	public void getAllReturnsAllTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.filter.getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7, 8, 9)));
	}

	@Test
	public void getAllReturnsAllTransactionsInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.transactions.get(2).setFee(Amount.fromNem(11));

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.filter.getAll());

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(8, 9, 7, 6)));
	}

	// endregion

	// region getUnknownTransactions

	@Test
	public void getUnknownTransactionsReturnsAllTransactionsIfHashShortIdListIsEmpty() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 3);

		// Act:
		final Collection<Transaction> unknownTransactions = context.filter.getUnknownTransactions(new ArrayList<>());

		// Assert:
		MatcherAssert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(context.transactions));
	}

	@Test
	public void getUnknownTransactionsFiltersKnownTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 6);
		final List<HashShortId> hashShortIds = Arrays.asList(1, 2, 4).stream()
				.map(i -> new HashShortId(HashUtils.calculateHash(context.transactions.get(i)).getShortId())).collect(Collectors.toList());

		// Act:
		final Collection<Transaction> unknownTransactions = context.filter.getUnknownTransactions(hashShortIds);

		// Assert:
		MatcherAssert.assertThat(unknownTransactions,
				IsEquivalent.equivalentTo(Arrays.asList(0, 3, 5).stream().map(context.transactions::get).collect(Collectors.toList())));
	}

	@Test
	public void getUnknownTransactionsReturnsEmptyListIfAllTransactionsAreKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 6);
		final List<HashShortId> hashShortIds = context.transactions.stream()
				.map(t -> new HashShortId(HashUtils.calculateHash(t).getShortId())).collect(Collectors.toList());

		// Act:
		final Collection<Transaction> unknownTransactions = context.filter.getUnknownTransactions(hashShortIds);

		// Assert:
		MatcherAssert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(new ArrayList<>()));
	}

	@Test
	public void getUnknownTransactionsReturnsSignatureTransactionsAsOwnEntities() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigTransaction transaction = RandomTransactionFactory.createMultisigTransferWithThreeSignatures();
		context.transactions.add(transaction);
		final List<Transaction> expectedTransactions = new ArrayList<>();
		expectedTransactions.add(transaction);
		expectedTransactions.addAll(transaction.getCosignerSignatures());

		// Act:
		final Collection<Transaction> unknownTransactions = context.filter.getUnknownTransactions(new ArrayList<>());

		// Assert:
		MatcherAssert.assertThat(unknownTransactions, IsEquivalent.equivalentTo(expectedTransactions));
	}

	// endregion

	// region getMostRecentTransactionsForAccount

	@Test
	public void getMostRecentTransactionsForAccountReturnsAllTransactionsIfLessThanGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactionsForAccount(10, 20, 10);
	}

	@Test
	public void getMostRecentTransactionsForAccountReturnsMaximumTransactionsIfMoreThanGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactionsForAccount(20, 10, 10);
	}

	@Test
	public void getMostRecentTransactionsForAccountReturnsMaximumTransactionsIfGivenLimitTransactionsAreAvailable() {
		// Assert:
		assertNumMostRecentTransactionsForAccount(10, 10, 10);
	}

	private static void assertNumMostRecentTransactionsForAccount(final int numTotal, final int numRequested, final int numExpected) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, numTotal);

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(),
				numRequested);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(numExpected));
	}

	@Test
	public void getMostRecentTransactionsForAccountDoesNotCountChildTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithChildren(6, 20, 7);

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), 10);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(10));
	}

	@Test
	public void getMostRecentTransactionsForAccountExcludesSignatureTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactions(12, 16);
		context.addSignatureTransactions(1, 10);

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), 20);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(MockTransactionUtils.getCustomFieldValues(mostRecentTransactions),
				IsEquivalent.equivalentTo(12, 13, 14, 15, 16));
	}

	@Test
	public void getMostRecentTransactionsForAccountExcludesNonMatchingTransactions() {
		// Arrange:
		final TestContext context = new TestContext((account, transaction) -> 0 == ((MockTransaction) transaction).getCustomField() % 3);
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactions(1, 10);

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), 20);

		// Assert:
		MatcherAssert.assertThat(mostRecentTransactions.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(MockTransactionUtils.getCustomFieldValues(mostRecentTransactions), IsEquivalent.equivalentTo(3, 6, 9));
	}

	@Test
	public void getMostRecentTransactionsForAccountReturnsTransactionsSortedByTimeInDescendingOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		context.addMockTransactionsWithRandomTimeStamp(account, 10);

		// Act:
		final Collection<Transaction> mostRecentTransactions = context.filter.getMostRecentTransactionsForAccount(account.getAddress(), 25);

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
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils
				.getCustomFieldValues(context.filter.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	@Test
	public void getTransactionsBeforeReturnsAllTransactionsBeforeSpecifiedTimeInstantInSortedOrder() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.transactions.get(1).setFee(Amount.fromNem(11));

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils
				.getCustomFieldValues(context.filter.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(7, 6)));
	}

	@Test
	public void getTransactionsBeforeExcludesSignatureTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMockTransactions(6, 9);
		context.addSignatureTransactions(2, 5);

		// Act:
		final List<Integer> customFieldValues = MockTransactionUtils
				.getCustomFieldValues(context.filter.getTransactionsBefore(new TimeInstant(8)));

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 7)));
	}

	// endregion

	private static class TestContext {
		private final List<Transaction> transactions = new ArrayList<>();
		private final UnconfirmedTransactionsCache cache = Mockito.mock(UnconfirmedTransactionsCache.class);
		private final UnconfirmedTransactionsFilter filter;

		public TestContext() {
			this((address, transaction) -> true);
		}

		public TestContext(final BiPredicate<Address, Transaction> matchesPredicate) {
			this.filter = new DefaultUnconfirmedTransactionsFilter(this.cache, matchesPredicate);
			Mockito.when(this.cache.stream()).thenAnswer(invocationOnMock -> this.transactions.stream());
		}

		private void addMockTransactions(final int startCustomField, final int endCustomField) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				this.transactions.add(createMockTransaction(Utils.generateRandomAccount(), new TimeInstant(i), i));
			}
		}

		private void addSignatureTransactions(final int startCustomField, final int endCustomField) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				this.transactions.add(new MockTransaction(TransactionTypes.MULTISIG_SIGNATURE, i, new TimeInstant(i)));
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
				final MockTransaction mockTransaction = (MockTransaction) t;
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
