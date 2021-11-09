package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.FatalPeerException;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.ExceptionAssert;
import org.nem.nis.cache.DefaultNisCache;
import org.nem.nis.test.BlockChain.*;
import org.nem.nis.test.*;
import org.nem.peer.NodeInteractionResult;
import org.nem.peer.connect.*;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.logging.Logger;

public class BlockChainUpdaterTest {
	private static final Logger LOGGER = Logger.getLogger(BlockChainUpdaterTest.class.getName());
	private static final TestOptions DEFAULT_TEST_OPTIONS = new TestOptions(100, 1, 10);

	// region updateScore

	@Test
	public void updateScoreAddsBlockScoreToBlockChainScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block child = context.createChild(nodeContext.getChain(), 0);
		final Block parent = nodeContext.getChain().get(nodeContext.getChain().size() - 1);
		final int timeDiff = child.getTimeStamp().subtract(parent.getTimeStamp());
		final BlockChainScore oldScore = nodeContext.getBlockChainUpdater().getScore();

		// Act:
		nodeContext.getBlockChainUpdater().updateScore(parent, child);

		// Assert:
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().getScore(),
				IsEqual.equalTo(oldScore.add(new BlockChainScore(child.getDifficulty().getRaw() - timeDiff))));
	}

	@Test
	public void updateScoreDelegatesToNisCache() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block parent = nodeContext.getChain().get(nodeContext.getChain().size() - 1);
		final Block child = context.createChild(nodeContext.getChain(), 0);
		final BlockChainDelegationContext delegationContext = new BlockChainDelegationContext();

		// Act:
		delegationContext.getBlockChainUpdater().updateScore(parent, child);

		// Assert:
		// getAccountCacheCalls, getAccountStateCacheCalls, getPoxFacadeCalls, copyCalls
		BlockChainUtils.assertNisCacheCalls(delegationContext.getNisCache(), 0, 1, 0, 0);
	}

	// endregion

	// region updateBlock

	// In the following tests, MockBlockDao (and MockAccountDao inside it) play the role of the h2 db.
	// So "UpdatesDb" really means that BlockDao.Save() and BlockDao.deleteBlocksAfter() were called.
	@Test
	public void updateBlockUpdatesDbIfBlockIsValid() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final MockBlockDao blockDao = nodeContext.getMockBlockDao().shallowCopy();
		final Block child = context.createChild(nodeContext.getChain(), 5);
		final DefaultNisCache nisCache = nodeContext.getNisCache().deepCopy();
		nodeContext.processBlock(child, blockDao, nisCache);

		// Act:
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		BlockChainUtils.assertMockBlockDaosAreEquivalent(blockDao, nodeContext.getMockBlockDao());
	}

	@Test
	public void updateBlockUpdatesNisCacheIfBlockIsValid() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final DefaultNisCache nisCache = nodeContext.getNisCache().deepCopy();
		final Block child = context.createChild(nodeContext.getChain(), 5);
		nodeContext.processBlock(child, nodeContext.getMockBlockDao().shallowCopy(), nisCache);

		// Act:
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		BlockChainUtils.assertNisCachesAreEquivalent(nisCache.copy(), nodeContext.getNisCache().copy());
	}

	@Test
	public void updateBlockReturnsSuccessForValidBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block child = context.createChild(nodeContext.getChain(), 0);

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(child);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void updateBlockReturnsNeutralForSiblingsWithEqualScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), 0);
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void updateBlockReturnsNeutralForSiblingsWithInferiorScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), 10);
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void updateBlockReturnsSuccessForSiblingsWithSuperiorScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), -5);
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void updateBlockUpdatesDbWhenSiblingWithSuperiorScoreReplacesLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 3);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), -5);
		final MockBlockDao blockDao = nodeContext.getMockBlockDao().shallowCopy();
		final DefaultNisCache nisCache = nodeContext.getNisCache().deepCopy();
		nodeContext.processBlock(sibling, blockDao, nisCache);
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(sibling), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		BlockChainUtils.assertMockBlockDaosAreEquivalent(blockDao, nodeContext.getMockBlockDao());
	}

	@Test
	public void updateBlockUpdatesNisCacheWhenSiblingWithSuperiorScoreReplacesLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 3);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), -5);
		final DefaultNisCache nisCache = nodeContext.getNisCache().deepCopy();
		nodeContext.processBlock(sibling, nodeContext.getMockBlockDao().shallowCopy(), nisCache);
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		MatcherAssert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(sibling), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		BlockChainUtils.assertNisCachesAreEquivalent(nisCache.copy(), nodeContext.getNisCache().copy());
	}

	// endregion

	// region updateChain

	@Test
	public void updateChainReturnsNeutralIfRemoteHasSameChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);

		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.NEUTRAL));
	}

	@Test
	public void updateChainReturnsNeutralIfRemoteHasInferiorChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), null);

		// extra block for local to make his chain superior
		final Block child = context.createChild(nodeContext1.getChain(), 0);
		MatcherAssert.assertThat(nodeContext1.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.NEUTRAL));
	}

	// sub-region remote has chain with higher score

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteHasUnverifiableLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		MatcherAssert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), null);

		// make the remote's last block unverifiable
		context.addTransactions(nodeContext2.getLastBlock(), 1);

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()),
				FatalPeerException.class);
	}

	@Test
	public void updateChainReturnsFailureIfRemoteReturnsNullAsLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		MatcherAssert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// make the remote's last block null
		nodeContext2.getChain().add(null);
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.FAILURE));
	}

	@Test
	public void updateChainReturnsFailureIfRemoteIsTooFarBehindAlthoughHePromisedABetterChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		nodeContext1.processChain(context.newChainPart(nodeContext1.getChain(), NisTestConstants.REWRITE_LIMIT + 10));

		// simulate that remote reports better score although our last block has a much higher height
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(), null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.FAILURE));
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteReturnsTooManyHashes() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		MatcherAssert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		nodeContext2.getChain().add(child);

		// remote supplies more hashes than allowed
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), new HashChain(NisUtils.createHashesList(NisTestConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()),
				FatalPeerException.class);
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteReturnsInvalidHashes() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		MatcherAssert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		nodeContext2.getChain().add(child);

		// remote supplies some random hashes
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(), new HashChain(NisUtils.createHashesList(5)));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()),
				FatalPeerException.class);
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemotePromisesABetterChainScoreButSuppliesLessHashesThanWeKnow() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		nodeContext2.getChain().add(child);

		// remote reports huge chain score but supplies less hashes than we know
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(), nodeContext2.getMockBlockDao().getHashesFrom(BlockHeight.ONE, 5));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()),
				FatalPeerException.class);
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemotePromisesABetterChainScoreButSuppliesExactlyAllHashesThanWeKnow() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		nodeContext2.getChain().add(child);

		// remote reports huge chain score but supplies exactly all hashes that we know
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(), nodeContext2.getMockBlockDao().getHashesFrom(BlockHeight.ONE, 100));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()),
				FatalPeerException.class);
	}

	@Test
	public void updateChainSynchronizesSuccessfullyIfOurChainIsPartOfRemotesChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		nodeContext2.processChain(context.newChainPart(nodeContext2.getChain(), 10));

		// getHashesFrom, getChainAfter, getBlockAt
		nodeContext2.setupSyncConnectorPool();

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(nodeContext2.getConnectorPool(),
				nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.SUCCESS));
		BlockChainUtils.assertNisCachesAreEquivalent(nodeContext1.getNisCache(), nodeContext2.getNisCache());
		BlockChainUtils.assertMockBlockDaosAreEquivalent(nodeContext1.getMockBlockDao(), nodeContext2.getMockBlockDao());
	}

	@Test
	public void updateChainResolvesForkSuccessfullyIfForkIsNotTooDeep() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		nodeContext2.processChain(context.newChainPart(nodeContext2.getChain(), 10));
		nodeContext1.processChain(context.newChainPart(nodeContext1.getChain(), 5));

		// getHashesFrom, getChainAfter, getBlockAt
		nodeContext2.setupSyncConnectorPool();

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(nodeContext2.getConnectorPool(),
				nodeContext2.getNode());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.SUCCESS));
		BlockChainUtils.assertNisCachesAreEquivalent(nodeContext1.getNisCache(), nodeContext2.getNisCache());
		BlockChainUtils.assertMockBlockDaosAreEquivalent(nodeContext1.getMockBlockDao(), nodeContext2.getMockBlockDao());
	}

	@Test
	public void happyBlockChainTest() {
		// Arrange:
		final int numNodes = 10;
		final int numRounds = 20;
		final SecureRandom random = new SecureRandom();
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, numNodes, 10));
		BlockChainScore bestScore = BlockChainScore.ZERO;
		for (final NodeContext nodeContext : context.getNodeContexts()) {
			final int growBySize = random.nextInt(NisTestConstants.REWRITE_LIMIT);
			LOGGER.info(String.format("%s: growing chain by %d blocks", nodeContext.getNode().getIdentity(), growBySize));
			nodeContext.processChain(context.newChainPart(nodeContext.getChain(), growBySize));
			nodeContext.setupSyncConnectorPool();
			if (nodeContext.getBlockChainUpdater().getScore().compareTo(bestScore) > 0) {
				bestScore = nodeContext.getBlockChainUpdater().getScore();
			}
		}

		// Act:
		// in each round each node picks a random partner and synchronizes
		int round;
		for (round = 1; round <= numRounds; round++) {
			for (final NodeContext nodeContext : context.getNodeContexts()) {
				NodeContext partner = context.getNodeContexts().get(random.nextInt(numNodes));
				while (nodeContext == partner) {
					partner = context.getNodeContexts().get(random.nextInt(numNodes));
				}

				LOGGER.info(String.format("%s (%d blocks, score: %d) synchronizes with %s (%d blocks, score: %d): start",
						nodeContext.getNode().getIdentity(), nodeContext.getMockBlockDao().count(),
						nodeContext.getBlockChainUpdater().getScore().getRaw().longValue(), partner.getNode().getIdentity(),
						partner.getMockBlockDao().count(), partner.getBlockChainUpdater().getScore().getRaw().longValue()));
				final NodeInteractionResult result = nodeContext.getBlockChainUpdater().updateChain(partner.getConnectorPool(),
						partner.getNode());
				LOGGER.info(String.format("%s synchronizes with %s: end (result: %s)", nodeContext.getNode().getIdentity(),
						partner.getNode().getIdentity(), result.toString()));
				MatcherAssert.assertThat(result, IsNot.not(NodeInteractionResult.FAILURE));
			}
			if (this.allBlockChainsHaveEqualScore(context.getNodeContexts())) {
				break;
			}
		}

		// Assert:
		LOGGER.info(String.format("synchronization ended after %d rounds", round));
		MatcherAssert.assertThat(round <= numRounds, IsEqual.equalTo(true));
		MatcherAssert.assertThat(bestScore, IsEqual.equalTo(context.getNodeContexts().get(0).getBlockChainUpdater().getScore()));
		for (int i = 0; i < context.getNodeContexts().size() - 1; i++) {
			final NodeContext nodeContext1 = context.getNodeContexts().get(i);
			final NodeContext nodeContext2 = context.getNodeContexts().get(i + 1);
			BlockChainUtils.assertNisCachesAreEquivalent(nodeContext1.getNisCache(), nodeContext2.getNisCache());
			BlockChainUtils.assertMockBlockDaosAreEquivalent(nodeContext1.getMockBlockDao(), nodeContext2.getMockBlockDao());
		}
	}

	private boolean allBlockChainsHaveEqualScore(final List<NodeContext> nodeContexts) {
		for (int i = 0; i < nodeContexts.size() - 1; i++) {
			if (!nodeContexts.get(i).getBlockChainUpdater().getScore().equals(nodeContexts.get(i + 1).getBlockChainUpdater().getScore())) {
				return false;
			}
		}

		return true;
	}

	private SyncConnectorPool mockSyncConnectorForTwoNodesAction(final BlockChainScore remoteChainScore, final Block remoteLastBlock,
			final HashChain remoteHashes) {
		final SyncConnectorPool connectorPool = Mockito.mock(SyncConnectorPool.class);
		final SyncConnector connector = Mockito.mock(SyncConnector.class);
		Mockito.when(connectorPool.getSyncConnector(Mockito.any())).thenReturn(connector);
		Mockito.when(connector.getChainScore(Mockito.any())).thenReturn(remoteChainScore);
		Mockito.when(connector.getLastBlock(Mockito.any())).thenReturn(remoteLastBlock);
		Mockito.when(connector.getHashesFrom(Mockito.any(), Mockito.any())).thenReturn(remoteHashes);
		return connectorPool;
	}

	// endregion
}
