package org.nem.nis.test.BlockChain;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.sync.*;
import org.nem.nis.test.MockBlockDao;

import java.lang.reflect.Field;

public class BlockChainUtils {

	// TODO 20141225 BR: evil hack! need to add some way to make deep copies of a DefaultNisCache.
	public static DefaultNisCache createDeepNisCacheCopy(final DefaultNisCache nisCache) {
		try {
			Field field = DefaultNisCache.class.getDeclaredField("accountCache");
			field.setAccessible(true);
			final SynchronizedAccountCache accountCacheCopy = ((SynchronizedAccountCache)field.get(nisCache)).copy();
			field = DefaultNisCache.class.getDeclaredField("accountStateCache");
			field.setAccessible(true);
			final SynchronizedAccountStateCache accountStateCacheCopy = ((SynchronizedAccountStateCache)field.get(nisCache)).copy();
			field = DefaultNisCache.class.getDeclaredField("poiFacade");
			field.setAccessible(true);
			final SynchronizedPoiFacade poiFacadeCopy = ((SynchronizedPoiFacade)field.get(nisCache)).copy();
			field = DefaultNisCache.class.getDeclaredField("transactionHashCache");
			field.setAccessible(true);
			final SynchronizedHashCache transactionHashCacheCopy = ((SynchronizedHashCache)field.get(nisCache)).copy();
			return new DefaultNisCache(
					accountCacheCopy,
					accountStateCacheCopy,
					poiFacadeCopy,
					transactionHashCacheCopy
			);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException("Exception in createDeepNisCacheCopy");
		}
	}

	public static void assertBlockDaoCalls(
			final BlockDao blockDao,
			final int saveCalls,
			final int findByHashCalls,
			final int findByHeightCalls,
			final int deleteBlocksAfterHeightCalls,
			final int getHashesFromCalls,
			final int getDifficultiesFromCalls,
			final int getTimeStampsFromCall) {
		Mockito.verify(blockDao, Mockito.times(saveCalls)).save(Mockito.any(Block.class));
		Mockito.verify(blockDao, Mockito.times(findByHashCalls)).findByHash(Mockito.any());
		Mockito.verify(blockDao, Mockito.times(findByHeightCalls)).findByHeight(Mockito.any());
		Mockito.verify(blockDao, Mockito.times(deleteBlocksAfterHeightCalls)).deleteBlocksAfterHeight(Mockito.any());
		Mockito.verify(blockDao, Mockito.times(getTimeStampsFromCall)).getTimeStampsFrom(BlockHeight.ONE, 11);
		Mockito.verify(blockDao, Mockito.times(getDifficultiesFromCalls)).getDifficultiesFrom(BlockHeight.ONE, 11);
		Mockito.verify(blockDao, Mockito.times(getHashesFromCalls)).getHashesFrom(BlockHeight.ONE, BlockChainConstants.BLOCKS_LIMIT);
	}

	public static  void assertAccountDaoCalls(final AccountDao accountDao, final int getAccountByPrintableAddressCalls) {
		Mockito.verify(accountDao, Mockito.times(getAccountByPrintableAddressCalls)).getAccountByPrintableAddress(Mockito.any());
	}

	public static  void assertNisCacheCalls(
			final ReadOnlyNisCache nisCache,
			final int getAccountCacheCalls,
			final int getAccountStateCacheCalls,
			final int getPoiFacadeCalls,
			final int copyCalls) {
		Mockito.verify(nisCache, Mockito.times(getAccountCacheCalls)).getAccountCache();
		Mockito.verify(nisCache, Mockito.times(getAccountStateCacheCalls)).getAccountStateCache();
		Mockito.verify(nisCache, Mockito.times(getPoiFacadeCalls)).getPoiFacade();
		Mockito.verify(nisCache, Mockito.times(copyCalls)).copy();
	}

	public static  void assertBlockChainServicesCalls(
			final BlockChainServices blockChainServices,
			final int isPeerChainValidCalls,
			final int undoAndGetScoreCalls) {
		Mockito.verify(blockChainServices, Mockito.times(isPeerChainValidCalls))
				.isPeerChainValid(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(blockChainServices, Mockito.times(undoAndGetScoreCalls))
				.undoAndGetScore(Mockito.any(), Mockito.any(), Mockito.any());
	}

	public static void assertBlockChainLastBlockLayerCalls(
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final int addBlockToDbCalls,
			final int dropDbBlocksAfterCalls,
			final int getLastBlockHeightCalls) {
		Mockito.verify(blockChainLastBlockLayer, Mockito.times(addBlockToDbCalls)).addBlockToDb(Mockito.any());
		Mockito.verify(blockChainLastBlockLayer, Mockito.times(dropDbBlocksAfterCalls)).dropDbBlocksAfter(Mockito.any());
		Mockito.verify(blockChainLastBlockLayer, Mockito.times(getLastBlockHeightCalls)).getLastBlockHeight();
	}

	public static void assertBlockChainContextFactoryCalls(
			final BlockChainContextFactory blockChainContextFactory,
			final int createSyncContextCalls,
			final int createUpdateContextCalls) {
		Mockito.verify(blockChainContextFactory, Mockito.times(createSyncContextCalls)).createSyncContext(Mockito.any());
		Mockito.verify(blockChainContextFactory, Mockito.times(createUpdateContextCalls)).createUpdateContext(
				Mockito.any(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(),
				Mockito.anyBoolean());
	}

	public static  void assertUnconfirmedTransactionsCalls(
			final UnconfirmedTransactions unconfirmedTransactions,
			final int addExistingCalls,
			final int removeAllCalls) {
		Mockito.verify(unconfirmedTransactions, Mockito.times(addExistingCalls)).addExisting(Mockito.any());
		Mockito.verify(unconfirmedTransactions, Mockito.times(removeAllCalls)).removeAll(Mockito.any());
	}

	public static  void assertNisCachesAreEquivalent(final ReadOnlyNisCache lhs, final ReadOnlyNisCache rhs) {
		// we are only interested in the accounts and balances of the accounts
		final ReadOnlyAccountStateCache lhsCache = lhs.getAccountStateCache();
		final ReadOnlyAccountStateCache rhsCache = rhs.getAccountStateCache();

		Assert.assertThat(lhsCache.size(), IsEqual.equalTo(rhsCache.size()));
		for (final ReadOnlyAccountState accountState : lhsCache.contents()) {
			Assert.assertThat(lhs.getAccountCache().isKnownAddress(accountState.getAddress()), IsEqual.equalTo(true));
			Assert.assertThat(rhsCache.findStateByAddress(accountState.getAddress()).getAccountInfo().getBalance(),
					IsEqual.equalTo(accountState.getAccountInfo().getBalance()));
		}
	}

	public static  void assertMockBlockDaosAreEquivalent(final MockBlockDao lhs, final MockBlockDao rhs) {
		Assert.assertThat(lhs.equals(rhs), IsEqual.equalTo(true));
	}

}
