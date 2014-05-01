package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.test.MockAccountDao;
import org.nem.nis.test.MockBlockDao;

public class BlockChainLastBlockLayerTest {
	@Test
	public void unintializedLastBlockLayerReturnsNull() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = createBlockChainLastBlockLayer();

		// Assert:
		Assert.assertNull(lastBlockLayer.getLastDbBlock());
	}

	@Test
	public void afterAnalyzeLastBlockLayerReturnsBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = createBlockChainLastBlockLayer();
		final Block block = createDbBlock(1);

		// Act:
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
	}

	@Test
	public void foo() {
		// Arrange:
		final MockAccountDao accountDao = new MockAccountDao();
		MockBlockDao mockBlockDao = new MockBlockDao(null);
		final BlockChainLastBlockLayer lastBlockLayer = new BlockChainLastBlockLayer(accountDao, mockBlockDao);
		final org.nem.core.model.Block lastBlock = createBlock();
		final Block lastDbBlock = BlockMapper.toDbModel(lastBlock, new AccountDaoLookupAdapter(accountDao));

		// Act:
		lastBlockLayer.analyzeLastBlock(lastDbBlock);
		final Block result1 = lastBlockLayer.getLastDbBlock();

		final org.nem.core.model.Block nextBlock = createBlock();
		lastBlockLayer.addBlockToDb(nextBlock);

		// Assert:
		Assert.assertThat(result1, IsSame.sameInstance(lastDbBlock));
		Block last = mockBlockDao.getLastSavedBlock();
		Assert.assertThat(last.getId(), IsEqual.equalTo(1L));
		Assert.assertThat(new Signature(last.getForgerProof()), IsEqual.equalTo(nextBlock.getSignature()));
		Assert.assertThat(lastDbBlock.getNextBlockId(), IsEqual.equalTo(last.getId()));
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsEqual.equalTo(last));
	}

	private static org.nem.core.model.Block createBlock(final Account forger) {
		// Arrange:
		org.nem.core.model.Block block = new org.nem.core.model.Block(forger, Utils.generateRandomHash(), Utils.generateRandomHash(), new TimeInstant(7), new BlockHeight(3));
		block.sign();
		return block;
	}

	private static org.nem.core.model.Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}

	private Block createDbBlock(long i) {
		Block block = new Block();
		block.setShortId(i);
		return block;
	}

	private BlockChainLastBlockLayer createBlockChainLastBlockLayer() {
		final MockAccountDao accountDao = new MockAccountDao();
		MockBlockDao mockBlockDao = new MockBlockDao(null);
		return new BlockChainLastBlockLayer(accountDao, mockBlockDao);
	}
}
