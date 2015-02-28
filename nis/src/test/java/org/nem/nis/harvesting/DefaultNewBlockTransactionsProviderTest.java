package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.*;

public class DefaultNewBlockTransactionsProviderTest {
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
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1, currentTime);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

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
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

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
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1, new TimeInstant(3601));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(1, 2, 3, 4)));
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
				new TransferTransaction(new TimeInstant(1), account1, account2, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(3), account1, account2, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(4), account2, account1, Amount.fromNem(99), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions();
		final List<TimeInstant> timeInstants = getTimeInstantsAsList(filteredTransactions);

		// Assert:
		Assert.assertThat(timeInstants, IsEquivalent.equivalentTo(Arrays.asList(new TimeInstant(2), new TimeInstant(3))));
	}

	//endregion

	//region revalidation checking

	//region transaction

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
		assertTransactionValidationFiltering(ValidationResult.FAILURE_ENTITY_UNUSABLE, Arrays.asList(1, 3));
	}

	private static void assertTransactionValidationFiltering(
			final ValidationResult validationResult,
			final List<Integer> expectedFilteredIds) {
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
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(expectedFilteredIds));
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any(), Mockito.any());
	}

	//endregion

	//region block

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
		assertBlockValidationFiltering(ValidationResult.FAILURE_ENTITY_UNUSABLE, Arrays.asList(1, 3));
	}

	private static void assertBlockValidationFiltering(
			final ValidationResult validationResult,
			final List<Integer> expectedFilteredIds) {
		// Arrange:
		final BlockValidator validator = Mockito.mock(BlockValidator.class);
		final TestContext context = new TestContext(new ProviderFactories(validator));

		final Account account1 = context.addAccount(Amount.fromNem(100));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<MockTransaction> transactions = Arrays.asList(
				new MockTransaction(account2, 1, new TimeInstant(4)),
				new MockTransaction(account2, 2, new TimeInstant(6)),
				new MockTransaction(account2, 3, new TimeInstant(8)));
		context.addTransactions(transactions);
		Mockito.when(validator.validate(Mockito.any())).then(invocationOnMock -> {
			final Block block = (Block)invocationOnMock.getArguments()[0];
			final TimeInstant lastTimeStamp = block.getTransactions().get(block.getTransactions().size() - 1).getTimeStamp();
			return 0 == lastTimeStamp.compareTo(new TimeInstant(6))
					? validationResult
					: ValidationResult.SUCCESS;
		});

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(expectedFilteredIds));
		Mockito.verify(validator, Mockito.times(3)).validate(Mockito.any());
	}

	//endregion

	//region observer

	//endregion

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
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));
		context.addTransactions(account2, 6, 6 + numTransactions - 1);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

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
		final Account account1 = context.addAccount(Amount.fromNem(1000));
		final Account account2 = context.addAccount(Amount.fromNem(1000));
		context.addTransactionsWithChildren(account2, 6, 6 + numTransactions - 1, numChildTransactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

		// Assert:
		Assert.assertThat(customFieldValues.size(), IsEqual.equalTo(numFilteredTransactions));
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(createIntRange(6, 6 + numFilteredTransactions)));

		final int numTotalTransactions = filteredTransactions.stream().mapToInt(t -> 1 + t.getChildTransactions().size()).sum();
		Assert.assertThat(numTotalTransactions, IsEqual.equalTo((numChildTransactions + 1) * numFilteredTransactions));
	}

	//endregion

	//endregion

	//region real validators

	//region transfers

	@Test
	public void getBlockTransactionsAllowsEarlierBlockTransfersToBeSpentLater() {
		// Arrange:
		final TestContext context = new TestContext(ProviderFactories.createReal());
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// - T(O) - S: 20 | R: 10
		// - T(1) - S -15-> R | S: 03 | R: 25
		// - T(2) - R -12-> S | S: 15 | R: 10 # this transfer is allowed even though it wouldn't be allowed in reverse order
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t1.setFee(Amount.fromNem(2));
		t1.sign();
		context.addTransaction(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t2.setFee(Amount.fromNem(3));
		t2.sign();
		context.addTransaction(t2);

		// Act:
		final List<Transaction> filtered = context.getBlockTransactions(currentTime.addSeconds(1));

		// Assert:
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t1, t2)));
	}

	@Test
	public void getBlockTransactionsExcludesTransactionFromNextBlockIfConfirmedBalanceIsInsufficient() {
		// Arrange:
		final TestContext context = new TestContext(ProviderFactories.createReal());
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// - T(O) - S: 20 | R: 10
		// - T(1) - R -12-> S | XXX
		// - T(2) - S -15-> R | S: 03 | R: 25
		final Transaction t1 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		context.addTransaction(t1);
		final Transaction t2 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t2.setFee(Amount.fromNem(2));
		t2.sign();
		context.addTransaction(t2);

		// Act:
		final List<Transaction> filtered = context.getBlockTransactions(currentTime.addSeconds(1));

		// Assert:
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t2)));
	}

	//endregion

	//region importance transfer

	@Test
	public void getBlockTransactionsDoesNotAllowConflictingImportanceTransfersToBeInSingleBlock() {
		// Arrange:
		final TestContext context = new TestContext(ProviderFactories.createReal());
		final Account sender = context.addAccount(Amount.fromNem(50000));
		final Account remote = context.addAccount(Amount.ZERO);

		final Transaction t1 = createActivateImportanceTransfer(sender, remote);
		final Transaction t2 = createActivateImportanceTransfer(sender, remote);
		context.addTransactions(Arrays.asList(t1, t2));

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(blockTransactions, IsEqual.equalTo(Arrays.asList(t1)));
	}

	private static Transaction createActivateImportanceTransfer(final Account sender, final Account remote) {
		final Transaction transaction = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransaction.Mode.Activate, remote);
		transaction.setDeadline(transaction.getTimeStamp().addMinutes(1));
		return transaction;
	}

	//endregion

	//region multisig modification

	@Test
	public void getBlockTransactionsDoesNotAllowMultipleMultisigModificationsForSameAccountToBeInSingleBlock() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account cosigner3 = context.addAccount(Amount.ZERO);
		context.makeCosignatory(cosigner1, multisig);

		final Transaction t1 = createModification(cosigner1, multisig, cosigner2);
		final Transaction t2 = createModification(cosigner1, multisig, cosigner3);
		context.addTransactions(Arrays.asList(t1, t2));

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(blockTransactions, IsEqual.equalTo(Arrays.asList(t1)));
	}

	private static Transaction createModification(
			final Account cosigner,
			final Account multisig,
			final Account newCosigner) {
		final Transaction transaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosigner)));
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		return createMultisig(cosigner, transaction);
	}

	//endregion

	//region multisig

	@Test
	public void getBlockTransactionsDoesNotReturnMultisigTransactionIfMultisigSignaturesAreNotPresent() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.makeCosignatory(cosigner1, multisig);
		context.makeCosignatory(cosigner2, multisig);

		final Transaction t1 = createTransferTransaction(TimeInstant.ZERO, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		context.addTransaction(mt1);

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getBlockTransactionsReturnsMultisigTransactionIfMultisigSignaturesArePresent() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = context.addAccount(Amount.fromNem(2000));
		final Account cosigner1 = context.addAccount(Amount.ZERO);
		final Account cosigner2 = context.addAccount(Amount.ZERO);
		final Account recipient = context.addAccount(Amount.ZERO);

		context.makeCosignatory(cosigner1, multisig);
		context.makeCosignatory(cosigner2, multisig);

		final Transaction t1 = createTransferTransaction(TimeInstant.ZERO, multisig, recipient, Amount.fromNem(7));
		final MultisigTransaction mt1 = createMultisig(cosigner1, t1);
		mt1.addSignature(createSignature(cosigner2, multisig, t1));
		context.addTransaction(mt1);

		// Act:
		final List<Transaction> blockTransactions = context.getBlockTransactions();

		// Assert:
		Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(blockTransactions, IsEquivalent.equivalentTo(mt1));
	}

	private static MultisigTransaction createMultisig(final Account cosigner, final Transaction innerTransaction) {
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, cosigner, innerTransaction);
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		return transaction;
	}

	private static MultisigSignatureTransaction createSignature(final Account cosigner, final Account multisig, final Transaction innerTransaction) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(TimeInstant.ZERO, cosigner, multisig, innerTransaction);
		transaction.setDeadline(TimeInstant.ZERO.addMinutes(1));
		return transaction;
	}

	private static class MultisigTestContext extends TestContext {

		public MultisigTestContext() {
			super(ProviderFactories.createReal());
		}

		public void makeCosignatory(final Account cosigner, final Account multisig) {
			this.accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
		}
	}

	//endregion

	//endregion

	//region test utils

	private static List<TimeInstant> getTimeInstantsAsList(final Collection<Transaction> transactions) {
		return transactions.stream()
				.map(Transaction::getTimeStamp)
				.collect(Collectors.toList());
	}

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
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
				this.setObserver((notification, context) -> { });
			}

			return this.observerFactory;
		}

		public void setValidator(final SingleTransactionValidator singleValidator) {
			this.validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			Mockito.when(this.validatorFactory.createSingle(Mockito.any())).thenReturn(singleValidator);
		}

		public void setBlockValidator(final BlockValidator validator) {
			this.blockValidatorFactory = Mockito.mock(BlockValidatorFactory.class);
			Mockito.when(this.blockValidatorFactory.createTransactionOnly(Mockito.any())).thenReturn(validator);
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

		private TestContext(final SingleTransactionValidator singleValidator) {
			this(new ProviderFactories(singleValidator));
		}

		private TestContext(final ProviderFactories factories) {
			Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);

			// set up the nis copy
			final NisCache nisCacheCopy = Mockito.mock(NisCache.class);
			Mockito.when(nisCacheCopy.getAccountCache()).thenReturn(Mockito.mock(AccountCache.class));
			Mockito.when(nisCacheCopy.getAccountStateCache()).thenReturn(this.accountStateCache);
			Mockito.when(this.nisCache.copy()).thenReturn(nisCacheCopy);

			this.provider = new DefaultNewBlockTransactionsProvider(
					this.nisCache,
					factories.getValidatorFactory(),
					factories.getBlockValidatorFactory(),
					factories.getObserverFactory(),
					this.unconfirmedTransactions);
		}

		public List<Transaction> getBlockTransactions(final Account account, final TimeInstant timeInstant) {
			return this.provider.getBlockTransactions(account.getAddress(), timeInstant, new BlockHeight(10));
		}

		public List<Transaction> getBlockTransactions(final Account account) {
			return this.getBlockTransactions(account, TimeInstant.ZERO);
		}

		public List<Transaction> getBlockTransactions(final TimeInstant timeInstant) {
			return this.getBlockTransactions(Utils.generateRandomAccount(), timeInstant);
		}

		public List<Transaction> getBlockTransactions() {
			return this.getBlockTransactions(Utils.generateRandomAccount());
		}

		//region addAccount

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

	//endregion
}