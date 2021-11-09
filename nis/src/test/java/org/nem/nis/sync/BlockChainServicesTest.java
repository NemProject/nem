package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.NisMapperFactory;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.test.BlockChain.*;
import org.nem.nis.test.MapperUtils;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class BlockChainServicesTest {

	// region createMapper

	@Test
	public void createMapperDelegatesToMapperFactory() {
		// Arrange:
		// note: createMapper() only uses the NisMapperFactory object
		final NisMapperFactory factory = Mockito.mock(NisMapperFactory.class);
		final AccountLookup lookup = new DefaultAccountCache();
		final BlockChainServices services = new BlockChainServices(null, null, null, null, factory, new ForkConfiguration());

		// Act:
		services.createMapper(lookup);

		// Assert:
		Mockito.verify(factory, Mockito.only()).createDbModelToModelNisMapper(lookup);
	}

	// endregion

	// region isPeerChainValid

	@Test
	public void isPeerChainValidReturnsTrueIfPeerChainIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);

		// Act:
		final ValidationResult result = context.getBlockChainServices().isPeerChainValid(context.getNisCacheCopy(), context.getLastBlock(),
				blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void isPeerChainValidReturnsFalseIfPeerChainIsInvalid() {
		// Arrange: change the peer data without changing the peer signature so that the peer chain is not verifiable
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		blocks.get(0).getTransactions().get(0).setFee(Amount.fromNem(1234));

		// Act:
		final ValidationResult result = context.getBlockChainServices().isPeerChainValid(context.getNisCacheCopy(), context.getLastBlock(),
				blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_UNVERIFIABLE));
	}

	@Test
	public void isPeerChainValidSetsBlockDifficulties() {
		// Arrange: remember difficulties, then reset them
		final TestContext context = new TestContext();
		final List<Block> blocks = context.createPeerChain(5);
		final Collection<BlockDifficulty> expectedDifficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
		blocks.forEach(b -> b.setDifficulty(new BlockDifficulty(0)));

		// Act:
		context.getBlockChainServices().isPeerChainValid(context.getNisCacheCopy(), context.getLastBlock(), blocks);
		final Collection<BlockDifficulty> difficulties = blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(difficulties, IsEqual.equalTo(expectedDifficulties));
	}

	// endregion

	// region undoAndGetScore

	@Test
	public void undoAndGetScoreReturnsExpectedBlockChainScore() {
		// Arrange:
		// - the peer chain has empty blocks because if the blocks in the chain are undone, transaction.undo() would throw
		// the reason is that processing the chain is done manually and not via block.execute(). Weighted balances are not updated correctly
		// causing problems when trying to undo.
		final TestContext context = new TestContext();
		final BlockChainScore initialScore = context.getBlockChainScore();
		final BlockHeight height = context.getChainHeight();
		context.processPeerChain(context.createPeerChain(0));
		final BlockChainScore peerScore = context.getBlockChainScore();

		// sanity check
		MatcherAssert.assertThat(peerScore, IsNot.not(IsEqual.equalTo(initialScore)));

		// Act:
		final BlockChainScore score = context.getBlockChainServices().undoAndGetScore(context.getNisCacheCopy(),
				context.createBlockLookup(), height);

		// Assert:
		MatcherAssert.assertThat(score, IsEqual.equalTo(peerScore.subtract(initialScore)));
	}

	@Test
	public void undoAndGetScoreUnwindsWeightedBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = context.getChainHeight();

		// - save the existing weighed balances
		final Map<Address, Integer> addressToWeightedBalancesSizeMap = context.getNisCacheCopy().getAccountStateCache().contents().stream()
				.collect(Collectors.toMap(ReadOnlyAccountState::getAddress, a -> a.getWeightedBalances().size()));

		// - add a bunch of blocks that change the number of weighted balances
		context.processPeerChain(context.createPeerChain(1500, 0));
		final NisCache copy = context.getNisCacheCopy();

		// sanity check
		copy.getAccountStateCache().contents().stream().filter(a -> !context.getNemesisAccount().getAddress().equals(a.getAddress()))
				.forEach(a -> MatcherAssert.assertThat(a.getWeightedBalances().size(),
						IsNot.not(IsEqual.equalTo(addressToWeightedBalancesSizeMap.get(a.getAddress())))));

		// Act:
		context.getBlockChainServices().undoAndGetScore(copy, context.createBlockLookup(), height);
		copy.commit();

		// Assert:
		copy.getAccountStateCache().contents().stream().forEach(a -> MatcherAssert.assertThat(a.getWeightedBalances().size(),
				IsEqual.equalTo(addressToWeightedBalancesSizeMap.get(a.getAddress()))));
	}

	@Test
	public void undoAndGetScoreUndoesTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = context.getChainHeight();

		// - save all the initial balances
		final AddressToBalanceCache cache = new AddressToBalanceCache();
		for (final ReadOnlyAccountState accountState : context.blockChainContext.getNodeContexts().get(0).getNisCache()
				.getAccountStateCache().contents()) {
			cache.addInitialBalance(accountState.getAddress(), accountState.getAccountInfo().getBalance());
		}

		// - create a peer chain and capture the expected signer and recipient balances after execution
		final List<Block> blocks = context.createPeerChain(5);
		blocks.stream().flatMap(BlockExtensions::streamDefault).filter(t -> TransactionTypes.TRANSFER == t.getType())
				.map(t -> (TransferTransaction) t).forEach(cache::addTransfer);
		blocks.stream().forEach(b -> cache.addFee(b.getSigner().getAddress(), b.getTotalFee()));

		// - process the peer chain
		context.processPeerChain(blocks);
		final NisCache copy = context.getNisCacheCopy();

		// sanity check: the balances should all be adjusted
		MatcherAssert.assertThat(cache.addressToBalanceMap.size(), IsNot.not(IsEqual.equalTo(0)));
		cache.addressToBalanceMap.entrySet().stream().forEach(e -> {
			final Amount balance = copy.getAccountStateCache().findStateByAddress(e.getKey()).getAccountInfo().getBalance();
			final Amount expected = e.getValue();
			MatcherAssert.assertThat(balance, IsEqual.equalTo(expected));
		});

		// Act: undo and revert changes
		context.getBlockChainServices().undoAndGetScore(copy, context.createBlockLookup(), height);
		copy.commit();

		// Assert: all balances should have their initial values
		cache.addressToBalanceMap.entrySet().stream().forEach(e -> {
			final Amount balance = copy.getAccountStateCache().findStateByAddress(e.getKey()).getAccountInfo().getBalance();
			final Amount expected = cache.getInitialBalance(e.getKey());
			MatcherAssert.assertThat(balance, IsEqual.equalTo(expected));
		});
	}

	private static class AddressToBalanceCache {
		private final Map<Address, Amount> addressToBalanceMap = new HashMap<>();
		private final Map<Address, Amount> addressToInitialBalanceMap = new HashMap<>();

		public void addInitialBalance(final Address address, final Amount balance) {
			this.addressToInitialBalanceMap.put(address, balance);
			this.incrementBalance(address, balance);
		}

		public void addTransfer(final TransferTransaction transaction) {
			this.decrementBalance(transaction.getSigner().getAddress(), transaction.getAmount().add(transaction.getFee()));
			this.incrementBalance(transaction.getRecipient().getAddress(), transaction.getAmount());
		}

		public void addFee(final Address address, final Amount fee) {
			this.incrementBalance(address, fee);
		}

		private void incrementBalance(final Address address, final Amount delta) {
			this.addressToBalanceMap.put(address, this.addressToBalanceMap.getOrDefault(address, Amount.ZERO).add(delta));
		}

		private void decrementBalance(final Address address, final Amount delta) {
			this.addressToBalanceMap.put(address, this.addressToBalanceMap.getOrDefault(address, Amount.ZERO).subtract(delta));
		}

		public Amount getInitialBalance(final Address address) {
			return this.addressToInitialBalanceMap.getOrDefault(address, Amount.ZERO);
		}
	}

	// endregion

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
			return new LocalBlockLookupAdapter(this.nodeContext.getMockBlockDao(),
					MapperUtils.createDbModelToModelNisMapper(this.nodeContext.getNisCache().getAccountCache()),
					this.nodeContext.getBlockChainLastBlockLayer().getLastDbBlock(), this.getBlockChainScore(), 0);
		}

		private Account getNemesisAccount() {
			return this.nodeContext.getChain().get(0).getSigner();
		}
	}
}
