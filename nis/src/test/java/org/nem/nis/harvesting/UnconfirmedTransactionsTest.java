package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;

import java.util.*;
import java.util.function.Function;

public class UnconfirmedTransactionsTest {

	@Before
	public void setup() {
		Utils.setupGlobals();
	}

	@After
	public void destroy() {
		Utils.resetGlobals();
	}

	@Test
	public void addAllowsConflictingImportanceTransferTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = context.addAccount(Amount.fromNem(50000));
		final Account remote = context.addAccount(Amount.fromNem(100));

		final Transaction t1 = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferMode.Activate, remote);
		final Transaction t2 = new ImportanceTransferTransaction(new TimeInstant(1), sender, ImportanceTransferMode.Activate, remote);
		context.signAndAddExisting(t1);

		// Act:
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(2));
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext(createBalanceValidator());
		final Account sender = context.addAccount(Amount.fromNem(10));

		final MockTransaction t1 = new MockTransaction(sender);
		t1.setFee(Amount.fromNem(6));
		context.signAndAddExisting(t1);

		// Act:
		final MockTransaction t2 = new MockTransaction(sender);
		t2.setFee(Amount.fromNem(5));
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	@Test
	public void addFailsIfSenderHasInsufficientUnconfirmedMosaicBalance() {
		// Arrange:
		final TestContext context = new TestContext(createMosaicBalanceValidator());
		final MosaicId mosaicId1 = Utils.createMosaicId(1);
		final Account sender = context.addAccount(Amount.fromNem(100), mosaicId1, Supply.fromValue(10));
		final Account recipient = context.addAccount(Amount.fromNem(100));
		final TimeInstant currentTime = new TimeInstant(11);
		final Transaction t1 = new TransferTransaction(currentTime, sender, recipient, Amount.fromNem(1), createAttachment(mosaicId1, new Quantity(5_000)));
		final Transaction t2 = new TransferTransaction(currentTime, sender, recipient, Amount.fromNem(1), createAttachment(mosaicId1, new Quantity(6_000)));
		setFeeAndDeadline(t1, Amount.fromNem(20));
		setFeeAndDeadline(t2, Amount.fromNem(20));
		context.signAndAddExisting(t1);

		// Act:
		final ValidationResult result = context.signAndAddExisting(t2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(context.transactions.size(), IsEqual.equalTo(1));
	}

	//region removeAll

	@Test
	public void removeAllRemovesAllTransactionsInBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.addMockTransactions(context.transactions, 6, 9);

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(3));

		// Act:
		context.transactions.removeAll(block.getTransactions());
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.asFilter().getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(6, 8)));
	}

	@Test
	public void removeAllDoesUndoTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.addMockTransactions(context.transactions, 6, 9);

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(1));
		block.addTransaction(transactions.get(3));

		// Act:
		context.transactions.removeAll(block.getTransactions());

		// Assert:
		// not the greatest test, but the count is 2 for the removed transactions and 1 for the others
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(2));
	}

	@Test
	public void removeAllRebuildsCacheIfIllegalArgumentExceptionOccurs() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(createBalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(50));

		final Block block = NisUtils.createRandomBlock();
		block.addTransaction(transactions.get(0));

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.removeAll(block.getTransactions());

		// Assert:
		// - removing the first transaction triggers an exception and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (50) < 80 + 2
		// - second transaction cannot be added - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
	}

	@Test
	public void removeAllRebuildsCacheIfInvalidXemTransferInCacheIsDetected() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(createBalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 20, 0);

		final Block block = NisUtils.createRandomBlock();
		final TransferTransaction transaction = context.createTransferTransaction(
				transactions.get(0).getSigner(),
				transactions.get(0).getRecipient(),
				Amount.fromNem(8),
				new TimeInstant(8));
		block.addTransaction(transaction);

		// Act:
		final int numTransactions = context.transactions.size();

		// Before the call to removeAll the transaction contained in the block is usually executed (which
		// will change the confirmed balance) and thus account1 is debited 80 + 2 NEM and account2 is credited 80 NEM
		context.setBalance(transactions.get(0).getSigner(), Amount.fromNem(18));
		context.setBalance(transactions.get(1).getSigner(), Amount.fromNem(100));
		context.transactions.removeAll(block.getTransactions());

		// Assert:
		// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
		// - first transaction cannot be added - account1 balance (18) < 80 + 2
		// - second transaction can be added - account2 balance (100) >= 50 + 2
		// - third transaction can be added - account2 balance (48) >= 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
	}

	@Test
	public void removeAllRebuildsCacheIfInvalidMosaicTransferInCacheIsDetected() {
		// Arrange:
		// 1 -> 2 80 mosaic1 | 2 -> 3 50 mosaic2 | 2 -> 3 10 mosaic2
		final TestContext context = new TestContext(createMosaicBalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeMosaicTransferTransactions(100, 60);

		final Block block = NisUtils.createRandomBlock();
		final TransferTransaction transaction = context.createTransferTransaction(
				transactions.get(0).getSigner(),
				transactions.get(0).getRecipient(),
				Amount.fromNem(8),
				new TimeInstant(8));
		block.addTransaction(transaction);

		// Act:
		final int numTransactions = context.transactions.size();

		// Decreasing the supply makes first transaction invalid
		context.decreaseSupply(Utils.createMosaicId(1), Supply.fromValue(25));
		context.transactions.removeAll(block.getTransactions());

		// Assert:
		// - after call to removeAll the first transaction in the list is invalid and forces a cache rebuild
		// - first transaction cannot be added - account1 mosaic 1 balance (75) < 80
		// - second transaction can be added - account2 mosaic 2 balance (60) >= 50
		// - third transaction can be added - account2 mosaic 2 balance (10) >= 10
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Arrays.asList(transactions.get(1), transactions.get(2))));
	}

	//endregion

	//region dropExpiredTransactions

	@Test
	public void dropExpiredTransactionsRemovesAllTransactionsBeforeSpecifiedTimeInstant() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));
		final List<Integer> customFieldValues = MockTransactionUtils.getCustomFieldValues(context.transactions.asFilter().getAll());

		// Assert:
		Assert.assertThat(customFieldValues, IsEquivalent.equivalentTo(Arrays.asList(7, 9)));
	}

	@Test
	public void dropExpiredTransactionsExecutesAllNonExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<MockTransaction> transactions = context.createMockTransactions(6, 9);
		transactions.get(0).setDeadline(new TimeInstant(5));
		transactions.get(1).setDeadline(new TimeInstant(7));
		transactions.get(2).setDeadline(new TimeInstant(6));
		transactions.get(3).setDeadline(new TimeInstant(8));
		transactions.forEach(context::signAndAddExisting);

		// Act:
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		Assert.assertThat(transactions.get(0).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(1).getNumTransferCalls(), IsEqual.equalTo(2));
		Assert.assertThat(transactions.get(2).getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(3).getNumTransferCalls(), IsEqual.equalTo(2));
	}

	@Test
	public void dropExpiredTransactionsDropsAllTransactionsThatAreDependentOnTheDroppedTransactions() {
		// Arrange:
		// 1 -> 2 (80A + 2F)NEM @ 5T | 2 -> 3 (50A + 2F)NEM @ 8T | 2 -> 3 (10A + 2F)NEM @ 9T
		final TestContext context = new TestContext(createBalanceValidator());
		final List<TransferTransaction> transactions = context.createThreeTransferTransactions(100, 12, 0);

		// Act:
		final int numTransactions = context.transactions.size();
		context.transactions.dropExpiredTransactions(new TimeInstant(7));

		// Assert:
		// - first transaction was dropped because it expired
		// - second was dropped because it was dependent on the first - account2 balance (12) < 50 + 2
		// - third transaction can be added - account2 balance (12) == 10 + 2
		Assert.assertThat(numTransactions, IsEqual.equalTo(3));
		Assert.assertThat(context.transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(transactions.get(2))));
	}

	//endregion

	//region tests with real validator

	@Test
	public void checkingUnconfirmedTransactionsDisallowsAddingDoubleSpendTransactions() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final UnconfirmedTransactions transactions = context.transactions;
		final Account sender = context.addAccount(Amount.fromNem(10));
		final Account recipient = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		final Transaction t1 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(7));
		t1.sign();
		final ValidationResult result1 = transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(currentTime.addSeconds(-1), sender, recipient, Amount.fromNem(7));
		t2.sign();
		final ValidationResult result2 = transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(t1)));
	}

	@Test
	public void checkingUnconfirmedMosaicTransactionsDisallowsAddingDoubleSpendTransactions() {
		// Arrange:
		final TestContext context = createUnconfirmedTransactionsWithRealValidator();
		final UnconfirmedTransactions transactions = context.transactions;
		final Account sender = context.addAccount(Amount.fromNem(500), Utils.createMosaicId(1), Supply.fromValue(10));
		final Account recipient = context.addAccount();
		final TimeInstant currentTime = new TimeInstant(11);

		// Act:
		final Transaction t1 = createTransferTransaction(
				2,
				currentTime,
				sender,
				recipient,
				Amount.fromNem(1),
				createAttachment(Utils.createMosaicId(1), Quantity.fromValue(7_000)));
		t1.setFee(Amount.fromNem(200));
		t1.sign();
		final ValidationResult result1 = transactions.addExisting(t1);
		final Transaction t2 = createTransferTransaction(
				2,
				currentTime.addSeconds(-1),
				sender,
				recipient,
				Amount.fromNem(1),
				createAttachment(Utils.createMosaicId(1), Quantity.fromValue(7_000)));
		t2.setFee(Amount.fromNem(200));
		t2.sign();
		final ValidationResult result2 = transactions.addExisting(t2);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(result2, IsEqual.equalTo(ValidationResult.FAILURE_INSUFFICIENT_BALANCE));
		Assert.assertThat(transactions.asFilter().getAll(), IsEqual.equalTo(Collections.singletonList(t1)));
	}

	//endregion

	private static TestContext createUnconfirmedTransactionsWithRealValidator() {
		return createUnconfirmedTransactionsWithRealValidator(Mockito.mock(AccountStateCache.class));
	}

	private static TestContext createUnconfirmedTransactionsWithRealValidator(final AccountStateCache stateCache) {
		final TransactionValidatorFactory factory = NisUtils.createTransactionValidatorFactory(new SystemTimeProvider());
		return new TestContext(
				factory::createSingleBuilder,
				null,
				factory.createBatch(Mockito.mock(DefaultHashCache.class)),
				stateCache,
				Mockito.mock(ReadOnlyPoiFacade.class),
				Mockito.mock(ReadOnlyNamespaceCache.class));
	}

	private static BalanceValidator createBalanceValidator() {
		return new BalanceValidator();
	}

	private static MosaicBalanceValidator createMosaicBalanceValidator() {
		return new MosaicBalanceValidator();
	}

	public static TransferTransaction createTransferTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount) {
		return createTransferTransaction(1, timeStamp, sender, recipient, amount, null);
	}

	public static TransferTransaction createTransferTransaction(
			final int version,
			final TimeInstant timeStamp,
			final Account sender,
			final Account recipient,
			final Amount amount,
			final TransferTransactionAttachment attachment) {
		final TransferTransaction transferTransaction = new TransferTransaction(version, timeStamp, sender, recipient, amount, attachment);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	private static TransferTransactionAttachment createAttachment(final MosaicId mosaicId, final Quantity quantity) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, quantity);
		return attachment;
	}

	private static void setFeeAndDeadline(final Transaction transaction, final Amount fee) {
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(10));
		transaction.setFee(fee);
	}

	private static class TestContext {
		private final SingleTransactionValidator singleValidator;
		private final BatchTransactionValidator batchValidator;
		private final UnconfirmedTransactions transactions;
		private final ReadOnlyNisCache nisCache;
		private final Map<MosaicId, MosaicEntry> mosaicMap = new HashMap<>();

		private TestContext() {
			this(Mockito.mock(SingleTransactionValidator.class), Mockito.mock(BatchTransactionValidator.class));
			this.setSingleValidationResult(ValidationResult.SUCCESS);
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext(final SingleTransactionValidator singleValidator) {
			this(singleValidator, Mockito.mock(BatchTransactionValidator.class));
			this.setBatchValidationResult(ValidationResult.SUCCESS);
		}

		private TestContext(final SingleTransactionValidator singleValidator, final BatchTransactionValidator batchValidator) {
			this(
					null,
					singleValidator,
					batchValidator,
					Mockito.mock(ReadOnlyAccountStateCache.class),
					Mockito.mock(ReadOnlyPoiFacade.class),
					Mockito.mock(ReadOnlyNamespaceCache.class));
		}

		private TestContext(
				final Function<ReadOnlyNisCache, AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier,
				final SingleTransactionValidator singleValidator,
				final BatchTransactionValidator batchValidator,
				final ReadOnlyAccountStateCache accountStateCache,
				final ReadOnlyPoiFacade poiFacade,
				final ReadOnlyNamespaceCache namespaceCache) {
			this.singleValidator = singleValidator;
			this.batchValidator = batchValidator;

			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
			Mockito.when(validatorFactory.createBatch(transactionHashCache)).thenReturn(this.batchValidator);

			this.nisCache = NisCacheFactory.createReadOnly(accountStateCache, transactionHashCache, poiFacade, namespaceCache);

			if (null != singleTransactionBuilderSupplier) {
				this.setSingleTransactionBuilderSupplier(validatorFactory, singleTransactionBuilderSupplier);
			} else {
				this.setSingleTransactionBuilderSupplier(validatorFactory, nisCache -> {
					final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
					builder.add(this.singleValidator);
					return builder;
				});
			}

			final TimeProvider timeProvider = Utils.createMockTimeProvider(TimeInstant.ZERO.getRawTime());
			final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
					NisUtils.createTransactionValidatorFactory(timeProvider),
					cache -> (notification, context) -> { },
					timeProvider,
					BlockHeight.MAX::prev);
			this.transactions = new DefaultUnconfirmedTransactions(factory, this.nisCache);
		}

		private void setSingleTransactionBuilderSupplier(
				final TransactionValidatorFactory validatorFactory,
				final Function<ReadOnlyNisCache, AggregateSingleTransactionValidatorBuilder> singleTransactionBuilderSupplier) {
			Mockito.when(validatorFactory.createSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.apply(this.nisCache));
			Mockito.when(validatorFactory.createIncompleteSingleBuilder(Mockito.any()))
					.then((invocationOnMock) -> singleTransactionBuilderSupplier.apply(this.nisCache));
		}

		private void setSingleValidationResult(final ValidationResult result) {
			Mockito.when(this.singleValidator.validate(Mockito.any(), Mockito.any())).thenReturn(result);
		}

		private void setBatchValidationResult(final ValidationResult result) {
			Mockito.when(this.batchValidator.validate(Mockito.any())).thenReturn(result);
		}

		private ValidationResult signAndAddExisting(final Transaction transaction) {
			transaction.sign();
			return this.transactions.addExisting(transaction);
		}

		private ValidationResult signAndAddNew(final Transaction transaction) {
			transaction.sign();
			return this.transactions.addNew(transaction);
		}

		private Account addAccount() {
			return this.addAccount(Amount.ZERO);
		}

		private Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		private Account addAccount(final Amount amount, final MosaicId mosaicId, final Supply supply) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount, mosaicId, supply);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

		public Account prepareAccount(final Account account, final Amount amount, final MosaicId mosaicId, final Supply supply) {
			this.prepareAccount(account, amount);
			final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
			final Mosaics mosaics = Mockito.mock(Mosaics.class);
			final MosaicEntry mosaicEntry = new MosaicEntry(Utils.createMosaicDefinition(account, mosaicId, Utils.createMosaicProperties()), supply);
			this.mosaicMap.put(mosaicId, mosaicEntry);
			Mockito.when(this.nisCache.getNamespaceCache().isActive(Mockito.any(), Mockito.any())).thenReturn(true);
			Mockito.when(this.nisCache.getNamespaceCache().get(mosaicId.getNamespaceId())).thenReturn(namespaceEntry);
			Mockito.when(namespaceEntry.getMosaics()).thenReturn(mosaics);
			Mockito.when(mosaics.get(mosaicId)).thenReturn(mosaicEntry);
			return account;
		}

		private List<MockTransaction> createMockTransactions(final int startCustomField, final int endCustomField) {
			final List<MockTransaction> transactions = new ArrayList<>();

			for (int i = startCustomField; i <= endCustomField; ++i) {
				transactions.add(this.createMockTransaction(Utils.generateRandomAccount(), new TimeInstant(i), i));
			}

			return transactions;
		}

		private MockTransaction createMockTransaction(final Account account, final TimeInstant timeStamp, final int customField) {
			this.prepareAccount(account, Amount.fromNem(1000));
			final MockTransaction transaction = new MockTransaction(account, customField, timeStamp);
			transaction.setFee(Amount.fromNem(customField));
			return transaction;
		}

		private List<MockTransaction> addMockTransactions(
				final UnconfirmedTransactions unconfirmedTransactions,
				final int startCustomField,
				final int endCustomField) {
			final List<MockTransaction> transactions = this.createMockTransactions(startCustomField, endCustomField);
			transactions.forEach(Transaction::sign);
			transactions.forEach(unconfirmedTransactions::addExisting);
			return transactions;
		}

		public TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TimeInstant deadline) {
			return this.createTransferTransaction(sender, recipient, amount, deadline, null);
		}

		public TransferTransaction createTransferTransaction(
				final Account sender,
				final Account recipient,
				final Amount amount,
				final TimeInstant deadline,
				final TransferTransactionAttachment attachment) {
			final TransferTransaction transaction = new TransferTransaction(deadline, sender, recipient, amount, attachment);
			transaction.setFee(Amount.fromNem(1));
			transaction.setDeadline(deadline);
			return transaction;
		}

		public List<TransferTransaction> createThreeTransferTransactions(
				final int amount1,
				final int amount2,
				final int amount3) {
			final Account account1 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount1));
			final Account account2 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount2));
			final Account account3 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(amount3));
			final List<TransferTransaction> transactions = new ArrayList<>();
			transactions.add(this.createTransferTransaction(account1, account2, Amount.fromNem(80), new TimeInstant(5)));
			transactions.add(this.createTransferTransaction(account2, account3, Amount.fromNem(50), new TimeInstant(8)));
			transactions.add(this.createTransferTransaction(account2, account3, Amount.fromNem(10), new TimeInstant(9)));
			transactions.forEach(this::signAndAddExisting);
			return transactions;
		}

		public void setBalance(final Account account, final Amount amount) {
			this.prepareAccount(account, amount);
		}

		public List<TransferTransaction> createThreeMosaicTransferTransactions(final int supply1, final int supply2) {
			final MosaicId[] mosaicIds = new MosaicId[] { Utils.createMosaicId(1), Utils.createMosaicId(2) };
			final Account account1 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100), mosaicIds[0], Supply.fromValue(supply1));
			final Account account2 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100), mosaicIds[1], Supply.fromValue(supply2));
			final Account account3 = this.prepareAccount(Utils.generateRandomAccount(), Amount.fromNem(100));
			final List<TransferTransaction> transactions = new ArrayList<>();
			transactions.add(this.createTransferTransaction(
					account1,
					account2,
					Amount.fromNem(1),
					new TimeInstant(5),
					createAttachment(mosaicIds[0], Quantity.fromValue(80_000))));
			transactions.add(this.createTransferTransaction(
					account2,
					account3,
					Amount.fromNem(1),
					new TimeInstant(8),
					createAttachment(mosaicIds[1], Quantity.fromValue(50_000))));
			transactions.add(this.createTransferTransaction(
					account2,
					account3,
					Amount.fromNem(1),
					new TimeInstant(9),
					createAttachment(mosaicIds[1], Quantity.fromValue(10_000))));
			transactions.forEach(this::signAndAddExisting);
			return transactions;
		}

		private void decreaseSupply(final MosaicId mosaicId, final Supply supply) {
			final MosaicEntry mosaicEntry = this.mosaicMap.get(mosaicId);
			mosaicEntry.decreaseSupply(supply);
		}
	}
}
