package org.nem.nis.test.BlockChain;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.*;
import org.nem.nis.test.MapperUtils;
import org.nem.specific.deploy.NisConfiguration;

import java.util.*;

public class BlockChainDelegationContext {
	private static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	private final BlockDao blockDao = Mockito.mock(BlockDao.class);
	private final AccountDao accountDao = Mockito.mock(AccountDao.class);
	private final AccountCache accountCache = Mockito.mock(AccountCache.class);
	private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
	private final PoxFacade poxFacade = Mockito.mock(PoxFacade.class);
	private final NisCache nisCache = Mockito.mock(NisCache.class);
	private final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapperAccountDao(this.accountDao);
	private final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(this.blockDao, this.mapper);
	private final BlockChainServices blockChainServices = Mockito.mock(BlockChainServices.class);
	private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
	private final BlockChainContextFactory contextFactory = Mockito.mock(BlockChainContextFactory.class);
	private final BlockChainUpdater blockChainUpdater;
	private final Block parent;
	private final Block block;
	private DbBlock dbBlock;
	private final Account blockHarvester = Utils.generateRandomAccount();
	private final Account parentHarvester = Utils.generateRandomAccount();
	private final Collection<Address> harvesterAddresses = Arrays.asList(this.blockHarvester.getAddress(),
			this.parentHarvester.getAddress());

	public BlockChainDelegationContext() {
		final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);
		this.blockChainUpdater = Mockito.spy(new BlockChainUpdater(this.nisCache, this.blockChainLastBlockLayer, this.blockDao,
				this.contextFactory, this.unconfirmedTransactions, nisConfiguration));

		this.parent = new Block(this.parentHarvester, Hash.ZERO, DUMMY_GENERATION_HASH, TimeInstant.ZERO, new BlockHeight(1));
		this.parent.sign();
		this.block = new Block(this.blockHarvester, this.parent, new TimeInstant(this.parent.getTimeStamp().getRawTime() + 100000));
		this.block.sign();

		this.prepareMockCalls();
	}

	public BlockChainUpdater getBlockChainUpdater() {
		return this.blockChainUpdater;
	}

	public NisCache getNisCache() {
		return this.nisCache;
	}

	private void prepareMockCalls() {
		this.prepareAccountDao();
		this.prepareNisCache();
		this.prepareAccountCache();
		this.prepareAccountStateCache();
		this.preparePoxFacade();
		this.prepareBlockDao();
		this.prepareUnconfirmedTransactions();
		this.prepareBlockChainLastBlockLayer();
		this.prepareBlockChainServices();
		this.prepareBlockChainContextFactory();
	}

	private void prepareAccountDao() {
		for (final Address address : this.harvesterAddresses) {
			Mockito.when(this.accountDao.getAccountByPrintableAddress(address.getEncoded())).thenReturn(new DbAccount(address));
		}
	}

	private void prepareBlockDao() {
		final DbBlock dbParent = this.mapper.map(this.parent);
		this.dbBlock = this.mapper.map(this.block);
		Mockito.when(this.blockDao.findByHeight(this.block.getHeight())).thenReturn(null);
		Mockito.when(this.blockDao.findByHeight(this.block.getHeight().prev())).thenReturn(dbParent);
	}

	private void prepareNisCache() {
		Mockito.when(this.nisCache.copy()).thenReturn(this.nisCache);
		Mockito.when(this.nisCache.getAccountCache()).thenReturn(this.accountCache);
		Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
		Mockito.when(this.nisCache.getPoxFacade()).thenReturn(this.poxFacade);
	}

	private void prepareAccountCache() {
		for (final Address address : this.harvesterAddresses) {
			Mockito.when(this.accountCache.findByAddress(Mockito.eq(address), Mockito.any())).thenReturn(new Account(address));
		}

		Mockito.when(this.accountCache.findByAddress(Mockito.any()))
				.then(invocationOnMock -> new Account((Address) invocationOnMock.getArguments()[0]));
	}

	private void prepareAccountStateCache() {
		for (final Address address : this.harvesterAddresses) {
			final AccountState state = new AccountState(address);
			Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(state);
			Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.eq(address), Mockito.any())).thenReturn(state);
		}
	}

	@SuppressWarnings("EmptyMethod")
	private void preparePoxFacade() {
		// placeholder
	}

	private void prepareUnconfirmedTransactions() {
		Mockito.when(this.unconfirmedTransactions.addNewBatch(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
	}

	private void prepareBlockChainLastBlockLayer() {
		this.blockChainLastBlockLayer.analyzeLastBlock(this.dbBlock);
	}

	private void prepareBlockChainServices() {
		Mockito.when(this.blockChainServices.isPeerChainValid(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(ValidationResult.SUCCESS);
		Mockito.when(this.blockChainServices.createMapper(Mockito.any()))
				.thenAnswer(invocation -> MapperUtils.createDbModelToModelNisMapper((AccountLookup) invocation.getArguments()[0]));
	}

	private void prepareBlockChainContextFactory() {
		final BlockChainSyncContext syncContext = Mockito.mock(BlockChainSyncContext.class);
		Mockito.when(syncContext.undoTxesAndGetScore(Mockito.any())).thenReturn(BlockChainScore.ZERO);
		Mockito.when(syncContext.nisCache()).thenReturn(this.nisCache);
		Mockito.when(this.contextFactory.createSyncContext(Mockito.any(BlockChainScore.class))).thenReturn(syncContext);
		Mockito.when(
				this.contextFactory.createUpdateContext(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
				.thenAnswer(invocation -> new BlockChainUpdateContext(((BlockChainSyncContext) invocation.getArguments()[0]).nisCache(),
						this.nisCache, this.blockChainLastBlockLayer, this.blockDao, this.blockChainServices, this.unconfirmedTransactions,
						(DbBlock) invocation.getArguments()[1], castToBlockCollection(invocation.getArguments()[2]),
						(BlockChainScore) invocation.getArguments()[3], (Boolean) invocation.getArguments()[4]));
	}

	@SuppressWarnings("unchecked")
	private static Collection<Block> castToBlockCollection(final Object obj) {
		return (Collection<Block>) obj;
	}
}
