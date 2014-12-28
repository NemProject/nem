package org.nem.nis.test.BlockChain;

import org.mockito.Mockito;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.*;
import org.nem.nis.sync.*;
import org.nem.nis.test.MockBlockDao;
import org.nem.peer.connect.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class NodeContext {
	private static final Logger LOGGER = Logger.getLogger(NodeContext.class.getName());
	private final Node node;
	private final BlockChain blockChain;
	private final BlockChainUpdater blockChainUpdater;
	private final BlockChainServices blockChainServices;
	private final BlockChainContextFactory contextFactory;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final List<Block> chain = new ArrayList<>();
	private final MockBlockDao blockDao;
	private final DefaultNisCache nisCache;
	private SyncConnectorPool connectorPool;

	public NodeContext(
			final Node node,
			final BlockChain blockChain,
			final BlockChainUpdater blockChainUpdater,
			final BlockChainServices blockChainServices,
			final BlockChainContextFactory contextFactory,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final UnconfirmedTransactions unconfirmedTransactions,
			final List<Block> chain,
			final MockBlockDao blockDao,
			final DefaultNisCache nisCache) {
		this.node = node;
		this.blockChain = blockChain;
		this.blockChainUpdater = blockChainUpdater;
		this.blockChainServices = blockChainServices;
		this.contextFactory = contextFactory;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockDao = blockDao;
		this.nisCache = nisCache;
		this.processChain(chain);
		final NisCache nisCacheCopy = this.nisCache.copy();
		nisCacheCopy.getPoiFacade().recalculateImportances(
				new BlockHeight(this.blockChainLastBlockLayer.getLastBlockHeight()).next(),
				nisCacheCopy.getAccountStateCache().mutableContents().asCollection());
		nisCacheCopy.commit();
	}

	public Node getNode() {
		return this.node;
	}

	public BlockChain getBlockChain() {
		return this.blockChain;
	}

	public BlockChainUpdater getBlockChainUpdater() {
		return this.blockChainUpdater;
	}

	public BlockChainServices getBlockChainServices() {
		return this.blockChainServices;
	}

	public BlockChainContextFactory getBlockChainContextFactory() {
		return this.contextFactory;
	}

	public BlockChainLastBlockLayer getBlockChainLastBlockLayer() {
		return this.blockChainLastBlockLayer;
	}

	public UnconfirmedTransactions getUnconfirmedTransactions() {
		return this.unconfirmedTransactions;
	}

	public List<Block> getChain() {
		return this.chain;
	}

	public MockBlockDao getMockBlockDao() {
		return this.blockDao;
	}

	public DefaultNisCache getNisCache() {
		return this.nisCache;
	}

	public SyncConnectorPool getConnectorPool() {
		return this.connectorPool;
	}

	public void setupSyncConnectorPool() {
		this.connectorPool = Mockito.mock(SyncConnectorPool.class);
		Mockito.when(connectorPool.getSyncConnector(Mockito.any()))
				.thenAnswer(invocation -> new MockSyncConnector((AccountLookup)invocation.getArguments()[0]));
	}

	private class MockSyncConnector implements SyncConnector {
		private final AccountLookup accountLookup;

		public MockSyncConnector(final AccountLookup accountLookup) {
			this.accountLookup = accountLookup;
		}

		@Override
		public Block getLastBlock(final Node node) {
			return this.checkNull(BlockMapper.toModel(blockDao.getLastBlock(), nisCache.getAccountCache()));
		}

		@Override
		public Block getBlockAt(final Node node, final BlockHeight height) {
			return this.checkNull(BlockMapper.toModel(blockDao.findByHeight(height), nisCache.getAccountCache()));
		}

		@Override
		public HashChain getHashesFrom(final Node node, final BlockHeight height) {
			return this.checkNull(blockDao.getHashesFrom(height, BlockChainConstants.BLOCKS_LIMIT));
		}

		@Override
		public Collection<Block> getChainAfter(final Node node, final ChainRequest request) {
			final List<Block> blocks = new ArrayList<>();
			final List<org.nem.nis.dbmodel.Block> dbBlocks = blockDao.getBlocksAfter(request.getHeight(), BlockChainConstants.BLOCKS_LIMIT);
			dbBlocks.stream().forEach(dbBlock -> blocks.add(BlockMapper.toModel(dbBlock, this.accountLookup)));
			return this.checkNull(blocks);
		}

		@Override
		public BlockChainScore getChainScore(final Node node) {
			return this.checkNull(blockChainUpdater.getScore());
		}

		@Override
		public Collection<Transaction> getUnconfirmedTransactions(final Node node, final UnconfirmedTransactionsRequest unconfirmedTransactionsRequest) {
			return new ArrayList<>();
		}

		@Override
		public CompletableFuture<BlockHeight> getChainHeightAsync(final Node node) {
			throw new UnsupportedOperationException("getChainHeightAsync is not supported");
		}

		private <T> T checkNull(final T value) {
			if (null == value) {
				LOGGER.severe("null value detected!!!");
				throw new IllegalStateException("mock remote connector returned unexpected null value");
			}

			return value;
		}
	}

	public Block getLastBlock() {
		return this.chain.get(this.chain.size() - 1);
	}

	private org.nem.nis.dbmodel.Block mapBlockToDbModel(final Block block, final AccountDao accountDao) {
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);
		return BlockMapper.toDbModel(block, accountDaoLookup);
	}

	private void incrementBalance(final NisCache nisCache, final Account account, final BlockHeight height, final Amount amount) {
		this.getAccountInfo(nisCache, account).incrementBalance(amount);
		this.getAccountState(nisCache, account).getWeightedBalances().addReceive(height, amount);
	}

	private void decrementBalance(final NisCache nisCache, final Account account, final BlockHeight height, final Amount amount) {
		this.getAccountInfo(nisCache, account).decrementBalance(amount);
		this.getAccountState(nisCache, account).getWeightedBalances().addSend(height, amount);
	}

	private AccountInfo getAccountInfo(final NisCache nisCache, final Account account) {
		return nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo();
	}

	private AccountState getAccountState(final NisCache nisCache, final Account account) {
		return nisCache.getAccountStateCache().findStateByAddress(account.getAddress());
	}

	private Account addAccount(
			final Account account,
			final NisCache nisCache) {
		final AccountCache cache = nisCache.getAccountCache();
		return cache.addAccountToCache(account.getAddress());
	}

	private void processTransaction(
			final TransferTransaction transaction,
			final BlockHeight height,
			final NisCache nisCache) {
		final Account sender = addAccount(transaction.getSigner(), nisCache);
		final Account recipient = addAccount(transaction.getRecipient(), nisCache);
		decrementBalance(nisCache, sender, height, transaction.getAmount().add(transaction.getFee()));
		incrementBalance(nisCache, recipient, height, transaction.getAmount());
	}

	public void processBlock(
			final Block block,
			final MockBlockDao blockDao,
			final DefaultNisCache nisCache) {
		final NisCache nisCacheCopy = nisCache.copy();
		final Account harvester = addAccount(block.getSigner(), nisCacheCopy);
		this.incrementBalance(nisCacheCopy, harvester, block.getHeight(), block.getTotalFee());
		this.getAccountInfo(nisCacheCopy, harvester).incrementHarvestedBlocks();
		for (Transaction transaction : block.getTransactions()) {
			processTransaction((TransferTransaction)transaction, block.getHeight(), nisCacheCopy);
		}

		nisCacheCopy.commit();
		blockDao.save(this.mapBlockToDbModel(block, blockDao.getAccountDao()));
	}

	public void processChain(
			final List<Block> newChainPart) {
		for (Block block : newChainPart) {
			this.chain.add(block);
			processBlock(block, this.blockDao, this.nisCache);
			if (block.getHeight().compareTo(BlockHeight.ONE) > 0) {
				final Block parent = this.chain.get((int)block.getHeight().getRaw() - 2);
				this.blockChainUpdater.updateScore(parent, block);
			}
		}
		this.blockChainLastBlockLayer.analyzeLastBlock(mapBlockToDbModel(this.getLastBlock(), this.blockDao.getAccountDao()));
	}
}
