package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.NisMapperFactory;
import org.nem.nis.test.BlockChain.*;
import org.nem.nis.test.MapperUtils;

import java.util.List;

public class BlockChainServicesTest {

	@Test
	public void createMapperDelegatesToMapperFactory() {
		// Arrange:
		// note: createMapper() only uses the NisMapperFactory object
		final NisMapperFactory factory = Mockito.mock(NisMapperFactory.class);
		final AccountLookup lookup = new DefaultAccountCache();
		final BlockChainServices services = new BlockChainServices(null, null, null, null, factory);

		// Act:
		services.createMapper(lookup);

		// Assert:
		Mockito.verify(factory, Mockito.only()).createDbModelToModelNisMapper(lookup);
	}

	@Test
	public void isPeerChainValidReturnsTrueIfPeerChainIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);

		// Act:
		final boolean isValid = context.getBlockChainServices().isPeerChainValid(
				context.getNisCacheCopy(),
				context.getLastBlock(),
				blocks);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(true));
	}

	@Test
	public void isPeerChainValidReturnsFalseIfPeerChainIsInvalid() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		blocks.get(0).getTransactions().get(0).setFee(Amount.fromNem(1234));

		// Act:
		final boolean isValid = context.getBlockChainServices().isPeerChainValid(
				context.getNisCacheCopy(),
				context.getLastBlock(),
				blocks);

		// Assert:
		Assert.assertThat(isValid, IsEqual.equalTo(false));
	}

	@Test
	public void undoAndGetScoreReturnsExpectedBlockChainScore() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockChainScore score1 = context.getBlockChainScore();
		final BlockHeight height = context.getChainHeight();
		context.processPeerChain(context.createPeerChain(0)); // BR -> J: cannot undo transactions
		final BlockChainScore score2 = context.getBlockChainScore();
		final BlockLookup blockLookup = context.createBlockLookup();

		// sanity check
		Assert.assertThat(score2, IsNot.not(IsEqual.equalTo(score1)));

		// Act:
		final BlockChainScore score = context.getBlockChainServices().undoAndGetScore(context.getNisCacheCopy(), blockLookup, height);

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(score2.subtract(score1)));
	}

	private class TestContext {
		private final TestOptions options = new TestOptions(10, 1, 10);
		private final BlockChainContext blockChainContext = new BlockChainContext(this.options);
		private final NodeContext nodeContext = this.blockChainContext.getNodeContexts().get(0);

		private List<Block> createPeerChain(final int transactionsPerBlock) {
			return this.blockChainContext.newChainPart(this.nodeContext.getChain(), 10, transactionsPerBlock);
		}

		private void processPeerChain(final List<Block> peerChain) {
			this.nodeContext.processChain(peerChain);
		}

		private BlockChainServices getBlockChainServices() {
			return this.nodeContext.getBlockChainServices();
		}

		private Block getLastBlock() {
			return this.nodeContext.getLastBlock();
		}

		private NisCache getNisCacheCopy() {
			return this.nodeContext.getNisCache().copy();
		}

		private BlockChainScore getBlockChainScore() {
			return this.nodeContext.getBlockChainUpdater().getScore();
		}

		private BlockHeight getChainHeight() {
			return this.nodeContext.getBlockChain().getHeight();
		}

		private BlockLookup createBlockLookup() {
			return new LocalBlockLookupAdapter(
					this.nodeContext.getMockBlockDao(),
					MapperUtils.createDbModelToModelNisMapper(this.nodeContext.getNisCache().getAccountCache()),
					this.nodeContext.getBlockChainLastBlockLayer().getLastDbBlock(),
					this.getBlockChainScore(),
					0);
		}
	}
}
