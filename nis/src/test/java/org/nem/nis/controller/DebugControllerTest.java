package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.PoiAlphaImportanceGeneratorImpl;
import org.nem.nis.test.NisUtils;
import org.nem.peer.test.MockPeerNetwork;

import java.math.BigInteger;
import java.util.ArrayList;

public class DebugControllerTest {

	// TODO: refactor the creation of Block and dbBlock

	@Test
	public void blockDebugInfoDelegatesToBlockDaoAndBlockScorer() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = new BlockHeight(10);
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(new PoiAlphaImportanceGeneratorImpl());
		final Account signer1 = addRandomAccountWithBalance(accountAnalyzer);
		final Account signer2 = addRandomAccountWithBalance(accountAnalyzer);

		final Block blockDaoBlock = new Block(
				signer1,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp.addSeconds(60),
				height);
		blockDaoBlock.setDifficulty(difficulty);
		blockDaoBlock.getSigner().setHeight(BlockHeight.ONE);

		final Block blockDaoParent = new Block(
				signer2,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp,
				height.prev());
		blockDaoParent.setDifficulty(difficulty);
		blockDaoParent.getSigner().setHeight(BlockHeight.ONE);

		final BlockScorer scorer = new BlockScorer(accountAnalyzer);
		scorer.forceImportanceCalculation();
		final BigInteger hit = scorer.calculateHit(blockDaoBlock);
		final BigInteger target = scorer.calculateTarget(blockDaoParent, blockDaoBlock);

		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
		dbBlock.setPrevBlockHash(blockDaoBlock.getPreviousBlockHash().getRaw());
		dbBlock.setGenerationHash(blockDaoBlock.getGenerationHash().getRaw());
		dbBlock.setForgerId(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbBlock.setDifficulty(blockDaoBlock.getDifficulty().getRaw());
		dbBlock.setHeight(blockDaoBlock.getHeight().getRaw());
		dbBlock.setTimestamp(blockDaoBlock.getTimeStamp().getRawTime());
		dbBlock.setForgerProof(new byte[64]);
		dbBlock.setBlockTransfers(new ArrayList<>());

		final org.nem.nis.dbmodel.Block dbParent = NisUtils.createDbBlockWithTimeStamp(383);
		dbParent.setPrevBlockHash(blockDaoParent.getPreviousBlockHash().getRaw());
		dbParent.setGenerationHash(blockDaoParent.getGenerationHash().getRaw());
		dbParent.setForgerId(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbParent.setDifficulty(blockDaoParent.getDifficulty().getRaw());
		dbParent.setHeight(blockDaoParent.getHeight().getRaw());
		dbParent.setTimestamp(blockDaoParent.getTimeStamp().getRawTime());
		dbParent.setForgerProof(new byte[64]);
		dbParent.setBlockTransfers(new ArrayList<>());
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(10))).thenReturn(dbBlock);
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(9))).thenReturn(dbParent);

		Mockito.when(context.blockChain.copyAccountAnalyzer()).thenReturn(accountAnalyzer);

		// Act:
		final BlockDebugInfo blockDebugInfo = context.controller.blockDebugInfo("10");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(signer1.getAddress()));
		Assert.assertThat(blockDebugInfo.getTimestamp(), IsEqual.equalTo(timestamp.addSeconds(60)));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(60));

		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(10));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(9));
		Mockito.verify(context.blockDao, Mockito.times(2)).findByHeight(Mockito.any());
	}

	private static Account addRandomAccountWithBalance(final AccountAnalyzer accountAnalyzer) {
		final Account account = accountAnalyzer.addAccountToCache(Utils.generateRandomAccount().getAddress());
		account.incrementBalance(Amount.fromNem(10000));
		account.getWeightedBalances().addFullyVested(BlockHeight.ONE, Amount.fromNem(10000));
		return account;
	}

	private static class TestContext {
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final MockPeerNetwork network = new MockPeerNetwork();
		private final NisPeerNetworkHost host;
		private final DebugController controller;

		private TestContext() {
			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new DebugController(this.host, this.blockChain, this.blockDao);
		}
	}
}