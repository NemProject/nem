package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.controller.viewmodels.AuthenticatedBlockHeightRequest;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.NisUtils;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.test.PeerUtils;

import java.util.*;
import java.util.function.Function;

public class ChainControllerTest {

	//region blockLast

	@Test
	public void blockLastReturnsMappedBlockFromBlockChain() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runBlockLastTest(context, c -> c.controller.blockLast(), b -> b);
	}

	@Test
	public void blockLastAuthenticatedReturnsMappedBlockFromBlockChain() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runBlockLastTest(
				context,
				c -> c.controller.blockLast(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runBlockLastTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, Block> getBlock) {
		// Arrange:
		final org.nem.nis.dbmodel.Block dbBlock = NisUtils.createDbBlockWithTimeStamp(443);
		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbBlock);

		// Act:
		final T result = action.apply(context);
		final Block block = getBlock.apply(result);

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastDbBlock();
		return result;
	}

	//endregion

	//region hashesFrom

	@Test
	public void hashesFromReturnsHashesFromHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		final BlockHeight height = new BlockHeight(44);
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(height, challenge);

		final int defaultLimit = BlockChainConstants.BLOCKS_LIMIT;
		final HashChain originalHashes = new HashChain(NisUtils.createHashesList(3));
		Mockito.when(context.blockDao.getHashesFrom(height, defaultLimit)).thenReturn(originalHashes);

		// Act:
		final AuthenticatedResponse<HashChain> response = context.controller.hashesFrom(request);
		final HashChain chain = response.getEntity(localNode.getIdentity(), challenge);

		// Assert:
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
		Assert.assertThat(chain, IsEqual.equalTo(originalHashes));
		Mockito.verify(context.blockDao, Mockito.times(1)).getHashesFrom(height, defaultLimit);
	}

	//endregion

	//region chainScore

	@Test
	public void chainScoreReturnsScore() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runChainScoreTest(context, c -> c.controller.chainScore(), s -> s);
	}

	@Test
	public void chainScoreAuthenticatedReturnsScore() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runChainScoreTest(
				context,
				c -> c.controller.chainScore(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runChainScoreTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, BlockChainScore> getChainScore) {
		// Arrange:
		Mockito.when(context.blockChain.getScore()).thenReturn(new BlockChainScore(21));

		// Act:
		final T result = action.apply(context);
		final BlockChainScore score = getChainScore.apply(result);

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(new BlockChainScore(21)));
		Mockito.verify(context.blockChain, Mockito.times(1)).getScore();
		return result;
	}

	//endregion

	//region chainHeight

	@Test
	public void chainHeightReturnsHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runChainHeightTest(context, c -> c.controller.chainHeight(), s -> s);
	}

	@Test
	public void chainHeightAuthenticatedReturnsHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runChainHeightTest(
				context,
				c -> c.controller.chainHeight(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runChainHeightTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, BlockHeight> getChainHeight) {
		// Arrange:
		Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(1234L);

		// Act:
		final T result = action.apply(context);
		final BlockHeight height = getChainHeight.apply(result);

		// Assert:
		Assert.assertThat(height, IsEqual.equalTo(new BlockHeight(1234L)));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastBlockHeight();
		return result;
	}

	//endregion

	//region blocksAfter

	@Test
	public void blocksAfterAuthenticatedReturnsMappedBlocksFromDatabase() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(new BlockHeight(10), challenge);

		// Assert:
		final AuthenticatedResponse<?> response = runBlocksAfterTest(
				context,
				c -> c.controller.blocksAfter(request),
				r -> r.getEntity(localNode.getIdentity(), challenge),
				getValidBlockList());
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void blocksAfterAuthenticatedThrowsIfDatabaseReturnsCorruptBlockList() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(new BlockHeight(10), challenge);

		// Arrange: set the id and next block id of the second block incorrectly
		final List<org.nem.nis.dbmodel.Block> blockList = getValidBlockList();
		blockList.get(1).setId(11L);
		blockList.get(1).setNextBlockId(12L);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> {
					runBlocksAfterTest(
							context,
							c -> c.controller.blocksAfter(request),
							r -> r.getEntity(localNode.getIdentity(), challenge),
							blockList);
				},
				RuntimeException.class);
	}

	private static List<org.nem.nis.dbmodel.Block> getValidBlockList() {
		final org.nem.nis.dbmodel.Block dbBlock1 = NisUtils.createDbBlockWithTimeStampAtHeight(443, 11);
		dbBlock1.setId(11L);
		dbBlock1.setNextBlockId(12L);
		final org.nem.nis.dbmodel.Block dbBlock2 = NisUtils.createDbBlockWithTimeStampAtHeight(543, 12);
		dbBlock2.setId(12L);
		dbBlock2.setNextBlockId(13L);
		return Arrays.asList(dbBlock1, dbBlock2);
	}

	private static <T> T runBlocksAfterTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, SerializableList<Block>> getBlocks,
			final List<org.nem.nis.dbmodel.Block> blockList) {
		// Arrange:
		Mockito.when(context.blockDao.getBlocksAfter(new BlockHeight(10), BlockChainConstants.BLOCKS_LIMIT)).thenReturn(blockList);

		// Act:
		final T result = action.apply(context);
		final SerializableList<Block> blocks = getBlocks.apply(result);

		// Assert:
		Assert.assertThat(blocks.get(0).getHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(blocks.get(0).getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Assert.assertThat(blocks.get(1).getHeight(), IsEqual.equalTo(new BlockHeight(12)));
		Assert.assertThat(blocks.get(1).getTimeStamp(), IsEqual.equalTo(new TimeInstant(543)));
		Mockito.verify(context.blockDao, Mockito.times(1))
				.getBlocksAfter(new BlockHeight(10), BlockChainConstants.BLOCKS_LIMIT);
		return result;
	}

	//endregion

	private static class TestContext {
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final AccountLookup accountLookup = new MockAccountLookup();
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final ChainController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(PeerUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new ChainController(
					this.blockDao,
					this.accountLookup,
					this.blockChainLastBlockLayer,
					this.blockChain,
					this.host);
		}
	}
}
