package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
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
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.requests.ChainRequest;

import java.util.*;
import java.util.function.Function;

public class ChainControllerTest {

	// region blockLast

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
		final AuthenticatedResponse<?> response = runBlockLastTest(context, c -> c.controller.blockLast(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runBlockLastTest(final TestContext context, final Function<TestContext, T> action,
			final Function<T, Block> getBlock) {
		// Arrange:
		final DbBlock dbBlock = NisUtils.createDbBlockWithTimeStamp(443);
		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbBlock);

		// Act:
		final T result = action.apply(context);
		final Block block = getBlock.apply(result);

		// Assert:
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastDbBlock();
		return result;
	}

	// endregion

	// region hashesFrom

	@Test
	public void hashesFromReturnsHashesFromHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		final BlockHeight height = new BlockHeight(44);
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(height, challenge);

		final int defaultLimit = NisTestConstants.BLOCKS_LIMIT;
		final HashChain originalHashes = new HashChain(NisUtils.createHashesList(3));
		Mockito.when(context.blockDao.getHashesFrom(height, defaultLimit)).thenReturn(originalHashes);

		// Act:
		final AuthenticatedResponse<HashChain> response = context.controller.hashesFrom(request);
		final HashChain chain = response.getEntity(localNode.getIdentity(), challenge);

		// Assert:
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
		MatcherAssert.assertThat(chain, IsEqual.equalTo(originalHashes));
		Mockito.verify(context.blockDao, Mockito.times(1)).getHashesFrom(height, defaultLimit);
	}

	// endregion

	// region chainScore

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
		final AuthenticatedResponse<?> response = runChainScoreTest(context, c -> c.controller.chainScore(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runChainScoreTest(final TestContext context, final Function<TestContext, T> action,
			final Function<T, BlockChainScore> getChainScore) {
		// Arrange:
		Mockito.when(context.blockChainScoreManager.getScore()).thenReturn(new BlockChainScore(21));

		// Act:
		final T result = action.apply(context);
		final BlockChainScore score = getChainScore.apply(result);

		// Assert:
		MatcherAssert.assertThat(score, IsEqual.equalTo(new BlockChainScore(21)));
		Mockito.verify(context.blockChainScoreManager, Mockito.times(1)).getScore();
		return result;
	}

	// endregion

	// region chainHeight

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
		final AuthenticatedResponse<?> response = runChainHeightTest(context, c -> c.controller.chainHeight(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runChainHeightTest(final TestContext context, final Function<TestContext, T> action,
			final Function<T, BlockHeight> getChainHeight) {
		// Arrange:
		context.setLastBlockHeight(1234L);

		// Act:
		final T result = action.apply(context);
		final BlockHeight height = getChainHeight.apply(result);

		// Assert:
		MatcherAssert.assertThat(height, IsEqual.equalTo(new BlockHeight(1234L)));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastBlockHeight();
		return result;
	}

	// endregion

	// region blocksAfter

	@Test
	public void blocksAfterAuthenticatedReturnsMappedBlocksFromDatabase() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final AuthenticatedChainRequest request = new AuthenticatedChainRequest(new ChainRequest(new BlockHeight(10), 10, 10), challenge);

		// Assert:
		final AuthenticatedResponse<?> response = runBlocksAfterTest(context, c -> c.controller.blocksAfter(request),
				r -> r.getEntity(localNode.getIdentity(), challenge), createDbBlockList(11, 2));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void blocksAfterAuthenticatedThrowsIfDatabaseReturnsCorruptBlockList() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final AuthenticatedChainRequest request = new AuthenticatedChainRequest(new ChainRequest(new BlockHeight(10)), challenge);

		// Arrange: set the id and next block id of the second block incorrectly
		final List<DbBlock> blockList = createDbBlockList(11, 2);
		blockList.get(1).setHeight(11L);

		// Assert:
		ExceptionAssert.assertThrows(v -> runBlocksAfterTest(context, c -> c.controller.blocksAfter(request),
				r -> r.getEntity(localNode.getIdentity(), challenge), blockList), RuntimeException.class);
	}

	@Test
	public void blocksAfterAuthenticatedReturnsAtMostMaxTransactionsTransactionsInBlocksFromDatabase() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final AuthenticatedChainRequest request = new AuthenticatedChainRequest(new ChainRequest(new BlockHeight(10), 10, 130), challenge);

		// Assert:
		final AuthenticatedResponse<SerializableList<Block>> response = runBlocksAfterTest(context, c -> c.controller.blocksAfter(request),
				r -> r.getEntity(localNode.getIdentity(), challenge), createDbBlockList(11, 150));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());

		// (1 transfer + 1 multisig transaction containing 1 transfer per block)
		MatcherAssert.assertThat(response.getEntity(localNode.getIdentity(), challenge).size(), IsEqual.equalTo(130 / 3));
	}

	@SuppressWarnings("unchecked")
	private static <T> T runBlocksAfterTest(final TestContext context, final Function<TestContext, T> action,
			final Function<T, SerializableList<Block>> getBlocks, final List<DbBlock> blockList) {
		// Arrange:
		Mockito.when(context.blockDao.getBlocksAfter(Mockito.any(), Mockito.anyInt())).thenReturn(blockList, new ArrayList<>());

		// Act:
		final T result = action.apply(context);
		final Collection<Block> blocks = getBlocks.apply(result).asCollection();

		// Assert:
		final long[] heights = new long[1];
		final int[] timeInstants = new int[1];
		heights[0] = 11;
		timeInstants[0] = 400;
		blocks.stream().sorted((b1, b2) -> b1.getHeight().compareTo(b2.getHeight())).forEach(b -> {
			MatcherAssert.assertThat(b.getHeight(), IsEqual.equalTo(new BlockHeight(heights[0]++)));
			MatcherAssert.assertThat(b.getTimeStamp(), IsEqual.equalTo(new TimeInstant(timeInstants[0]++)));
		});

		// (the second parameter should be min blocks (10) + 100)
		Mockito.verify(context.blockDao, Mockito.times(1)).getBlocksAfter(new BlockHeight(10), 110);
		return result;
	}

	private static List<DbBlock> createDbBlockList(final int height, final int count) {
		final List<DbBlock> dbBlockList = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			final DbBlock dbBlock = NisUtils.createDbBlockWithTimeStampAtHeight(400 + i, height + i);
			dbBlock.setId((long) (height + i));
			dbBlock.setBlockTransferTransactions(
					Collections.singletonList(RandomDbTransactionFactory.createTransferWithTimeStamp(400 + i)));
			dbBlock.setBlockMultisigTransactions(
					Collections.singletonList(RandomDbTransactionFactory.createMultisigTransferWithTimeStamp(400 + i)));
			dbBlockList.add(dbBlock);
		}

		return dbBlockList;
	}

	// endregion

	private static class TestContext {
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final AccountLookup accountLookup = new MockAccountLookup();
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final BlockChainScoreManager blockChainScoreManager = Mockito.mock(BlockChainScoreManager.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final ChainController controller;

		public TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new ChainController(this.blockDao, this.blockChainLastBlockLayer, this.blockChainScoreManager, this.host,
					MapperUtils.createDbModelToModelNisMapper(this.accountLookup));
		}

		public void setLastBlockHeight(final long height) {
			final DbBlock lastBlock = new DbBlock();
			lastBlock.setHeight(height);
			Mockito.when(this.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(lastBlock);
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(height));
		}
	}
}
