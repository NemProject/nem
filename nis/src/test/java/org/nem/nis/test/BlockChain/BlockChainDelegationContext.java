package org.nem.nis.test.BlockChain;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.BlockChain;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.*;

import java.util.Collection;

public class BlockChainDelegationContext {
	private static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	private final BlockDao blockDao = Mockito.mock(BlockDao.class);
	private final AccountDao accountDao = Mockito.mock(AccountDao.class);
	private final AccountCache accountCache = Mockito.mock(AccountCache.class);
	private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
	private final PoiFacade poiFacde = Mockito.mock(PoiFacade.class);
	private final NisCache nisCache = Mockito.mock(NisCache.class);
	private final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(this.accountDao, this.blockDao);
	private final BlockChainServices blockChainServices = Mockito.mock(BlockChainServices.class);
	private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
	private final BlockChainContextFactory contextFactory = Mockito.mock(BlockChainContextFactory.class);
	private final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);
	private final BlockChainUpdater blockChainUpdater;
	private final BlockChain blockChain;
	private final Block parent;
	private final Block block;
	private org.nem.nis.dbmodel.Block dbParent;
	private org.nem.nis.dbmodel.Block dbBlock;
	private final Account blockHarvester = Utils.generateRandomAccount();
	private final AccountState blockHarvesterState = new AccountState(blockHarvester.getAddress());
	private final Account parentHarvester = Utils.generateRandomAccount();
	private final AccountState parentHarvesterState = new AccountState(parentHarvester.getAddress());

	public BlockChainDelegationContext() {
		this.blockChainUpdater = Mockito.spy(new BlockChainUpdater(
				this.nisCache,
				this.accountDao,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.contextFactory,
				this.unconfirmedTransactions,
				this.nisConfiguration));
		this.blockChain = new BlockChain(
				this.blockChainLastBlockLayer,
				this.blockChainUpdater);

		this.parent = new Block(
				this.parentHarvester,
				Hash.ZERO,
				DUMMY_GENERATION_HASH,
				TimeInstant.ZERO,
				new BlockHeight(1));
		this.parent.sign();
		this.block = new Block(
				this.blockHarvester,
				this.parent,
				new TimeInstant(this.parent.getTimeStamp().getRawTime() + 100000));
		this.block.sign();

		this.prepareMockCalls();
	}

	public BlockChainUpdater getBlockChainUpdater() {
		return this.blockChainUpdater;
	}

	public NisCache getNisCache() {
		return this.nisCache;
	}

	public Block getBlock() {
		return this.block;
	}

	public BlockDao getBlockDao() {
		return this.blockDao;
	}

	public AccountDao getAccountDao() {
		return this.accountDao;
	}

	public BlockChainServices getBlockChainServices() {
		return this.blockChainServices;
	}

	public BlockChainContextFactory getBlockChainContextFactory() {
		return this.contextFactory;
	}

	public UnconfirmedTransactions getUnconfirmedTransactions() {
		return this.unconfirmedTransactions;
	}

	private void prepareMockCalls() {
		this.prepareAccountDao();
		this.prepareNisCache();
		this.prepareAccountCache();
		this.prepareAccountStateCache();
		this.preparePoiFacade();
		this.prepareBlockDao();
		this.prepareUnconfirmedTransactions();
		this.prepareBlockChainLastBlockLayer();
		this.prepareBlockChainServices();
		this.prepareBlockChainContextFactory();
	}

	private void prepareAccountDao() {
		Mockito.when(this.accountDao.getAccountByPrintableAddress(blockHarvester.getAddress().getEncoded()))
				.thenReturn(new org.nem.nis.dbmodel.Account(blockHarvester.getAddress().getEncoded(), blockHarvester.getAddress().getPublicKey()));
		Mockito.when(this.accountDao.getAccountByPrintableAddress(parentHarvester.getAddress().getEncoded()))
				.thenReturn(new org.nem.nis.dbmodel.Account(parentHarvester.getAddress().getEncoded(), parentHarvester.getAddress().getPublicKey()));
	}

	private void prepareBlockDao() {
		final Hash blockHash = HashUtils.calculateHash(this.block);
		final Hash parentHash = HashUtils.calculateHash(this.parent);
		this.dbParent = BlockMapper.toDbModel(this.parent, new AccountDaoLookupAdapter(this.accountDao));
		this.dbBlock = BlockMapper.toDbModel(this.block, new AccountDaoLookupAdapter(this.accountDao));
		Mockito.when(this.blockDao.findByHash(blockHash)).thenReturn(null);
		Mockito.when(this.blockDao.findByHash(parentHash)).thenReturn(this.dbParent);
		Mockito.when(this.blockDao.findByHeight(Mockito.any())).thenReturn(this.dbParent, this.dbBlock);
	}

	private void prepareNisCache() {
		Mockito.when(this.nisCache.copy()).thenReturn(this.nisCache);
		Mockito.when(this.nisCache.getAccountCache()).thenReturn(this.accountCache);
		Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
		Mockito.when(this.nisCache.getPoiFacade()).thenReturn(this.poiFacde);
	}

	private void prepareAccountCache() {
		Mockito.when(this.accountCache.findByAddress(blockHarvester.getAddress())).thenReturn(this.blockHarvester);
		Mockito.when(this.accountCache.findByAddress(parentHarvester.getAddress())).thenReturn(this.parentHarvester);
	}

	private void prepareAccountStateCache() {
		Mockito.when(this.accountStateCache.findStateByAddress(blockHarvester.getAddress())).thenReturn(this.blockHarvesterState);
		Mockito.when(this.accountStateCache.findStateByAddress(parentHarvester.getAddress())).thenReturn(this.parentHarvesterState);
		Mockito.when(this.accountStateCache.findForwardedStateByAddress(this.block.getSigner().getAddress(), this.block.getHeight()))
				.thenReturn(this.blockHarvesterState);
	}

	private void preparePoiFacade() {
	}

	private void prepareUnconfirmedTransactions() {
		Mockito.when(this.unconfirmedTransactions.addNewBatch(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
	}

	private void prepareBlockChainLastBlockLayer() {
		this.blockChainLastBlockLayer.analyzeLastBlock(this.dbBlock);
	}

	private void prepareBlockChainServices() {
		Mockito.when(this.blockChainServices.isPeerChainValid(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
	}

	private void prepareBlockChainContextFactory() {
		final BlockChainSyncContext syncContext = Mockito.mock(BlockChainSyncContext.class);
		Mockito.when(syncContext.undoTxesAndGetScore(Mockito.any())).thenReturn(BlockChainScore.ZERO);
		Mockito.when(syncContext.nisCache()).thenReturn(this.nisCache);
		Mockito.when(this.contextFactory.createSyncContext(Mockito.any(BlockChainScore.class)))
				.thenReturn(syncContext);
		Mockito.when(this.contextFactory.createUpdateContext(
						Mockito.any(),
						Mockito.any(),
						Mockito.any(),
						Mockito.any(),
						Mockito.anyBoolean())
		).thenAnswer(invocation -> new BlockChainUpdateContext(
				((BlockChainSyncContext)invocation.getArguments()[0]).nisCache(),
				this.nisCache,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.blockChainServices,
				this.unconfirmedTransactions,
				(org.nem.nis.dbmodel.Block)invocation.getArguments()[1],
				(Collection<Block>)invocation.getArguments()[2],
				(BlockChainScore)invocation.getArguments()[3],
				(Boolean)invocation.getArguments()[4]));
	}
}
