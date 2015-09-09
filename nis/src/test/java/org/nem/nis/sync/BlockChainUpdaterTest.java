package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.FatalPeerException;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
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
		Assert.assertThat(
				nodeContext.getBlockChainUpdater().getScore(),
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
		// getAccountCacheCalls, getAccountStateCacheCalls, getPoiFacadeCalls, copyCalls
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
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

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
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

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
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void updateBlockReturnsNeutralForSiblingsWithEqualScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), 0);
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void updateBlockReturnsNeutralForSiblingsWithInferiorScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), 10);
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	@Test
	public void updateBlockReturnsSuccessForSiblingsWithSuperiorScore() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final List<Block> chain = nodeContext.getChain();
		final Block child = context.createChild(chain, 0);
		final Block sibling = context.createSibling(child, chain.get(chain.size() - 1), -5);
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final ValidationResult result = nodeContext.getBlockChainUpdater().updateBlock(sibling);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(sibling), IsEqual.equalTo(ValidationResult.SUCCESS));

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
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		Assert.assertThat(nodeContext.getBlockChainUpdater().updateBlock(sibling), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		BlockChainUtils.assertNisCachesAreEquivalent(nisCache.copy(), nodeContext.getNisCache().copy());
	}

	// endregion

	// sub-region delegation

	@Test
	public void updateBlockDelegatesToBlockDao() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();

		// Act (append new block to current chain, no rollback):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		// saveCalls, findByHeightCalls, deleteBlocksAfterHeightCalls
		BlockChainUtils.assertBlockDaoCalls(context.getBlockDao(), 1, 4, 1, 0, 0, 0);
	}

	@Test
	public void updateBlockDelegatesToAccountDao() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();
		final Block block = context.getBlock();
		for (int i = 0; i < 3; ++i) {
			final Transaction transaction = RandomTransactionFactory.createTransfer();
			transaction.sign();
			block.addTransaction(transaction);
		}

		block.sign();

		// Act (append new block to current chain, transactions, harvesters are already known):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert
		// - 2 calls during context construction
		// - 2 calls for each of the 3 transactions (1 sender and 1 recipient)
		BlockChainUtils.assertAccountDaoCalls(context.getAccountDao(), 2 + 2 * 3);
	}

	@Test
	public void updateBlockDelegatesToNisCache() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();

		// Act (append new block to current chain, transactions, harvesters are already known):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		// getAccountCacheCalls, getAccountStateCacheCalls, getPoiFacadeCalls, copyCalls
		// no call to copy() since the sync context is mocked
		BlockChainUtils.assertNisCacheCalls(context.getNisCache(), 2, 2, 0, 0);
	}

	@Test
	public void updateBlockDelegatesToBlockChainServices() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();

		// Act (append new block to current chain, transactions, harvesters are already known):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		// isPeerChainValidCalls, undoAndGetScoreCalls
		// no call to undoTxesAndGetScore() since the sync context is mocked
		BlockChainUtils.assertBlockChainServicesCalls(context.getBlockChainServices(), 1, 0);
	}

	@Test
	public void updateBlockDelegatesToUnconfirmedTransactions() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();

		// Act (append new block to current chain, transactions, harvesters are already known):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		// addExistingCalls, removeAllCalls
		BlockChainUtils.assertUnconfirmedTransactionsCalls(context.getUnconfirmedTransactions(), 0, 1);
	}

	@Test
	public void updateBlockDelegatesToBlockChainContextFactory() {
		// Arrange:
		final BlockChainDelegationContext context = new BlockChainDelegationContext();

		// Act (append new block to current chain, transactions, harvesters are already known):
		Assert.assertThat(context.getBlockChainUpdater().updateBlock(context.getBlock()), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Assert:
		// createSyncContextCalls, createUpdateContextCalls
		BlockChainUtils.assertBlockChainContextFactoryCalls(context.getBlockChainContextFactory(), 1, 1);
	}

	// endregion

	// region updateChain

	@Test
	public void updateChainReturnsNeutralIfRemoteHasSameChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);

		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.NEUTRAL));
	}

	@Test
	public void updateChainReturnsNeutralIfRemoteHasInferiorChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				null);

		// extra block for local to make his chain superior
		final Block child = context.createChild(nodeContext1.getChain(), 0);
		Assert.assertThat(nodeContext1.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.NEUTRAL));
	}

	// sub-region remote has chain with higher score

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteHasUnverifiableLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		Assert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				null);

		// make the remote's last block unverifiable
		context.addTransactions(nodeContext2.getLastBlock(), 1);

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()), FatalPeerException.class);
	}

	@Test
	public void updateChainReturnsFailureIfRemoteReturnsNullAsLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		Assert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));

		// make the remote's last block null
		nodeContext2.getChain().add(null);
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.FAILURE));
	}

	@Test
	public void updateChainReturnsFailureIfRemoteIsTooFarBehindAlthoughHePromisedABetterChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		nodeContext1.processChain(context.newChainPart(nodeContext1.getChain(), BlockChainConstants.REWRITE_LIMIT + 10));

		// simulate that remote reports better score although our last block has a much higher height
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(),
				null);

		// Act:
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.FAILURE));
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteReturnsTooManyHashes() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		Assert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		nodeContext2.getChain().add(child);

		// remote supplies more hashes than allowed
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				new HashChain(NisUtils.createHashesList(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()), FatalPeerException.class);
	}

	@Test
	public void updateChainThrowsFatalPeerExceptionIfRemoteReturnsInvalidHashes() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		final Block child = context.createChild(nodeContext2.getChain(), 0);
		Assert.assertThat(nodeContext2.getBlockChainUpdater().updateBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
		nodeContext2.getChain().add(child);

		// remote supplies some random hashes
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				nodeContext2.getBlockChainUpdater().getScore(),
				nodeContext2.getLastBlock(),
				new HashChain(NisUtils.createHashesList(5)));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()), FatalPeerException.class);
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
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(),
				nodeContext2.getMockBlockDao().getHashesFrom(BlockHeight.ONE, 5));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()), FatalPeerException.class);
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
		final SyncConnectorPool connectorPool = this.mockSyncConnectorForTwoNodesAction(
				new BlockChainScore(BigInteger.ONE.shiftLeft(64)),
				nodeContext2.getLastBlock(),
				nodeContext2.getMockBlockDao().getHashesFrom(BlockHeight.ONE, 100));

		// Assert:
		ExceptionAssert.assertThrows(v -> nodeContext1.getBlockChainUpdater().updateChain(connectorPool, nodeContext2.getNode()), FatalPeerException.class);
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
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(nodeContext2.getConnectorPool(), nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.SUCCESS));
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
		final NodeInteractionResult result = nodeContext1.getBlockChainUpdater().updateChain(nodeContext2.getConnectorPool(), nodeContext2.getNode());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.SUCCESS));
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
			final int growBySize = random.nextInt(BlockChainConstants.REWRITE_LIMIT);
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
						nodeContext.getNode().getIdentity(),
						nodeContext.getMockBlockDao().count(),
						nodeContext.getBlockChainUpdater().getScore().getRaw().longValue(),
						partner.getNode().getIdentity(),
						partner.getMockBlockDao().count(),
						partner.getBlockChainUpdater().getScore().getRaw().longValue()));
				final NodeInteractionResult result = nodeContext.getBlockChainUpdater().updateChain(partner.getConnectorPool(), partner.getNode());
				LOGGER.info(String.format("%s synchronizes with %s: end (result: %s)",
						nodeContext.getNode().getIdentity(),
						partner.getNode().getIdentity(),
						result.toString()));
				Assert.assertThat(result, IsNot.not(NodeInteractionResult.FAILURE));
			}
			if (this.allBlockChainsHaveEqualScore(context.getNodeContexts())) {
				break;
			}
		}

		// Assert:
		LOGGER.info(String.format("synchronization ended after %d rounds", round));
		Assert.assertThat(round <= numRounds, IsEqual.equalTo(true));
		Assert.assertThat(bestScore, IsEqual.equalTo(context.getNodeContexts().get(0).getBlockChainUpdater().getScore()));
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

	private SyncConnectorPool mockSyncConnectorForTwoNodesAction(
			final BlockChainScore remoteChainScore,
			final Block remoteLastBlock,
			final HashChain remoteHashes) {
		final SyncConnectorPool connectorPool = Mockito.mock(SyncConnectorPool.class);
		final SyncConnector connector = Mockito.mock(SyncConnector.class);
		Mockito.when(connectorPool.getSyncConnector(Mockito.any())).thenReturn(connector);
		Mockito.when(connector.getChainScore(Mockito.any())).thenReturn(remoteChainScore);
		Mockito.when(connector.getLastBlock(Mockito.any())).thenReturn(remoteLastBlock);
		Mockito.when(connector.getHashesFrom(Mockito.any(), Mockito.any())).thenReturn(remoteHashes);
		return connectorPool;
	}

	// sub-region delegation

	// TODO 20150827 J-B: do these delegation tests actually add any value?
	// TODO 20150909 BR -> J: only in the sense that it assures that methods that we expect to get called really are called.
	// > but these tests are brittle, so it's probably not worth keeping them.

	@Test
	public void updateChainDelegatesToBlockDao() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert (11 calls to save during context construction):
		// saveCalls, findByHeightCalls, deleteBlocksAfterHeightCalls, updateLastBlockIdCalls,
		// getHashesFromCalls, getDifficultiesFromCalls, getTimeStampsFromCall
		BlockChainUtils.assertBlockDaoCalls(context.getNodeContexts().get(0).getMockBlockDao(), 11 + 1, 2, 1, 1, 1, 1);
	}

	@Test
	public void updateChainDelegatesToAccountDao() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert (11 + 1 calls to save during context construction):
		// getAccountByPrintableAddressCalls
		BlockChainUtils.assertAccountDaoCalls(context.getNodeContexts().get(0).getMockBlockDao().getAccountDao(), 12 + 1);
	}

	@Test
	public void updateChainDelegatesToNisCache() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		Mockito.reset(context.getNodeContexts().get(0).getNisCache());
		this.delegationSetup(context);

		// Assert:
		// getAccountCacheCalls, getAccountStateCacheCalls, getPoiFacadeCalls, copyCalls
		BlockChainUtils.assertNisCacheCalls(context.getNodeContexts().get(0).getNisCache(), 0, 0, 0, 2);
	}

	@Test
	public void updateChainDelegatesToBlockChainServices() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert:
		// isPeerChainValidCalls, undoAndGetScoreCalls
		BlockChainUtils.assertBlockChainServicesCalls(context.getNodeContexts().get(0).getBlockChainServices(), 1, 0);
	}

	@Test
	public void updateChainDelegatesToBlockChainLastBlockLayer() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert (1 call to getLastBlockHeight during context construction):
		// addBlockToDbCalls, dropDbBlocksAfterCalls, getLastBlockHeightCalls
		BlockChainUtils.assertBlockChainLastBlockLayerCalls(context.getNodeContexts().get(0).getBlockChainLastBlockLayer(), 1, 1, 1 + 1);
	}

	@Test
	public void updateChainDelegatesToUnconfirmedTransactions() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert:
		// addExistingCalls, removeAllCalls
		BlockChainUtils.assertUnconfirmedTransactionsCalls(context.getNodeContexts().get(0).getUnconfirmedTransactions(), 0, 1);
	}

	@Test
	public void updateChainDelegatesToBlockChainContextFactory() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(new TestOptions(100, 2, 10));
		this.delegationSetup(context);

		// Assert:
		// createSyncContextCalls, createUpdateContextCalls
		BlockChainUtils.assertBlockChainContextFactoryCalls(context.getNodeContexts().get(0).getBlockChainContextFactory(), 1, 1);
	}

	private void delegationSetup(final BlockChainContext context) {
		final NodeContext nodeContext1 = context.getNodeContexts().get(0);
		final NodeContext nodeContext2 = context.getNodeContexts().get(1);
		nodeContext2.processChain(context.newChainPart(nodeContext2.getChain(), 1));

		nodeContext2.setupSyncConnectorPool();

		// Act:
		Assert.assertThat(
				nodeContext1.getBlockChainUpdater().updateChain(nodeContext2.getConnectorPool(), nodeContext2.getNode()),
				IsEqual.equalTo(NodeInteractionResult.SUCCESS));
	}

	// endregion
	// endregion
}
