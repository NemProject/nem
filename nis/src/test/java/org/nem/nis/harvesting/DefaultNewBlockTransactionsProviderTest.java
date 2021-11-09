package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.stream.*;

public class DefaultNewBlockTransactionsProviderTest {
	private static final int TRANSFER_TRANSACTION_VERSION = 1;
	private static final int MAX_TRANSACTIONS_PER_BLOCK = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;

	// region candidate filtering

	@Test
	public void getBlockTransactionsDelegatesToGetTransactionsBefore() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeInstant currentTime = new TimeInstant(6);
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(account2, 1, new TimeInstant(4)),
				new MockTransaction(account2, 2, new TimeInstant(6)), new MockTransaction(account2, 3, new TimeInstant(8)));
		context.addTransactions(transactions);
		Mockito.when(context.unconfirmedTransactions.getTransactionsBefore(currentTime))
				.thenReturn(Collections.singletonList(transactions.get(0)));

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1, currentTime);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Collections.singletonList(1)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.only()).getTransactionsBefore(currentTime);
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsSignedByHarvesterAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(account1, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)), new MockTransaction(account1, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(2, 4)));
	}

	@Test
	public void getBlockTransactionsDoesNotIncludeExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(account2, 1, new TimeInstant(2)),
				new MockTransaction(account2, 2, new TimeInstant(4)), new MockTransaction(account2, 3, new TimeInstant(6)),
				new MockTransaction(account2, 4, new TimeInstant(8)));
		context.addTransactions(transactions);
		final MockTransaction transaction = new MockTransaction(account2, 5, new TimeInstant(1));
		transaction.setDeadline(new TimeInstant(3600));
		context.addTransaction(transaction);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1, new TimeInstant(3601));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2, 3, 4)));
	}

	@Test
	public void getBlockTransactionsExcludesConflictingTransactions() {
		// Arrange:
		// - T(O) - A1: 5 | A2: 100
		// - T(1) - A1 -10-> A2 | XXX
		// - T(2) - A2 -10-> A1 | A1: 15 | A2: 88
		// - T(3) - A1 -10-> A2 | A1: 03 | A2: 98
		// - T(4) - A2 -99-> A1 | XXX
		final TestContext context = new TestContext(ProviderFactories.createReal());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(TRANSFER_TRANSACTION_VERSION, new TimeInstant(1), account1, account2, Amount.fromNem(10), null),
				new TransferTransaction(TRANSFER_TRANSACTION_VERSION, new TimeInstant(2), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(TRANSFER_TRANSACTION_VERSION, new TimeInstant(3), account1, account2, Amount.fromNem(10), null),
				new TransferTransaction(TRANSFER_TRANSACTION_VERSION, new TimeInstant(4), account2, account1, Amount.fromNem(99), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions();
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(2), new TimeInstant(3))));
	}

	// endregion

	// region candidate filtering - treasury reissuance

	@Test
	public void getBlockTransactionsReturnsTreasuryReissuanceTransactionsAtForkHeight_SomePreferred() {
		// Arrange: create transactions
		final Account senderAccount = Utils.generateRandomAccount();
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(senderAccount, 1, new TimeInstant(6)),
				new MockTransaction(senderAccount, 4, new TimeInstant(4)), new MockTransaction(senderAccount, 9, new TimeInstant(2)),
				new MockTransaction(senderAccount, 16, new TimeInstant(8)), new MockTransaction(senderAccount, 25, new TimeInstant(0)));

		// - add three matching hashes
		final ArrayList<Hash> hashes = new ArrayList<Hash>();
		hashes.add(HashUtils.calculateHash(transactions.get(0)));
		hashes.add(Utils.generateRandomHash());
		hashes.add(HashUtils.calculateHash(transactions.get(2)));
		hashes.add(Utils.generateRandomHash());
		hashes.add(HashUtils.calculateHash(transactions.get(3)));

		// - create test context and add account
		final TestContext context = new TestContext(new ProviderFactories((transaction, context2) -> ValidationResult.SUCCESS),
				new ForkConfiguration(new BlockHeight(1234), hashes, new ArrayList<Hash>()));
		context.addTransactions(transactions);

		// - create accounts
		context.prepareAccount(senderAccount, Amount.fromNem(100));
		final Account harvesterAccount = context.addAccount(Amount.fromNem(5));

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(harvesterAccount, new BlockHeight(1234));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert: ordered by timestamp
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(9, 1, 16)));
	}

	@Test
	public void getBlockTransactionsReturnsTreasuryReissuanceTransactionsAtForkHeight_SomeFallback() {
		// Arrange: create transactions
		final Account senderAccount = Utils.generateRandomAccount();
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(senderAccount, 1, new TimeInstant(6)),
				new MockTransaction(senderAccount, 4, new TimeInstant(4)), new MockTransaction(senderAccount, 9, new TimeInstant(2)),
				new MockTransaction(senderAccount, 16, new TimeInstant(8)), new MockTransaction(senderAccount, 25, new TimeInstant(0)));

		// - add three matching hashes
		final ArrayList<Hash> fallbackHashes = new ArrayList<Hash>();
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(0)));
		fallbackHashes.add(Utils.generateRandomHash());
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(2)));
		fallbackHashes.add(Utils.generateRandomHash());
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(3)));

		// - create test context and add account
		final TestContext context = new TestContext(new ProviderFactories((transaction, context2) -> ValidationResult.SUCCESS),
				new ForkConfiguration(new BlockHeight(1234), new ArrayList<Hash>(), fallbackHashes));
		context.addTransactions(transactions);

		// - create accounts
		context.prepareAccount(senderAccount, Amount.fromNem(100));
		final Account harvesterAccount = context.addAccount(Amount.fromNem(5));

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(harvesterAccount, new BlockHeight(1234));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert: ordered by timestamp
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(9, 1, 16)));
	}

	@Test
	public void getBlockTransactionsReturnsTreasuryReissuanceTransactionsAtForkHeight_SomePreferredAndSomeFallbackChoosesFallback() {
		// Arrange: create transactions
		final Account senderAccount = Utils.generateRandomAccount();
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(senderAccount, 1, new TimeInstant(6)),
				new MockTransaction(senderAccount, 4, new TimeInstant(4)), new MockTransaction(senderAccount, 9, new TimeInstant(2)),
				new MockTransaction(senderAccount, 16, new TimeInstant(8)), new MockTransaction(senderAccount, 25, new TimeInstant(0)));

		// - add two matching hashes (preferred)
		final ArrayList<Hash> hashes = new ArrayList<Hash>();
		hashes.add(HashUtils.calculateHash(transactions.get(1)));
		hashes.add(Utils.generateRandomHash());
		hashes.add(HashUtils.calculateHash(transactions.get(4)));

		// - add three matching hashes (fallback)
		final ArrayList<Hash> fallbackHashes = new ArrayList<Hash>();
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(0)));
		fallbackHashes.add(Utils.generateRandomHash());
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(2)));
		fallbackHashes.add(Utils.generateRandomHash());
		fallbackHashes.add(HashUtils.calculateHash(transactions.get(3)));

		// - create test context and add account
		final TestContext context = new TestContext(new ProviderFactories((transaction, context2) -> ValidationResult.SUCCESS),
				new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes));
		context.addTransactions(transactions);

		// - create accounts
		context.prepareAccount(senderAccount, Amount.fromNem(100));
		final Account harvesterAccount = context.addAccount(Amount.fromNem(5));

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(harvesterAccount, new BlockHeight(1234));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert: ordered by timestamp
		MatcherAssert.assertThat(customFieldValues, IsEqual.equalTo(Arrays.asList(9, 1, 16)));
	}

	// endregion

	// region revalidation checking - transaction

	@Test
	public void getBlockTransactionsIncludesTransactionsWithSuccessValidationResult() {
		// Assert:
		assertTransactionValidationFiltering(ValidationResult.SUCCESS, Arrays.asList(1, 2, 3));
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsWithNeutralValidationResult() {
		// Assert:
		assertTransactionValidationFiltering(ValidationResult.NEUTRAL, Arrays.asList(1, 3));
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsWithFailedValidationResult() {
		// Assert:
		assertTransactionValidationFiltering(ValidationResult.FAILURE_UNKNOWN, Arrays.asList(1, 3));
	}

	private static void assertTransactionValidationFiltering(final ValidationResult validationResult,
			final List<Integer> expectedFilteredIds) {
		// Arrange:
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(validator));
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS, validationResult,
				ValidationResult.SUCCESS);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions();
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(expectedFilteredIds));
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void getBlockTransactionsPassesCorrectValidationContextToTransactionValidators() {
		// Arrange:
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(validator));
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.getBlockTransactions(Utils.generateRandomAccount(), new BlockHeight(72));

		// Assert:
		final ArgumentCaptor<ValidationContext> validationContextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any(), validationContextCaptor.capture());

		for (final ValidationContext validationContext : validationContextCaptor.getAllValues()) {
			MatcherAssert.assertThat(validationContext.getBlockHeight(), IsEqual.equalTo(new BlockHeight(72)));
			MatcherAssert.assertThat(validationContext.getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(72)));
		}
	}

	// endregion

	// region revalidation checking - block

	@Test
	public void getBlockTransactionsIncludesTransactionsWithSuccessBlockValidationResult() {
		// Assert:
		assertBlockValidationFiltering(ValidationResult.SUCCESS, Arrays.asList(1, 2, 3));
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsWithNeutralBlockValidationResult() {
		// Assert:
		assertBlockValidationFiltering(ValidationResult.NEUTRAL, Arrays.asList(1, 3));
	}

	@Test
	public void getBlockTransactionsExcludesTransactionsWithFailedBlockValidationResult() {
		// Assert:
		assertBlockValidationFiltering(ValidationResult.FAILURE_UNKNOWN, Arrays.asList(1, 3));
	}

	private static void assertBlockValidationFiltering(final ValidationResult validationResult, final List<Integer> expectedFilteredIds) {
		// Arrange:
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(validator));
		Mockito.when(validator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS, validationResult, ValidationResult.SUCCESS);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions();
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(expectedFilteredIds));
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any());
	}

	@Test
	public void getBlockTransactionsPassesCorrectBlockToBlockValidators() {
		// Arrange:
		final Account harvester = Utils.generateRandomAccount();
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(validator));
		Mockito.when(validator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.provider.getBlockTransactions(harvester.getAddress(), new TimeInstant(442), new BlockHeight(79));

		// Assert:
		final ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
		Mockito.verify(validator, Mockito.times(3)).validate(blockCaptor.capture());

		for (final Block block : blockCaptor.getAllValues()) {
			MatcherAssert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(harvester.getAddress()));
			MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(442)));
			MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(79)));
		}
	}

	// endregion

	// region revalidation checking - observer

	@Test
	public void getBlockTransactionsCallsObserversForAllValidTransactions() {
		// Arrange:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(observer));

		// Act:
		context.getBlockTransactions();

		// Assert:
		Mockito.verify(observer, Mockito.times(3)).notify(Mockito.any(), Mockito.any());
	}

	@Test
	public void getBlockTransactionsDoesNotCallObserversForTransactionsThatFailValidation() {
		// Arrange:
		final ProviderFactories factories = new ProviderFactories();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		factories.setObserver(observer);
		factories.setValidator(validator);

		final TestContext context = createContextWithThreeTransactions(factories);
		Mockito.when(validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS,
				ValidationResult.FAILURE_UNKNOWN, ValidationResult.SUCCESS);

		// Act:
		context.getBlockTransactions();

		// Assert:
		Mockito.verify(observer, Mockito.times(2)).notify(Mockito.any(), Mockito.any());
	}

	@Test
	public void getBlockTransactionsDoesNotCallObserversForTransactionsThatFailBlockValidation() {
		// Arrange:
		final ProviderFactories factories = new ProviderFactories();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		factories.setObserver(observer);
		factories.setBlockValidator(validator);

		final TestContext context = createContextWithThreeTransactions(factories);
		Mockito.when(validator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS, ValidationResult.FAILURE_UNKNOWN,
				ValidationResult.SUCCESS);

		// Act:
		context.getBlockTransactions();

		// Assert:
		Mockito.verify(observer, Mockito.times(2)).notify(Mockito.any(), Mockito.any());
	}

	@Test
	public void getBlockTransactionsPassesCorrectNotificationContextToObservers() {
		// Arrange:
		final Account harvester = Utils.generateRandomAccount();
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final TestContext context = createContextWithThreeTransactions(new ProviderFactories(observer));

		// Act:
		context.provider.getBlockTransactions(harvester.getAddress(), new TimeInstant(442), new BlockHeight(79));

		// Assert:
		final ArgumentCaptor<BlockNotificationContext> notificationContextCaptor = ArgumentCaptor.forClass(BlockNotificationContext.class);
		Mockito.verify(observer, Mockito.times(3)).notify(Mockito.any(), notificationContextCaptor.capture());

		for (final BlockNotificationContext notificationContext : notificationContextCaptor.getAllValues()) {
			MatcherAssert.assertThat(notificationContext.getTrigger(), IsEqual.equalTo(NotificationTrigger.Execute));
			MatcherAssert.assertThat(notificationContext.getTimeStamp(), IsEqual.equalTo(new TimeInstant(442)));
			MatcherAssert.assertThat(notificationContext.getHeight(), IsEqual.equalTo(new BlockHeight(79)));
		}
	}

	// endregion

	private static TestContext createContextWithThreeTransactions(final ProviderFactories factories) {
		// Arrange:
		final TestContext context = new TestContext(factories);
		final Account sender = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(new MockTransaction(sender, 1, new TimeInstant(4)),
				new MockTransaction(sender, 2, new TimeInstant(6)), new MockTransaction(sender, 3, new TimeInstant(8)));
		context.addTransactions(transactions);
		return context;
	}

	// region max transaction checking - no child transactions

	@Test
	public void getBlockTransactionsReturnsAllTransactionsWhenLessThanMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(5, 5);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenMoreThanMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(2 * MAX_TRANSACTIONS_PER_BLOCK, MAX_TRANSACTIONS_PER_BLOCK);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenExactlyMaximumTransactionsAreAvailable() {
		// Assert:
		assertNumTransactionsReturned(MAX_TRANSACTIONS_PER_BLOCK, MAX_TRANSACTIONS_PER_BLOCK);
	}

	private static void assertNumTransactionsReturned(final int numTransactions, final int numFilteredTransactions) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));
		context.addTransactions(account2, 6, 6 + numTransactions - 1);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues.size(), IsEqual.equalTo(numFilteredTransactions));
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(createIntRange(6, 6 + numFilteredTransactions)));
	}

	// endregion

	// region max transaction checking - child transactions

	@Test
	public void getBlockTransactionsReturnsLessThanMaximumTransactionsWhenLastTransactionAndChildrenCannotFit() {
		// 7 child transactions per transaction in the list, 120 / 7 == 17.14...
		assertNumTransactionsReturned(2 * MAX_TRANSACTIONS_PER_BLOCK, 6, 17);
	}

	@Test
	public void getBlockTransactionsReturnsMaximumTransactionsWhenLastTransactionAndChildrenCanFit() {
		// Assert:
		// 7 child transactions per transaction in the list, 120 / 8 == 15
		assertNumTransactionsReturned(2 * MAX_TRANSACTIONS_PER_BLOCK, 7, 15);
	}

	private static void assertNumTransactionsReturned(final int numTransactions, final int numChildTransactions,
			final int numFilteredTransactions) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));
		context.addTransactionsWithChildren(account2, 6, 6 + numTransactions - 1, numChildTransactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		MatcherAssert.assertThat(customFieldValues.size(), IsEqual.equalTo(numFilteredTransactions));
		MatcherAssert.assertThat(customFieldValues, IsEquivalent.equivalentTo(createIntRange(6, 6 + numFilteredTransactions)));

		final int numTotalTransactions = filteredTransactions.stream().mapToInt(t -> 1 + t.getChildTransactions().size()).sum();
		MatcherAssert.assertThat(numTotalTransactions, IsEqual.equalTo((numChildTransactions + 1) * numFilteredTransactions));
	}

	// endregion

	// region test utils

	private static List<TimeInstant> getTimeInstantsAsList(final Collection<Transaction> transactions) {
		return transactions.stream().map(Transaction::getTimeStamp).collect(Collectors.toList());
	}

	private static List<Integer> createIntRange(final int start, final int end) {
		return IntStream.range(start, end).mapToObj(i -> i).collect(Collectors.toList());
	}

	private static class ProviderFactories {
		private TransactionValidatorFactory validatorFactory;
		private BlockValidatorFactory blockValidatorFactory;
		private BlockTransactionObserverFactory observerFactory;

		public ProviderFactories() {
		}

		public ProviderFactories(final SingleTransactionValidator singleValidator) {
			this.setValidator(singleValidator);
		}

		public ProviderFactories(final BlockValidator validator) {
			this.setBlockValidator(validator);
		}

		public ProviderFactories(final BlockTransactionObserver observer) {
			this.setObserver(observer);
		}

		public static ProviderFactories createReal() {
			final ProviderFactories factories = new ProviderFactories();
			factories.validatorFactory = NisUtils.createTransactionValidatorFactory();
			factories.blockValidatorFactory = NisUtils.createBlockValidatorFactory();
			factories.observerFactory = new BlockTransactionObserverFactory();
			return factories;
		}

		public TransactionValidatorFactory getValidatorFactory() {
			if (null == this.validatorFactory) {
				this.setValidator((transaction, context) -> ValidationResult.SUCCESS);
			}

			return this.validatorFactory;
		}

		public BlockValidatorFactory getBlockValidatorFactory() {
			if (null == this.blockValidatorFactory) {
				this.setBlockValidator(block -> ValidationResult.SUCCESS);
			}

			return this.blockValidatorFactory;
		}

		public BlockTransactionObserverFactory getObserverFactory() {
			if (null == this.observerFactory) {
				this.setObserver((notification, context) -> {
				});
			}

			return this.observerFactory;
		}

		public void setValidator(final SingleTransactionValidator singleValidator) {
			this.validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			Mockito.when(this.validatorFactory.createSingle(Mockito.any())).thenReturn(singleValidator);
		}

		public void setBlockValidator(final BlockValidator validator) {
			this.blockValidatorFactory = Mockito.mock(BlockValidatorFactory.class);
			Mockito.when(this.blockValidatorFactory.createTransactionOnly()).thenReturn(validator);
		}

		public void setObserver(final BlockTransactionObserver observer) {
			this.observerFactory = Mockito.mock(BlockTransactionObserverFactory.class);
			Mockito.when(this.observerFactory.createExecuteCommitObserver(Mockito.any())).thenReturn(observer);
		}
	}

	private static class TestContext {
		protected final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final List<Transaction> transactions = new ArrayList<>();

		protected final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		protected final NewBlockTransactionsProvider provider;

		public TestContext() {
			this((transaction, context) -> ValidationResult.SUCCESS);
		}

		public TestContext(final SingleTransactionValidator singleValidator) {
			this(new ProviderFactories(singleValidator), new ForkConfiguration());
		}

		public TestContext(final ProviderFactories factories) {
			this(factories, new ForkConfiguration());
		}

		public TestContext(final ProviderFactories factories, final ForkConfiguration forkConfiguration) {
			Mockito.when(this.unconfirmedTransactions.getAll()).thenReturn(this.transactions);
			Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);

			// set up the nis copy
			final NisCache nisCacheCopy = Mockito.mock(NisCache.class);
			Mockito.when(nisCacheCopy.getAccountCache()).thenReturn(Mockito.mock(AccountCache.class));
			Mockito.when(nisCacheCopy.getAccountStateCache()).thenReturn(this.accountStateCache);
			Mockito.when(this.nisCache.copy()).thenReturn(nisCacheCopy);

			this.provider = new DefaultNewBlockTransactionsProvider(this.nisCache, factories.getValidatorFactory(),
					factories.getBlockValidatorFactory(), factories.getObserverFactory(), this.unconfirmedTransactions, forkConfiguration);
		}

		public List<Transaction> getBlockTransactions(final Account account, final TimeInstant timeInstant) {
			return this.provider.getBlockTransactions(account.getAddress(), timeInstant, new BlockHeight(1234));
		}

		public List<Transaction> getBlockTransactions(final Account account) {
			return this.getBlockTransactions(account, TimeInstant.ZERO);
		}

		public List<Transaction> getBlockTransactions(final Account account, final BlockHeight height) {
			return this.provider.getBlockTransactions(account.getAddress(), TimeInstant.ZERO, height);
		}

		public List<Transaction> getBlockTransactions() {
			return this.getBlockTransactions(Utils.generateRandomAccount());
		}

		public Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

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

		public void addTransactionsWithChildren(final Account signer, final int startCustomField, final int endCustomField,
				final int numChildren) {
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
	}

	// endregion
}
