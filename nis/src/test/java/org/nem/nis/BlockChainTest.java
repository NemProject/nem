package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainUpdater;
import org.nem.nis.test.BlockChain.*;
import org.nem.peer.connect.SyncConnectorPool;

public class BlockChainTest {
	private static final TestOptions DEFAULT_TEST_OPTIONS = new TestOptions(100, 1, 10);

	// region getHeight

	@Test
	public void getHeightDelegatesToBlockChainLastBlockLayer() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		Mockito.reset(nodeContext.getBlockChainLastBlockLayer());

		// Act:
		final BlockHeight height = nodeContext.getBlockChain().getHeight();

		// Assert: height is equal to common height (10) + 1
		MatcherAssert.assertThat(height, IsEqual.equalTo(new BlockHeight(11)));
		Mockito.verify(nodeContext.getBlockChainLastBlockLayer(), Mockito.only()).getLastBlockHeight();
	}

	// endregion

	// region checkPushedBlock

	@Test
	public void checkPushedBlockReturnsSuccessIfSuppliedBlockCanBeAddedToChain() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block child = context.createChild(nodeContext.getChain(), 0);

		// Assert:
		MatcherAssert.assertThat(nodeContext.getBlockChain().checkPushedBlock(child), IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void checkPushedBlockReturnsFailureIfSuppliedBlockCannotBeVerified() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block child = context.createChild(nodeContext.getChain(), 0);
		context.addTransactions(child, 1);

		// Assert:
		MatcherAssert.assertThat(nodeContext.getBlockChain().checkPushedBlock(child),
				IsEqual.equalTo(ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE));
	}

	@Test
	public void checkPushedBlockReturnsFailureIfSuppliedBlockIsNeitherChildNotSiblingOfLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block child = context.createChild(nodeContext.getChain(), 0);
		child.setPrevious(nodeContext.getChain().get(0));

		// Assert:
		MatcherAssert.assertThat(nodeContext.getBlockChain().checkPushedBlock(child),
				IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC));
	}

	@Test
	public void checkPushedBlockReturnsNeutralIfSuppliedBlockIsSiblingOfLastBlock() {
		// Arrange:
		final BlockChainContext context = new BlockChainContext(DEFAULT_TEST_OPTIONS);
		final NodeContext nodeContext = context.getNodeContexts().get(0);
		final Block lastBlock = nodeContext.getChain().get(nodeContext.getChain().size() - 1);
		final Block lastBlockParent = nodeContext.getChain().get(nodeContext.getChain().size() - 2);
		final Block child = context.createSibling(lastBlock, lastBlockParent, 0);

		// Assert:
		MatcherAssert.assertThat(nodeContext.getBlockChain().checkPushedBlock(child), IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	// endregion

	// region delegation

	@Test
	public void processBlockDelegatesToBlockChainUpdater() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.blockChain.processBlock(context.block);

		// Assert:
		Mockito.verify(context.updater, Mockito.only()).updateBlock(context.block);
	}

	@Test
	public void synchronizeDelegatesToBlockChainUpdater() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.blockChain.synchronizeNode(context.connectorPool, context.node);

		// Assert:
		Mockito.verify(context.updater, Mockito.only()).updateChain(context.connectorPool, context.node);
	}

	private class TestContext {
		private final BlockChainUpdater updater = Mockito.mock(BlockChainUpdater.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final BlockChain blockChain = new BlockChain(this.lastBlockLayer, this.updater);
		private final SyncConnectorPool connectorPool = Mockito.mock(SyncConnectorPool.class);
		private final Node node = Mockito.mock(Node.class);
		private final Block block = Mockito.mock(Block.class);
	}

	// endregion
}
