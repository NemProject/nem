package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;

import java.util.*;
import java.util.stream.*;

public class NewBlockTransactionsProviderV1Test {
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
		final TestContext context = new TestContext(new BalanceValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.getBlockTransactions();
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
		final List<Transaction> filteredTransactions = context.getBlockTransactions(account1);
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(filteredTransactions);

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
	public void getTransactionsForNewBlockFiltersOutConflictingTransactions() {
		// Arrange:
		final TestContext context = new TestContext(NisUtils.createTransactionValidatorFactory());
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
		// 		- initially the balances are: S = 20, R = 10
		// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 3, R = 25, 2 Fee
		// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 15, R = 10, 5 Fee
		//   - getTransactionsBefore() returns SORTED transactions, so R->S is ordered before S->R because it has a greater fee
		//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough balance
		//
		// However, this and test and one I'm gonna add below, should reject R's transaction for the following reason:
		// R doesn't have funds on the account, we don't want such TX because this would lead to creation
		// of a block that would get discarded (TXes are validated first, and then executed)

		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t1.setFee(Amount.fromNem(2));
		t1.sign();
		context.addTransaction(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
		t2.setFee(Amount.fromNem(3));
		t2.sign();
		context.addTransaction(t2);

		final List<Transaction> filtered = context.getBlockTransactions(currentTime.addSeconds(1));

		// Assert:
		// note: this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t1)));
	}

	@Test
	public void transactionIsExcludedFromNextBlockIfConfirmedBalanceIsInsufficient() {
		// Arrange:
		final TestContext context = new TestContext(NisUtils.createTransactionValidatorFactory());
		final Account sender = context.addAccount(Amount.fromNem(20));
		final Account recipient = context.addAccount(Amount.fromNem(10));
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		//   - add two txes, S->R, R->S, adding should succeed as it is done in proper order
		// 		- initially the balances are: S = 20, R = 10
		// 		- after "first" transaction is added, the (unconfirmed) balances are: S = 2, R = 25, 3 Fee
		// 		- after the "second" transaction is added, the (unconfirmed) balances are: S = 24, R = 1, 5 Fee
		//   - getTransactionsBefore() returns SORTED transactions, so S->R is ordered before R->S because it has a greater fee
		//   - than during removal, R->S should be rejected, BECAUSE R doesn't have enough *confirmed* balance
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
		t1.setFee(Amount.fromNem(3));
		t1.sign();
		context.addTransaction(t1);
		final Transaction t2 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(22));
		t2.setFee(Amount.fromNem(2));
		t2.sign();
		context.addTransaction(t2);

		final List<Transaction> filtered = context.getBlockTransactions(currentTime.addSeconds(1));

		// Assert:
		// - this checks that both TXes have been added and that returned TXes are in proper order
		// - the filtered transactions only contain first because transaction validation uses real "confirmed" balance
		Assert.assertThat(filtered, IsEqual.equalTo(Arrays.asList(t1)));
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
			super(NisUtils.createTransactionValidatorFactory());
		}

		public void makeCosignatory(final Account cosigner, final Account multisig) {
			this.accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
			this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
		}
	}

	//endregion

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

	private static class TestContext {
		protected final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final TransactionValidatorFactory validatorFactory;
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final List<Transaction> transactions = new ArrayList<>();

		protected final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		protected final NewBlockTransactionsProvider provider;

		public TestContext() {
			this((transaction, context) -> ValidationResult.SUCCESS);
		}

		private TestContext(final SingleTransactionValidator singleValidator) {
			this(createMockValidatorFactory(singleValidator));
		}

		private static TransactionValidatorFactory createMockValidatorFactory(final SingleTransactionValidator singleValidator) {
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			Mockito.when(validatorFactory.createSingle(Mockito.any())).thenReturn(singleValidator);
			return validatorFactory;
		}

		private TestContext(final TransactionValidatorFactory validatorFactory) {
			this.validatorFactory = validatorFactory;
			Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);

			this.provider = new NewBlockTransactionsProviderV1(
					this.nisCache,
					this.validatorFactory,
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