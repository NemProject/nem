package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.NisMain;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

public class BlockChainServicesTest {
	private final static long TEST_HEIGHT = 123;

	@Test
	public void chainWithMultisigTransactionsIssuedByNotCosignatoryIsInvalid() {
		assertBasicChainWithMultisig(false, false);
	}

	@Test
	public void chainWithMultisigTransactionIssuedByCosignatoryIsValid() {
		assertBasicChainWithMultisig(true, true);
	}

	private static void assertBasicChainWithMultisig(final boolean validationResult, final boolean issuedByCosignatory) {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(168));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.ZERO);
		context.recalculateImportances(TEST_HEIGHT);

		if (issuedByCosignatory) {
			context.makeCosignatory(cosignatory1, multisigAccount);
		}

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.sign();

		// Act:
		final boolean result = context.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(validationResult));
	}

	//region MultipleMultisigTransactionsWithSameInnerTransaction

	/**
	 * A inner transaction should not be "reusable" in different multisig transactions.
	 * This is important because not checking this would leave us prone to bit more sophisticated version of "replay" attack:
	 * > I'm one of cosigners I generate MT with inner Transfer to my account
	 * > after everyone signed it, I reuse inner transfer but I change outer MT
	 * (ofc I'd probably have limited amount of time (MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)
	 * but such thing shouldn't be feasible in first place)
	 */

	@Test
	public void chainIsInvalidIfContainsMultipleMultisigTransactionsWithSameInnerTransactionInSameBlock() {
		// Arrange:
		final SameInnerTransactionTestContext context = new SameInnerTransactionTestContext();
		final List<Block> blocks = context.createBlockList();
		final Block block = blocks.get(1);
		block.addTransaction(context.transaction1);
		block.addTransaction(context.transaction2);
		block.sign();

		// Act:
		// (the HashCache throws when attempting to add the duplicate transaction)
		ExceptionAssert.assertThrows(
				v -> context.isValid(blocks),
				IllegalArgumentException.class);
	}

	@Test
	public void chainIsInvalidIfContainsMultipleMultisigTransactionsWithSameInnerTransactionAcrossBlocks() {
		// Arrange:
		final SameInnerTransactionTestContext context = new SameInnerTransactionTestContext();
		final List<Block> blocks = context.createBlockList();
		final Block block1 = blocks.get(0);
		block1.addTransaction(context.transaction1);
		block1.sign();

		final Block block2 = blocks.get(1);
		block2.setPrevious(block1);
		block2.addTransaction(context.transaction2);
		block2.sign();

		// Act:
		// (the HashCache throws when attempting to add the duplicate transaction)
		ExceptionAssert.assertThrows(
				v -> context.isValid(blocks),
				IllegalArgumentException.class);
	}

	@Test
	public void chainIsInvalidIfContainsSameTransferInAndOutOfMultisigTransaction() {
		// Arrange:
		final SameInnerTransactionTestContext context = new SameInnerTransactionTestContext();
		final List<Block> blocks = context.createBlockList();
		final Block block = blocks.get(1);
		block.addTransaction(context.signedTransfer);
		block.addTransaction(context.transaction2);
		block.sign();

		// Act:
		final boolean result = context.isValid(blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	private static class SameInnerTransactionTestContext {
		private final TestContext context = new TestContext();

		private final Account blockSigner = this.context.createAccountWithBalance(Amount.fromNem(1_000_000));
		private final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);

		private final Account multisigAccount = this.context.createAccountWithBalance(Amount.fromNem(234));
		private final Account cosignatory1 = this.context.createAccountWithBalance(Amount.ZERO);

		private final Transaction transfer = createTransfer(this.multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		private final Transaction signedTransfer = createTransfer(this.multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		private final MultisigTransaction transaction1;
		private final MultisigTransaction transaction2;

		public SameInnerTransactionTestContext() {
			this.parentBlock.sign();
			this.signedTransfer.sign();

			this.context.recalculateImportances(TEST_HEIGHT);
			this.context.makeCosignatory(this.cosignatory1, this.multisigAccount);

			this.transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), this.cosignatory1, this.transfer);
			this.transaction1.setDeadline(this.transaction1.getTimeStamp().addSeconds(10));
			this.transaction1.sign();

			this.transaction2 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), this.cosignatory1, this.transfer);
			this.transaction2.setDeadline(this.transaction2.getTimeStamp().addSeconds(11));
			this.transaction2.sign();
		}

		public List<Block> createBlockList() {
			return NisUtils.createBlockList(this.blockSigner, this.parentBlock, 2, this.parentBlock.getTimeStamp());
		}

		public boolean isValid(final List<Block> blocks) {
			return this.context.isValid(this.parentBlock, blocks);
		}
	}

	//endregion

	@Test
	public void chainContainingTransferTransactionMadeFromMultisigAccountIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		context.recalculateImportances(TEST_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		transfer.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transfer);
		block.sign();

		// Act:
		final boolean result = context.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainWithMultipleMultisigTransactionIsValid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(100 + 10 + 7 + 9 + 200));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.ZERO);
		context.recalculateImportances(TEST_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final Transaction transfer1 = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer1);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final Transaction transfer2 = createTransfer(multisigAccount, Amount.fromNem(100), Amount.fromNem(9));
		final MultisigTransaction transaction2 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer2);
		transaction2.setDeadline(transaction2.getTimeStamp().addSeconds(10));
		transaction2.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.addTransaction(transaction2);
		block.sign();

		// Act:
		final boolean result = context.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	//region MultipleMultisigModificationsFromSingleAccountIsInvalid

	/**
	 * Within a block, there cannot be multiple multisig modifications that affect the same multisig account.
	 * They could possibly conflict, so it's better to allow only one per block.
	 */

	@Test
	public void chainWithMultipleMultisigModificationsFromSingleAccountIsInvalidWhenInitiatedBySameCosigner() {
		// Arrange:
		final MultipleMultisigModificationsFromSingleAccountTestContext context = new MultipleMultisigModificationsFromSingleAccountTestContext();
		final List<Block> blocks = context.createBlockChainWithTwoModifications(context.cosignatory1, context.cosignatory1);

		// Act:
		final boolean result = context.isValid(blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainWithMultipleMultisigModificationsFromSingleAccountIsInvalidWhenInitiatedByDifferentCosigners() {
		// Arrange:
		final MultipleMultisigModificationsFromSingleAccountTestContext context = new MultipleMultisigModificationsFromSingleAccountTestContext();
		final List<Block> blocks = context.createBlockChainWithTwoModifications(context.cosignatory1, context.cosignatory2);

		// Act:
		final boolean result = context.isValid(blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	private static class MultipleMultisigModificationsFromSingleAccountTestContext {
		private final TestContext context = new TestContext();

		private final Account blockSigner = this.context.createAccountWithBalance(Amount.fromNem(1_000_000));
		private final Block parentBlock = createParentBlock(this.blockSigner, TEST_HEIGHT);

		private final Account multisigAccount = this.context.createAccountWithBalance(Amount.fromNem(1000 + 1000));
		private final Account cosignatory1 = this.context.createAccountWithBalance(Amount.fromNem(200));
		private final Account cosignatory2 = this.context.createAccountWithBalance(Amount.fromNem(200));

		public MultipleMultisigModificationsFromSingleAccountTestContext() {
			this.parentBlock.sign();

			this.context.recalculateImportances(TEST_HEIGHT);
			this.context.makeCosignatory(this.cosignatory1, this.multisigAccount);
			this.context.makeCosignatory(this.cosignatory2, this.multisigAccount);
		}

		public List<Block> createBlockChainWithTwoModifications(final Account cosignatory1, final Account cosignatory2) {
			final Account cosignatoryNew1 = this.context.createAccountWithBalance(Amount.fromNem(10));
			final Account cosignatoryNew2 = this.context.createAccountWithBalance(Amount.fromNem(10));

			final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
			final MultisigAggregateModificationTransaction modification1 = createModification(this.multisigAccount, cosignatoryNew1);
			final MultisigTransaction transaction1 = new MultisigTransaction(currentTime, cosignatory1, modification1);
			transaction1.setDeadline(currentTime.addSeconds(10));
			transaction1.sign();

			final MultisigAggregateModificationTransaction modification2 = createModification(this.multisigAccount, cosignatoryNew2);
			final MultisigTransaction transaction2 = new MultisigTransaction(currentTime, cosignatory2, modification2);
			final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(
					currentTime,
					cosignatoryNew1,
					this.multisigAccount,
					modification2);
			signature.sign();
			transaction2.addSignature(signature);
			transaction2.setDeadline(currentTime.addSeconds(10));
			transaction2.sign();

			final List<Block> blocks = NisUtils.createBlockList(this.blockSigner, this.parentBlock, 2, this.parentBlock.getTimeStamp());
			final Block block = blocks.get(1);
			block.addTransaction(transaction1);
			block.addTransaction(transaction2);
			block.sign();
			return blocks;
		}

		public boolean isValid(final List<Block> blocks) {
			return this.context.isValid(this.parentBlock, blocks);
		}
	}

	//endregion

	@Test
	public void chainWithMultisigModificationsWithSingleDelIsIsValid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(1000 + 1000));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		final Account cosignatoryDel1 = context.createAccountWithBalance(Amount.fromNem(10));

		context.recalculateImportances(TEST_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);
		context.makeCosignatory(cosignatoryDel1, multisigAccount);

		final MultisigAggregateModificationTransaction modification1 = createDelModifications(multisigAccount, Arrays.asList(cosignatoryDel1));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, modification1);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.sign();

		// Act:
		final boolean result = context.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void chainWithMultisigModificationsWithMultipleDelIsIsInvalid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, TEST_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(1000 + 1000));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		final Account cosignatoryDel1 = context.createAccountWithBalance(Amount.fromNem(10));
		final Account cosignatoryDel2 = context.createAccountWithBalance(Amount.fromNem(10));

		context.recalculateImportances(TEST_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);
		context.makeCosignatory(cosignatoryDel1, multisigAccount);
		context.makeCosignatory(cosignatoryDel2, multisigAccount);

		final MultisigAggregateModificationTransaction modification1 = createDelModifications(multisigAccount, Arrays.asList(cosignatoryDel1, cosignatoryDel2));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, modification1);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.sign();

		// Act:
		final boolean result = context.isValid(parentBlock, blocks);

		// Assert: a single modification cannot contain more than one Del modification
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	// TODO 20150103 J-G: consider a test with mixed Del and Add modifications
	// TODO 20150103 J-G: is there a reason these tests are testing at the BlockChainServices level instead of the BlockChainValidator level?

	private static Block createParentBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Transaction createTransfer(final Account signer, final Amount amount, final Amount fee) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime, signer, Utils.generateRandomAccount(), amount, null);
		transaction.setFee(fee);
		transaction.setDeadline(currentTime.addSeconds(10));
		return transaction;
	}

	private static MultisigAggregateModificationTransaction createModification(final Account multisigAccount, final Account cosignatoryNew1) {
		final MultisigAggregateModificationTransaction result = new MultisigAggregateModificationTransaction(
				NisMain.TIME_PROVIDER.getCurrentTime(),
				multisigAccount,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, cosignatoryNew1)));
		result.setDeadline(result.getTimeStamp().addSeconds(10));
		return result;
	}

	private static MultisigAggregateModificationTransaction createDelModifications(final Account multisigAccount, final List<Account> cosignatories) {
		final MultisigAggregateModificationTransaction result = new MultisigAggregateModificationTransaction(
				NisMain.TIME_PROVIDER.getCurrentTime(),
				multisigAccount,
				cosignatories.stream().map(a -> new MultisigModification(MultisigModificationType.Del, a)).collect(Collectors.toList()));
		result.setDeadline(result.getTimeStamp().addSeconds(10));
		return result;
	}

	private static class TestContext {
		private final BlockChainServices blockChainServices;
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final BlockTransactionObserverFactory observerFactory = new BlockTransactionObserverFactory();
		private final BlockValidatorFactory blockValidatorFactory = new BlockValidatorFactory(NisMain.TIME_PROVIDER);
		private final TransactionValidatorFactory transactionValidatorFactory = new TransactionValidatorFactory(
				NisMain.TIME_PROVIDER,
				new PoiOptionsBuilder().create());

		private final NisCache nisCache;

		private TestContext() {
			this.nisCache = new DefaultNisCache(
					new SynchronizedAccountCache(new DefaultAccountCache()),
					new SynchronizedAccountStateCache(new DefaultAccountStateCache()),
					new SynchronizedPoiFacade(new DefaultPoiFacade(new PoiImportanceCalculator(new PoiScorer(), new PoiOptionsBuilder().create()))),
					new SynchronizedHashCache(new DefaultHashCache())).copy();

			this.blockChainServices = new BlockChainServices(
					this.blockDao,
					this.observerFactory,
					this.blockValidatorFactory,
					this.transactionValidatorFactory,
					MapperUtils.createNisMapperFactory());
		}

		private Account createAccountWithBalance(final Amount balance) {
			final Account account = Utils.generateRandomAccount();
			final AccountState accountState = this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress());
			accountState.setHeight(BlockHeight.ONE);
			accountState.getAccountInfo().incrementBalance(balance);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
			return account;
		}

		public boolean isValid(final Block parentBlock, final List<Block> blocks) {
			return this.blockChainServices.isPeerChainValid(this.nisCache, parentBlock, blocks);
		}

		private void recalculateImportances(final long height) {
			this.nisCache.getPoiFacade().recalculateImportances(new BlockHeight(height), this.nisCache.getAccountStateCache().mutableContents().asCollection());
		}

		public void makeCosignatory(final Account cosignatory, final Account multisig) {
			this.nisCache.getAccountStateCache().findStateByAddress(cosignatory.getAddress()).getMultisigLinks()
					.addCosignatoryOf(multisig.getAddress());
			this.nisCache.getAccountStateCache().findStateByAddress(multisig.getAddress()).getMultisigLinks()
					.addCosignatory(cosignatory.getAddress());
		}
	}
}
