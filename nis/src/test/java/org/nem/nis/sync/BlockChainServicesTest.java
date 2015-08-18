package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.NisMapperFactory;
import org.nem.nis.test.BlockChain.*;
import org.nem.nis.test.MapperUtils;

import java.util.*;
import java.util.stream.Collectors;

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
		// Arrange: change the peer data without changing the peer signature so that the peer chain is not verifiable
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

	// TODO 20150817 J-B: might want one more test that isPeerChainValid sets block difficulties on the peer chain correctly
	// TODO 20150818 BR -> J: sure

	@Test
	public void isPeerChainValidSetsBlockDifficulties() {
		// Arrange: remember difficulties, then reset them
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		final Collection<BlockDifficulty> expectedDifficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
		blocks.forEach(b -> b.setDifficulty(new BlockDifficulty(0)));

		// Act:
		context.getBlockChainServices().isPeerChainValid(context.getNisCacheCopy(), context.getLastBlock(),	blocks);
		final Collection<BlockDifficulty> difficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(difficulties, IsEquivalent.equivalentTo(expectedDifficulties));
	}

	@Test
	public void undoAndGetScoreReturnsExpectedBlockChainScore() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockChainScore score1 = context.getBlockChainScore();
		final BlockHeight height = context.getChainHeight();
		context.processPeerChain(context.createPeerChain(0)); // BR -> J: cannot undo transactions
		// TODO 20150817 J-B:  'BR -> J: cannot undo transactions' what do you mean by this comment?
		// TODO 20150818 BR -> J: the peer chain has empty blocks because if the blocks in the chain are undone, transaction.undo() would throw.
		// > the reason is that processing the chain is done manually and not via block.execute(). Weighted balances are not updated correctly
		// > causing problems when trying to undo.
		final BlockChainScore score2 = context.getBlockChainScore();
		final BlockLookup blockLookup = context.createBlockLookup();

		// sanity check
		Assert.assertThat(score2, IsNot.not(IsEqual.equalTo(score1)));

		// Act:
		final BlockChainScore score = context.getBlockChainServices().undoAndGetScore(context.getNisCacheCopy(), blockLookup, height);

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(score2.subtract(score1)));
	}

	@Test
	public void undoAndGetScoreUnwindsWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = context.getChainHeight();
		final HashMap<Address, Integer> addressToWeightedBalancesSize = new HashMap<>();
		context.getNisCacheCopy().getAccountStateCache().contents().stream()
				.forEach(a -> addressToWeightedBalancesSize.put(a.getAddress(), a.getWeightedBalances().size()));
		context.processPeerChain(context.createPeerChain(1500, 0));
		final BlockLookup blockLookup = context.createBlockLookup();
		final NisCache copy = context.getNisCacheCopy();

		// sanity check
		copy.getAccountStateCache().contents().stream()
				.filter(a -> !context.getNemesisAccount().getAddress().equals(a.getAddress()))
				.forEach(a -> Assert.assertThat(
						a.getWeightedBalances().size(),
						IsNot.not(IsEqual.equalTo(addressToWeightedBalancesSize.get(a.getAddress())))));

		// Act:
		context.getBlockChainServices().undoAndGetScore(copy, blockLookup, height);
		copy.commit();

		// Assert:
		copy.getAccountStateCache().contents().stream()
				.forEach(a -> Assert.assertThat(a.getWeightedBalances().size(), IsEqual.equalTo(addressToWeightedBalancesSize.get(a.getAddress()))));
	}

	// TODO 20150817 J-B:  might want a specific test for this:
	// > 'this is delicate and the order matters, first visitor during undo changes amount of harvested blocks
	// > second visitor needs that information'
	// TODO 20150818 BR -> J: I think that comment is old. The PartialWeightedScoreVisitor only needs the block difficulty and the time diff to the last block.
	// > No dependency on the first visitor.

	private class TestContext {
		private final TestOptions options = new TestOptions(10, 1, 10);
		private final BlockChainContext blockChainContext = new BlockChainContext(this.options);
		private final NodeContext nodeContext = this.blockChainContext.getNodeContexts().get(0);

		private List<Block> createPeerChain(final int transactionsPerBlock) {
			return this.createPeerChain(10, transactionsPerBlock);
		}

		private List<Block> createPeerChain(final int size, final int transactionsPerBlock) {
			return this.blockChainContext.newChainPart(this.nodeContext.getChain(), size, transactionsPerBlock);
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

		private Account getNemesisAccount() {
			return this.nodeContext.getChain().get(0).getSigner();
		}
	}
}
