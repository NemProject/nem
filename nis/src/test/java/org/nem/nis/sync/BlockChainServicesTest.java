package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.NisMain;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidatorFactory;
import org.nem.nis.validators.TransactionValidatorFactory;

import java.util.Arrays;
import java.util.List;

public class BlockChainServicesTest {
	final static long START_HEIGHT = BlockMarkerConstants.BETA_MULTISIG_FORK;

	@Test
	public void chainWithMultisigTransactionsIssuedByNotCosignatoryIsInvalid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(134));
		context.recalculateImportances(START_HEIGHT);

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.sign();

		final boolean result = context.isValid(parentBlock, blocks);

		// Act+Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void chainWithMultisigTransactionIssuedByCosignatoryIsValid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(134));
		context.recalculateImportances(START_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.sign();

		final boolean result = context.isValid(parentBlock, blocks);

		// Act+Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	// The idea is to check if inner transaction gets to the hash transaction cache
	@Test
	public void chainIsInvalidIfContainsMultipleMultisigTransactionsWithSameInnerTransaction() {
		// Arrange:
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		context.recalculateImportances(START_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final MultisigTransaction transaction2 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, transfer);
		transaction2.setDeadline(transaction2.getTimeStamp().addSeconds(11));
		transaction2.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.addTransaction(transaction2);
		block.sign();

		ExceptionAssert.assertThrows(
				v -> context.isValid(parentBlock, blocks),
				IllegalArgumentException.class
		);
	}

	@Test
	public void chainContainingTransferTransactionMadeFromMultisigAccountIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		context.recalculateImportances(START_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final Transaction transfer = createTransfer(multisigAccount, Amount.fromNem(10), Amount.fromNem(7));
		transfer.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transfer);
		block.sign();

		// Assert
		Assert.assertThat(context.isValid(parentBlock, blocks), IsEqual.equalTo(false));
//		ExceptionAssert.assertThrows(
//				v -> context.isValid(parentBlock, blocks),
//				IllegalArgumentException.class
//		);
	}

	@Test
	public void chainWithMultipleMultisigTransactionIsIsValid() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(100 + 10 + 7 + 9));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));
		context.recalculateImportances(START_HEIGHT);
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

		final boolean result = context.isValid(parentBlock, blocks);

		// Act+Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void chainWithMultipleMultisigModificationsIsIsInvalidFromSingleAccount() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, START_HEIGHT);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(1000 + 1000));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(200));

		final Account cosignatoryNew1 = context.createAccountWithBalance(Amount.fromNem(10));
		final Account cosignatoryNew2 = context.createAccountWithBalance(Amount.fromNem(10));

		context.recalculateImportances(START_HEIGHT);
		context.makeCosignatory(cosignatory1, multisigAccount);

		final MultisigSignerModificationTransaction modification1 = createModification(multisigAccount, cosignatoryNew1);
		final MultisigTransaction transaction1 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, modification1);
		transaction1.setDeadline(transaction1.getTimeStamp().addSeconds(10));
		transaction1.sign();

		final MultisigSignerModificationTransaction modification2 = createModification(multisigAccount, cosignatoryNew2);
		final MultisigTransaction transaction2 = new MultisigTransaction(NisMain.TIME_PROVIDER.getCurrentTime(), cosignatory1, modification2);
		final MultisigSignatureTransaction signature = new MultisigSignatureTransaction(transaction2.getTimeStamp(), cosignatoryNew1, HashUtils.calculateHash(modification2));
		signature.sign();
		transaction2.addSignature(signature);
		transaction2.setDeadline(transaction2.getTimeStamp().addSeconds(10));
		transaction2.sign();

		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 2, parentBlock.getTimeStamp());
		final Block block = blocks.get(1);
		block.addTransaction(transaction1);
		block.addTransaction(transaction2);
		block.sign();

		final boolean result = context.isValid(parentBlock, blocks);

		// Act+Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

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

	private static MultisigSignerModificationTransaction createModification(final Account multisigAccount, final Account cosignatoryNew1) {
		final MultisigSignerModificationTransaction result = new MultisigSignerModificationTransaction(
				NisMain.TIME_PROVIDER.getCurrentTime(),
				multisigAccount,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, cosignatoryNew1))
		);
		result.setDeadline(result.getTimeStamp().addSeconds(10));
		return result;
	}


	private static class TestContext {
		private final BlockChainServices blockChainServices;
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final BlockTransactionObserverFactory observerFactory = new BlockTransactionObserverFactory();
		private final BlockValidatorFactory blockValidatorFactory = new BlockValidatorFactory(NisMain.TIME_PROVIDER);
		private final TransactionValidatorFactory transactionValidatorFactory = new TransactionValidatorFactory(NisMain.TIME_PROVIDER, new PoiOptionsBuilder().create());

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
					this.transactionValidatorFactory
			);
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
			final BlockHeight blockHeight = new BlockHeight(START_HEIGHT);
			this.nisCache.getAccountStateCache().findStateByAddress(cosignatory.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
			this.nisCache.getAccountStateCache().findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosignatory.getAddress(), blockHeight);
		}
	}
}
