package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;

import java.util.*;
import java.util.stream.*;

public class NewBlockTransactionsProviderTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 120;

	//region candidate filtering

	@Test
	public void getBlockTransactionsDelegatesToGetTransactionsBefore() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(6);
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(4)),
				new MockTransaction(account2, 2, new TimeInstant(6)),
				new MockTransaction(account2, 3, new TimeInstant(8)));
		context.addTransactions(transactions);
		Mockito.when(context.unconfirmedTransactions.getTransactionsBefore(currentTime))
				.thenReturn(Arrays.asList(transactions.get(0)));

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(6));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.only()).getTransactionsBefore(currentTime);
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsSignedByHarvesterAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account1, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account1, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(10));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(2, 4)));
	}

	@Test
	public void getBlockTransactionsDoesNotIncludeExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)),
				new MockTransaction(account2, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		context.addTransactions(transactions);
		final MockTransaction transaction = new MockTransaction(account2, 5, new TimeInstant(1));
		transaction.setDeadline(new TimeInstant(3600));
		context.addTransaction(transaction);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(3601));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2, 3, 4)));
	}

	@Test
	public void getBlockTransactionsExcludesConflictingTransactions() {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(
				Utils.generateRandomAddress(),
				new TimeInstant(10));
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions);

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(1))));
	}

	//endregion

	//region revalidation checking

	@Test
	public void getBlockTransactionsExcludesTransactionsWithNeutralValidationResult() {
		// Assert:
		assertTransactionWithValidationResultIsFiltered(ValidationResult.NEUTRAL);
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsWithFailedValidationResult() {
		// Assert:
		assertTransactionWithValidationResultIsFiltered(ValidationResult.FAILURE_ENTITY_UNUSABLE);
	}

	private static void assertTransactionWithValidationResultIsFiltered(final ValidationResult validationResult) {
		// Arrange:
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		final TestContext context = new TestContext(validator);
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(4)),
				new MockTransaction(account2, 2, new TimeInstant(6)),
				new MockTransaction(account2, 3, new TimeInstant(8)));
		context.addTransactions(transactions);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		Mockito.when(validator.validate(Mockito.eq(transactions.get(1)), Mockito.any())).thenReturn(validationResult);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(3000));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 3)));
	}

	//endregion

	//region max transaction checking

	//region no child transactions

	@Test
	public void getBlockTransactionsReturnsAllTransactionsWhenLessThanMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(5, 5);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenMoreThanMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenExactlyMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
	}

	private static void assertNumTransactionsReturned(final int numTransactions, final int numFilteredTransactions) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		context.addTransactions(account2, 6, 6 + numTransactions - 1);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(3000));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues.size(), IsEqual.equalTo(numFilteredTransactions));
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(createIntRange(6, 6 + numFilteredTransactions)));
	}

	//endregion

	//region child transactions

	@Test
	public void getBlockTransactionsReturnsLessThanMaximumTransactionsWhenLastTransactionAndChildrenCannotFit() {
		// 7 child transactions per transaction in the list, 120 / 7 == 17.14...
		assertNumTransactionsReturned(2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, 6, 17);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenLastTransactionAndChildrenCanFit() {
		// Assert:
		// 7 child transactions per transaction in the list, 120 / 8 == 15
		assertNumTransactionsReturned(2 * MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, 7, 15);
	}

	private static void assertNumTransactionsReturned(final int numTransactions, final int numChildTransactions, final int numFilteredTransactions) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		context.addTransactionsWithChildren(account2, 6, 6 + numTransactions - 1, numChildTransactions);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(account1.getAddress(), new TimeInstant(3000));
		final List<Integer> customFieldValues = getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues.size(), IsEqual.equalTo(numFilteredTransactions));
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(createIntRange(6, 6 + numFilteredTransactions)));

		final int numTotalTransactions = filteredTransactions.stream().mapToInt(t -> 1 + t.getChildTransactions().size()).sum();
		Assert.assertThat(numTotalTransactions, IsEqual.equalTo((numChildTransactions + 1) * numFilteredTransactions));
	}

	//endregion

	//endregion

	// TODO 20150222 J-J: refactor to MockTransaction!
	private static List<TimeInstant> getTimeInstantsAsList(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(Transaction::getTimeStamp)
				.collect(Collectors.toList());
	}

	private static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(transaction -> ((MockTransaction)transaction).getCustomField())
				.collect(Collectors.toList());
	}

	private static List<Integer> createIntRange(final int start, final int end) {
		return IntStream.range(start, end).mapToObj(i -> i).collect(Collectors.toList());
	}

	private static class TestContext {
		private final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final NewBlockTransactionsProvider provider;
		private final List<Transaction> transactions = new ArrayList<>();

		public TestContext() {
			this((transaction, context) -> ValidationResult.SUCCESS);
		}

		private TestContext(final TransferTransactionValidator singleValidator) {
			this(new TSingleTransactionValidatorAdapter<>(TransactionTypes.TRANSFER, singleValidator));
		}

		private TestContext(final SingleTransactionValidator singleValidator) {
			Mockito.when(unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);
			Mockito.when(this.validatorFactory.createSingle(Mockito.any())).thenReturn(singleValidator);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);

			this.provider = new NewBlockTransactionsProvider(
					this.nisCache,
					this.validatorFactory,
					this.unconfirmedTransactions);
		}

		//region addAccount

		public Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

		//endregion

		//region addTransaction

		public void addTransaction(final Transaction transaction) {
			this.transactions.add(transaction);
		}

		public void addTransactions(final Collection<? extends Transaction> transactions) {
			this.transactions.addAll(transactions);
		}

		public void addTransactions(final Account signer, final int startCustomField, final int endCustomField) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				this.addTransaction(new MockTransaction(signer, i));
			}
		}

		public void addTransactionsWithChildren(final Account signer, final int startCustomField, final int endCustomField, final int numChildren) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				final MockTransaction transaction = new MockTransaction(signer, i);
				final List<Transaction> childTransactions = new ArrayList<>();
				for (int j = 0; j < numChildren; ++j) {
					childTransactions.add(new MockTransaction());
				}

				transaction.setChildTransactions(childTransactions);
				this.addTransaction(transaction);
			}
		}

		//endregion
	}
}