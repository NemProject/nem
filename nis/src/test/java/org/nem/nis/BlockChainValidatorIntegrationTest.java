package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.service.BlockExecutor;
import org.nem.nis.state.*;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * This suite is different from BlockChainValidatorTest because it uses REAL validators
 * (or at least very close to real)
 * TODO 20140129 J-*: we might want to consider having the validator to return a validation result instead of boolean
 */
public class BlockChainValidatorIntegrationTest {

	@Test
	public void allBlocksInChainMustHaveValidTimestamp() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block = createFutureBlock(blocks.get(2));
		blocks.add(block);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allTransactionsInChainMustBeValid() {
		// Assert:
		assertChainWithSingleInvalidTransactionIsInvalid(BlockChainValidatorFactory::createInvalidSignedTransaction);
	}

	@Test
	public void allTransactionsInChainMustHaveValidTimestamp() {
		// Assert:
		assertChainWithSingleInvalidTransactionIsInvalid(BlockChainValidatorFactory::createSignedFutureTransaction);
	}

	@Test
	public void chainWithTransactionWithInsufficientFeeIsInvalid() {
		// Assert:
		assertChainWithSingleInvalidTransactionIsInvalid(factory -> {
			final Transaction transaction = factory.createValidSignedTransaction();
			transaction.setFee(Amount.fromNem(1));
			transaction.sign();
			return transaction;
		});
	}

	private static void assertChainWithSingleInvalidTransactionIsInvalid(final Function<BlockChainValidatorFactory, Transaction> creteInvalidTransaction) {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(factory.createValidSignedTransaction());
		block.addTransaction(creteInvalidTransaction.apply(factory));
		block.addTransaction(factory.createValidSignedTransaction());
		block.sign();

		// Act:
		final boolean isValid = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(false));
	}

	@Test
	public void chainWithValidTransactionsIsValid() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(factory.createValidSignedTransaction());
		block.addTransaction(factory.createValidSignedTransaction());
		block.addTransaction(factory.createValidSignedTransaction());
		block.sign();

		// Assert:
		final boolean isValid = validator.isValid(parentBlock, blocks);
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}

	@Test
	public void chainWithImportanceTransferToNonZeroBalanceAccountIsInvalid() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 123);
		parentBlock.sign();

		final Account account1 = Utils.generateRandomAccount();
		factory.getAccountInfo(account1).incrementBalance(Amount.fromNem(12345));
		final Account account2 = Utils.generateRandomAccount();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		final Transaction transaction1 = new TransferTransaction(new TimeInstant(100), account1, account2, new Amount(7), null);
		transaction1.setDeadline(transaction1.getTimeStamp().addHours(1));
		transaction1.sign();
		block.addTransaction(transaction1);
		final Transaction transaction2 = new ImportanceTransferTransaction(
				new TimeInstant(150),
				account1,
				ImportanceTransferTransaction.Mode.Activate,
				account2);
		transaction2.setDeadline(transaction2.getTimeStamp().addHours(1));
		transaction2.sign();
		block.addTransaction(transaction2);
		block.sign();

		// Assert:
		final boolean result = validator.isValid(parentBlock, blocks);
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainWithConflictingImportanceTransfersIsInvalid() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 123);
		parentBlock.sign();

		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Account accountX = Utils.generateRandomAccount();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		final Transaction transaction1 = new ImportanceTransferTransaction(
				new TimeInstant(150),
				account1,
				ImportanceTransferTransaction.Mode.Activate,
				accountX);
		transaction1.setDeadline(transaction1.getTimeStamp().addHours(1));
		transaction1.sign();
		block.addTransaction(transaction1);

		final Transaction transaction2 = new ImportanceTransferTransaction(
				new TimeInstant(150),
				account2,
				ImportanceTransferTransaction.Mode.Activate,
				accountX);
		transaction2.setDeadline(transaction2.getTimeStamp().addHours(1));
		transaction2.sign();
		block.addTransaction(transaction2);
		block.sign();

		// Assert:
		final boolean result = validator.isValid(parentBlock, blocks);
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfTransactionHashAlreadyExistInHashCache() {
		// Arrange:
		final long confirmedBlockHeight = 10;
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		final Transaction transaction = factory.createValidSignedTransaction();
		Mockito.when(factory.transactionHashCache.anyHashExists(Mockito.any())).thenReturn(true);
		final BlockChainValidator validator = factory.create();

		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), confirmedBlockHeight);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(transaction);
		block.sign();

		// Assert:
		final boolean isValid = validator.isValid(parentBlock, blocks);
		Assert.assertThat(isValid, IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInABlockIsSignedByBlockHarvester() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(factory.createValidSignedTransaction());
		block.addTransaction(factory.createSignedTransactionWithGivenSender(block.getSigner()));
		block.addTransaction(factory.createValidSignedTransaction());
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAccountSpendsAmountReceivedInEarlierBlockInLaterBlock() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Account account1 = factory.createAccountWithBalance(Amount.fromNem(1000));
		final Account account2 = factory.createAccountWithBalance(Amount.fromNem(1000));

		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		blocks.get(0).addTransaction(createTransfer(account1, account2, Amount.fromNem(500)));
		blocks.get(1).addTransaction(createTransfer(account2, account1, Amount.fromNem(1250)));
		blocks.get(2).addTransaction(createTransfer(account1, account2, Amount.fromNem(1700)));
		resignBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	//region balance checks

	@Test
	public void chainIsInvalidIfItContainsTransferTransactionHavingSignerWithInsufficientBalance() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);

		final Account signer = factory.createAccountWithBalance(Amount.fromNem(7));
		block.addTransaction(createTransfer(signer, Amount.fromNem(5), Amount.fromNem(4)));
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfItContainsTransferTransactionHavingSignerWithExactBalance() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);

		final Account signer = factory.createAccountWithBalance(Amount.fromNem(20));
		block.addTransaction(createTransfer(signer, Amount.fromNem(15), Amount.fromNem(5)));
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	@Test
	public void chainIsInvalidIfItContainsMultipleTransferTransactionsFromSameSignerHavingSignerWithInsufficientBalanceForAll() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);

		final Account signer = factory.createAccountWithBalance(Amount.fromNem(36));
		block.addTransaction(createTransfer(signer, Amount.fromNem(6), Amount.fromNem(5)));
		block.addTransaction(createTransfer(signer, Amount.fromNem(8), Amount.fromNem(2)));
		block.addTransaction(createTransfer(signer, Amount.fromNem(11), Amount.fromNem(5)));
		block.sign();

		// Assert: block execution fails
		// TODO 20141210: probably should update BlockChainValidator to handle this correctly
		ExceptionAssert.assertThrows(
				v -> validator.isValid(parentBlock, blocks),
				IllegalArgumentException.class);
	}

	//endregion

	//region multisig tests

	@Test
	public void canValidateMultisigTransferFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
		final Account cosigner = factory.createAccountWithBalance(Amount.ZERO);
		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
		// Act:
		final boolean isValid = runMultisigTransferTest(factory, multisig, cosigner, recipient);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}

	@Test
	public void multisigTransferHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
		final Account cosigner = factory.createAccountWithBalance(Amount.fromNem(200));
		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);

		// Act:
		final boolean isValid = runMultisigTransferTest(factory, multisig, cosigner, recipient);

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) = 690
		// - C 200 - 0 (No Change) = 200
		// - R 0 + 100 (Inner T amount) = 100
		Assert.assertThat(isValid, IsEqual.equalTo(true));
		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(690)));
		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(200)));
		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	@Test
	public void canValidateMultisigTransferWithMultipleSignaturesFromCosignerAccountWithZeroBalance() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
		final Account cosigner = factory.createAccountWithBalance(Amount.ZERO);
		final Account cosigner2 = factory.createAccountWithBalance(Amount.ZERO);
		final Account cosigner3 = factory.createAccountWithBalance(Amount.ZERO);
		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);

		// Act:
		final boolean isValid = runMultisigTransferTest(factory, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}

	@Test
	public void multisigTransferWithMultipleSignaturesHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final Account multisig = factory.createAccountWithBalance(Amount.fromNem(1000));
		final Account cosigner = factory.createAccountWithBalance(Amount.fromNem(201));
		final Account cosigner2 = factory.createAccountWithBalance(Amount.fromNem(202));
		final Account cosigner3 = factory.createAccountWithBalance(Amount.fromNem(203));
		final Account recipient = factory.createAccountWithBalance(Amount.ZERO);
		// Act:
		final boolean isValid = runMultisigTransferTest(factory, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) - 2 * 6 (Signature fee) = 678
		// - C1 201 - 0 (No Change) = 201
		// - C2 202 - 0 (No Change) = 202
		// - C3 203 - 0 (No Change) = 203
		// - R 0 + 100 (Inner T amount) = 100
		Assert.assertThat(isValid, IsEqual.equalTo(true));
		Assert.assertThat(factory.getAccountInfo(multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(678)));
		Assert.assertThat(factory.getAccountInfo(cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(201)));
		Assert.assertThat(factory.getAccountInfo(cosigner2).getBalance(), IsEqual.equalTo(Amount.fromNem(202)));
		Assert.assertThat(factory.getAccountInfo(cosigner3).getBalance(), IsEqual.equalTo(Amount.fromNem(203)));
		Assert.assertThat(factory.getAccountInfo(recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	private static boolean runMultisigTransferTest(
			final BlockChainValidatorFactory factory,
			final Account multisig,
			final Account cosigner,
			final Account recipient) {
		return runMultisigTransferTest(
				factory,
				multisig,
				cosigner,
				Arrays.asList(),
				recipient);
	}

	private static boolean runMultisigTransferTest(
			final BlockChainValidatorFactory factory,
			final Account multisig,
			final Account cosigner,
			final List<Account> otherCosigners,
			final Account recipient) {
		// Arrange:
		final BlockChainValidator validator = factory.create();

		factory.setCosigner(multisig, cosigner);
		for (final Account otherCosigner : otherCosigners) {
			factory.setCosigner(multisig, otherCosigner);
		}

		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		Transaction transfer = createTransfer(multisig, recipient, Amount.fromNem(100));
		transfer.setFee(Amount.fromNem(10));
		transfer = prepareTransaction(transfer);
		transfer.setSignature(null);

		final MultisigTransaction msTransaction = new MultisigTransaction(currentTime, cosigner, transfer);
		msTransaction.setFee(Amount.fromNem(200));

		for (final Account otherCosigner : otherCosigners) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(currentTime, otherCosigner, multisig, transfer);
			signatureTransaction.setFee(Amount.fromNem(6));
			msTransaction.addSignature(prepareTransaction(signatureTransaction));
		}

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
		blocks.get(0).addTransaction(prepareTransaction(msTransaction));
		resignBlocks(blocks);

		// Act:
		return validator.isValid(parentBlock, blocks);
	}

	//endregion

	//region helper functions

	//region transactions / blocks

	private static Block createParentBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static void resignBlocks(final List<Block> blocks) {
		Block previousBlock = null;
		for (final Block block : blocks) {
			if (null != previousBlock) {
				block.setPrevious(previousBlock);
			}

			block.sign();
			previousBlock = block;
		}
	}

	//endregion

	private static class BlockChainValidatorFactory {
		public final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public final int maxChainSize = 21;
		public final AccountStateCache accountStateCache = new DefaultAccountStateCache().asAutoCache();
		public final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		public final ReadOnlyNisCache nisCache = NisCacheFactory.createReadOnly(this.accountStateCache, this.transactionHashCache);
		public final BlockValidator blockValidator = NisUtils.createBlockValidatorFactory().create(this.nisCache);
		public final SingleTransactionValidator transactionValidator;

		public BlockChainValidatorFactory() {
			final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
			final AggregateSingleTransactionValidatorBuilder builder = transactionValidatorFactory.createSingleBuilder(this.accountStateCache);
			builder.add(new MultisigSignaturesPresentValidator(accountStateCache));
			this.transactionValidator = builder.build();

			Mockito.when(this.transactionHashCache.anyHashExists(Mockito.any())).thenReturn(false);
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);
		}

		public BlockChainValidator create() {
			final NisCache nisCache = NisCacheFactory.create(this.accountStateCache);
			final BlockExecutor executor = new BlockExecutor(nisCache);
			final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(nisCache);
			return new BlockChainValidator(
					block -> executor.execute(block, observer),
					this.scorer,
					this.maxChainSize,
					this.blockValidator,
					this.transactionValidator,
					new DefaultDebitPredicate(this.accountStateCache));
		}

		public AccountInfo getAccountInfo(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
		}

		private Account createAccountWithBalance(final Amount balance) {
			final Account account = Utils.generateRandomAccount();
			return this.setAccountBalance(account, balance);
		}

		private Account setAccountBalance(final Account account, final Amount balance) {
			final AccountState accountState = this.accountStateCache.findStateByAddress(account.getAddress());
			accountState.getAccountInfo().incrementBalance(balance);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
			return account;
		}

		private void setCosigner(final Account multisig, final Account cosigner) {
			this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
			this.accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
		}

		private MockTransaction createValidSignedTransaction() {
			final TimeInstant timeInstant = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME / 2);
			final MockTransaction transaction = new MockTransaction(12, timeInstant);
			return this.prepareMockTransaction(transaction);
		}

		private Transaction createSignedFutureTransaction() {
			final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
			final MockTransaction transaction = new MockTransaction(0, currentTime.addMinutes(2));
			transaction.setDeadline(currentTime.addHours(2));
			transaction.sign();
			return this.prepareMockTransaction(transaction);
		}

		private Transaction createInvalidSignedTransaction() {
			final MockTransaction transaction = new MockTransaction();
			transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() - 1));
			transaction.sign();
			return this.prepareMockTransaction(transaction);
		}

		private Transaction createSignedTransactionWithGivenSender(final Account account) {
			final MockTransaction transaction = new MockTransaction(account);
			return this.prepareMockTransaction(transaction);
		}

		private MockTransaction prepareMockTransaction(final MockTransaction transaction) {
			this.setAccountBalance(transaction.getSigner(), Amount.fromNem(1000));
			return prepareTransaction(transaction);
		}
	}

	private static Transaction createTransfer(final Account sender, final Account recipient, final Amount amount) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime.addSeconds(1), sender, recipient, amount, null);
		return prepareTransaction(transaction);
	}

	private static Transaction createTransfer(final Account signer, final Amount amount, final Amount fee) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime, signer, Utils.generateRandomAccount(), amount, null);
		transaction.setFee(fee);
		return prepareTransaction(transaction);
	}

	private static <T extends Transaction> T prepareTransaction(final T transaction) {
		// set the deadline appropriately and sign
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		transaction.setDeadline(currentTime.addSeconds(10));
		transaction.sign();
		return transaction;
	}

	private static BlockChainValidatorFactory createValidatorFactory() {
		return new BlockChainValidatorFactory();
	}

	private static BlockChainValidator createValidator() {
		return new BlockChainValidatorFactory().create();
	}

	//endregion
}
