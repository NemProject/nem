package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.NisMain;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidatorFactory;
import org.nem.nis.validators.TransactionValidatorFactory;

import java.util.List;

public class BlockChainServicesTest {
	@Test
	public void chainIsValidIfContainsMultisigTransactionsWithInnerTransaction() {
		final TestContext context = new TestContext();

		final Account blockSigner = context.createAccountWithBalance(Amount.fromNem(1_000_000));
		final Block parentBlock = createParentBlock(blockSigner, 11);
		parentBlock.sign();

		final Account multisigAccount = context.createAccountWithBalance(Amount.fromNem(34));
		final Account cosignatory1 = context.createAccountWithBalance(Amount.fromNem(134));
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

	private static Block createParentBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Transaction createTransfer(final Account signer, final Amount amount, final Amount fee) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime, signer, Utils.generateRandomAccount(), amount, null);
		transaction.setFee(fee);
		transaction.setDeadline(currentTime.addSeconds(10));
		transaction.sign();
		return transaction;
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
			accountState.getImportanceInfo().setImportance(BlockHeight.ONE, 0.1);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
			return account;
		}

		public boolean isValid(final Block parentBlock, final List<Block> blocks) {
			return this.blockChainServices.isPeerChainValid(this.nisCache, parentBlock, blocks);
		}
	}
}
