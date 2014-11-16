package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;
import org.nem.nis.service.BlockExecutor;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

/**
 * This suite is different from BlockChainValidatorTest because it uses REAL validators
 * (or at least very close to real)
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
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(createValidSignedTransaction());
		block.addTransaction(createInvalidSignedTransaction());
		block.addTransaction(createValidSignedTransaction());
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allTransactionsInChainMustHaveValidTimestamp() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(createValidSignedTransaction());
		block.addTransaction(createSignedFutureTransaction());
		block.addTransaction(createValidSignedTransaction());
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainWithValidTransactionsIsValid() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(createValidSignedTransaction());
		block.addTransaction(createValidSignedTransaction());
		block.addTransaction(createValidSignedTransaction());
		block.sign();

		// Assert:
		final boolean isValid = validator.isValid(parentBlock, blocks);
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}


	@Test
	public void chainWithImportanceTransferToNonZeroBalanceAccountIsInvalid() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Account account1 = Utils.generateRandomAccount();
		account1.incrementBalance(Amount.fromNem(12345));
		final Account account2 = Utils.generateRandomAccount();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		final Transaction transaction1 = new TransferTransaction(new TimeInstant(100), account1, account2, new Amount(7), null);
		transaction1.setDeadline(transaction1.getTimeStamp().addHours(1));
		transaction1.sign();
		block.addTransaction(transaction1);
		final Transaction transaction2 = new ImportanceTransferTransaction(new TimeInstant(150), account1,
		                                                                   ImportanceTransferTransaction.Mode.Activate, account2);
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
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Account account1 = Utils.generateRandomAccount();
		account1.incrementBalance(Amount.fromNem(12345));
		final Account account2 = Utils.generateRandomAccount();
		account2.incrementBalance(Amount.fromNem(12345));
		final Account accountX = Utils.generateRandomAccount();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		final Transaction transaction1 = new ImportanceTransferTransaction(new TimeInstant(150), account1,
		                                                                   ImportanceTransferTransaction.Mode.Activate, accountX);
		transaction1.setDeadline(transaction1.getTimeStamp().addHours(1));
		transaction1.sign();
		block.addTransaction(transaction1);

		final Transaction transaction2 = new ImportanceTransferTransaction(new TimeInstant(150), account2,
		                                                                   ImportanceTransferTransaction.Mode.Activate, accountX);
		transaction2.setDeadline(transaction2.getTimeStamp().addHours(1));
		transaction2.sign();
		block.addTransaction(transaction2);
		block.sign();

		// Assert:
		final boolean result = validator.isValid(parentBlock, blocks);
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfTransactionAlreadyExistInDb() {
		// Arrange:
		final long confirmedBlockHeight = 10;
		final Transaction transaction = createValidSignedTransaction();
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		Mockito.when(factory.transferDao.anyHashExists(Mockito.any(), Mockito.any())).thenReturn(true);
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
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(createValidSignedTransaction());
		block.addTransaction(createSignedTransactionWithGivenSender(block.getSigner()));
		block.addTransaction(createValidSignedTransaction());
		block.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAccountSpendsAmountReceivedInEarlierBlockInLaterBlock() {
		// Arrange:
		class TestContext {
			final AccountCache accountCache = new AccountCache();
			final PoiFacade poiFacade = new PoiFacade(NisUtils.createImportanceCalculator());

			private BlockChainValidator createValidator() {
				final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(this.accountCache, this.poiFacade);
				final BlockExecutor executor = new BlockExecutor(this.poiFacade, this.accountCache);
				final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();

				final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(accountAnalyzer);
				factory.executor = block -> executor.execute(block, observer);
				return factory.create();
			}

			private Account createSeedAccount() {
				final Amount seedAmount = Amount.fromNem(1000);
				final Account account = Utils.generateRandomAccount(seedAmount);
				this.poiFacade.findStateByAddress(account.getAddress()).getWeightedBalances().addFullyVested(BlockHeight.ONE, seedAmount);
				return account;
			}
		}

		final TestContext context = new TestContext();
		final Account account1 = context.createSeedAccount();
		final Account account2 = context.createSeedAccount();
		final BlockChainValidator validator = context.createValidator();

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

	//region helper functions

	//region transactions / blocks

	private static MockTransaction createValidSignedTransaction() {
		final TimeInstant timeInstant = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME / 2);
		final MockTransaction transaction = new MockTransaction(12, timeInstant);
		transaction.setDeadline(timeInstant.addSeconds(1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedFutureTransaction() {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new MockTransaction(0, currentTime.addMinutes(2));
		transaction.setDeadline(currentTime.addHours(2));
		transaction.sign();
		return transaction;
	}

	private static Transaction createInvalidSignedTransaction() {
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() - 1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createTransfer(final Account sender, final Account recipient, final Amount amount) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime.addSeconds(1), sender, recipient, amount, null);
		transaction.setDeadline(currentTime.addHours(1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedTransactionWithGivenSender(final Account account) {
		final Transaction transaction = new MockTransaction(account);
		transaction.setDeadline(new TimeInstant(16));
		transaction.sign();
		return transaction;
	}

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
		public Consumer<Block> executor = block -> { };
		public BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public int maxChainSize = 21;
		public final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		public final BlockValidator blockValidator = NisUtils.createBlockValidatorFactory().create(this.poiFacade);
		public final TransferDao transferDao = Mockito.mock(TransferDao.class);
		public final SingleTransactionValidator transactionValidator;
		public final BatchTransactionValidator batchTransactionValidator;
		public final BatchTransactionValidator batchTransactionsPostValidator;

		public BlockChainValidatorFactory() {
			final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory(this.transferDao);
			this.transactionValidator = transactionValidatorFactory.createSingle(this.poiFacade);
			this.batchTransactionValidator = transactionValidatorFactory.createBatch(this.poiFacade);
			this.batchTransactionsPostValidator = transactionValidatorFactory.createBatchPost();

			Mockito.when(this.transferDao.anyHashExists(Mockito.any(), Mockito.any())).thenReturn(false);
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);

			Mockito.when(this.poiFacade.findStateByAddress(Mockito.any()))
					.then(invocation -> new PoiAccountState((Address)invocation.getArguments()[0]));
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
					.then(invocation -> new PoiAccountState((Address)invocation.getArguments()[0]));
		}

		public BlockChainValidator create() {
			return new BlockChainValidator(
					this.executor,
					this.scorer,
					this.maxChainSize,
					this.blockValidator,
					this.transactionValidator,
					this.batchTransactionValidator,
					this.batchTransactionsPostValidator);
		}
	}

	private static BlockChainValidator createValidator() {
		return new BlockChainValidatorFactory().create();
	}

	//endregion
}
