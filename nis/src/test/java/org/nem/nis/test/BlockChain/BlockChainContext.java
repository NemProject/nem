package org.nem.nis.test.BlockChain;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.mappers.*;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.*;
import org.nem.nis.sync.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.specific.deploy.NisConfiguration;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Note that mockito is only used for mocking the daos and spying real objects.
 */
public class BlockChainContext {
	private static final int MAX_TRANSACTIONS_PER_BLOCK = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;
	private static final int TRANSFER_TRANSACTION_VERSION = 1;
	private static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();
	private final TestOptions options;
	private final HashMap<Address, Account> accountMap;
	private final Account nemesisAccount;
	private final List<NodeContext> nodeContexts;
	private final BlockScorer scorer;
	private final SecureRandom random;

	public List<NodeContext> getNodeContexts() {
		return this.nodeContexts;
	}

	public BlockChainContext(final TestOptions options) {
		this.random = new SecureRandom();
		this.accountMap = new HashMap<>();
		this.options = options;
		final ImportanceCalculator importanceCalculator = (blockHeight, accountStates) -> accountStates.stream()
				.forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size()));
		final DefaultPoxFacade poxFacade = new DefaultPoxFacade(importanceCalculator);
		final ReadOnlyNisCache commonNisCache = NisCacheFactory.createReal(poxFacade);
		this.scorer = new BlockScorer(commonNisCache.getAccountStateCache());
		this.nemesisAccount = this.addAccount(commonNisCache);
		this.createNemesisAccounts(this.options.numAccounts(), commonNisCache);
		final Block nemesisBlock = this.createNemesisBlock(this.nemesisAccount);
		final List<Block> commonChain = this.createChain(nemesisBlock, this.options.commonChainHeight());
		this.nodeContexts = new ArrayList<>();

		final NisConfiguration nisConfiguration = new NisConfiguration();
		for (int i = 0; i < this.options.numNodes(); i++) {
			final Node node = this.createNode(i + 1);
			final DefaultNisCache nisCache = Mockito.spy(((DefaultNisCache) commonNisCache).deepCopy());
			final MockAccountDao accountDao = Mockito.spy(new MockAccountDao());
			final MockBlockDao blockDao = Mockito.spy(new MockBlockDao(MockBlockDao.MockBlockDaoMode.MultipleBlocks, accountDao));
			final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapperAccountDao(accountDao);
			final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.spy(new BlockChainLastBlockLayer(blockDao, mapper));
			final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
			final UnconfirmedStateFactory unconfirmedStateFactory = new UnconfirmedStateFactory(transactionValidatorFactory,
					NisUtils.createBlockTransactionObserverFactory()::createExecuteCommitObserver, new SystemTimeProvider(),
					blockChainLastBlockLayer::getLastBlockHeight, MAX_TRANSACTIONS_PER_BLOCK, nisConfiguration.getForkConfiguration());
			final UnconfirmedTransactions unconfirmedTransactions = Mockito
					.spy(new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache));
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			final NisMapperFactory nisMapperFactory = new NisMapperFactory(mapperFactory);
			final BlockValidatorFactory blockValidatorFactory = NisUtils.createBlockValidatorFactory();
			final BlockTransactionObserverFactory blockTransactionObserverFactory = new BlockTransactionObserverFactory();
			final BlockChainServices services = Mockito.spy(new BlockChainServices(blockDao, blockTransactionObserverFactory,
					blockValidatorFactory, transactionValidatorFactory, nisMapperFactory, nisConfiguration.getForkConfiguration()));
			final BlockChainContextFactory contextFactory = Mockito
					.spy(new BlockChainContextFactory(nisCache, blockChainLastBlockLayer, blockDao, services, unconfirmedTransactions));
			final BlockChainUpdater blockChainUpdater = new BlockChainUpdater(nisCache, blockChainLastBlockLayer, blockDao, contextFactory,
					unconfirmedTransactions, nisConfiguration);
			final BlockChain blockChain = new BlockChain(blockChainLastBlockLayer, blockChainUpdater);
			final NodeContext nodeContext = new NodeContext(node, blockChain, blockChainUpdater, services, blockChainLastBlockLayer,
					commonChain, blockDao, nisCache);

			this.nodeContexts.add(nodeContext);
		}
	}

	private Node createNode(final int i) {
		return new Node(new WeakNodeIdentity(String.format("Node %d", i)), new NodeEndpoint("ftp", String.format("10.8.8.%d", i), 12));
	}

	// nemesis accounts
	private void createNemesisAccounts(final int numAccounts, final ReadOnlyNisCache nisCache) {
		for (int i = 0; i < numAccounts; i++) {
			this.addAccount(nisCache);
		}
	}

	private Account addAccount(final ReadOnlyNisCache nisCache) {
		final NisCache copy = nisCache.copy();
		final AccountCache accountCache = copy.getAccountCache();
		final AccountStateCache accountStateCache = copy.getAccountStateCache();
		final Amount amount = Amount.fromNem(1_000_000);
		final Account account = Utils.generateRandomAccount();
		this.accountMap.put(account.getAddress(), account);
		accountCache.addAccountToCache(account.getAddress());
		final AccountState accountState = accountStateCache.findStateByAddress(account.getAddress());
		final AccountInfo accountInfoCopy = accountState.getAccountInfo().copy();
		accountState.getAccountInfo().incrementReferenceCount();
		accountState.getAccountInfo().incrementBalance(amount);
		accountInfoCopy.incrementBalance(amount);
		accountState.getWeightedBalances().addReceive(BlockHeight.ONE, amount);
		accountState.setHeight(BlockHeight.ONE);
		copy.commit();
		return account;
	}

	// not really a nemesis block but rather the starting block
	private Block createNemesisBlock(final Account nemesisAccount) {
		final Block block = new Block(nemesisAccount, Hash.ZERO, DUMMY_GENERATION_HASH, TimeInstant.ZERO, new BlockHeight(1));
		block.sign();
		return block;
	}

	private Account getRandomKnownAccount() {
		return this.accountMap.values().stream().filter(a -> !a.equals(this.nemesisAccount)).toArray(Account[]::new)[this.random
				.nextInt(this.options.numAccounts())];
	}

	private List<BlockDifficulty> historicalDifficulties(final List<Block> blocks) {
		return blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
	}

	private List<TimeInstant> historicalTimestamps(final List<Block> blocks) {
		return blocks.stream().map(VerifiableEntity::getTimeStamp).collect(Collectors.toList());
	}

	private TransferTransaction createTransferTransaction(final Account blockSigner, final TimeInstant timeInstant) {
		// Avoid self signed transaction
		Account signer = this.getRandomKnownAccount();
		while (signer.equals(blockSigner)) {
			signer = this.getRandomKnownAccount();
		}
		final Account recipient = new Account(Utils.generateRandomAddress());
		final TransferTransaction transaction = new TransferTransaction(TRANSFER_TRANSACTION_VERSION, timeInstant, signer, recipient,
				Amount.fromNem(100), null);
		transaction.setDeadline(timeInstant.addHours(20));
		transaction.sign();
		return transaction;
	}

	public void addTransactions(final Block block, final int numTransactions) {
		for (int i = 0; i < numTransactions; i++) {
			block.addTransaction(this.createTransferTransaction(block.getSigner(), block.getTimeStamp().addMinutes(-5)));
		}
	}

	public Block createChild(final List<Block> chain, final int numTransactions) {
		final Account harvester = this.getRandomKnownAccount();
		final Block parent = chain.get(chain.size() - 1);
		Block block = new Block(harvester, parent, new TimeInstant(parent.getTimeStamp().getRawTime() + 1));
		final List<Block> historicalBlocks = chain.subList(Math.max(0, chain.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION),
				chain.size());
		final BlockDifficulty difficulty = this.scorer.getDifficultyScorer()
				.calculateDifficulty(this.historicalDifficulties(historicalBlocks), this.historicalTimestamps(historicalBlocks));
		block.setDifficulty(difficulty);
		final BigInteger hit = this.scorer.calculateHit(block);

		// add 10 seconds to be able to create superior siblings
		// (the block generation is delayed a bit so that we can construct a better sibling by subtracting a few seconds)
		final Amount nemesisAmount = NetworkInfos.getDefault().getNemesisBlockInfo().getAmount();
		final int seconds = hit.multiply(block.getDifficulty().asBigInteger()).multiply(BigInteger.valueOf(this.options.numAccounts() + 1))
				.divide(BlockScorer.TWO_TO_THE_POWER_OF_64).divide(BigInteger.valueOf(nemesisAmount.getNumNem())).intValue() + 10;

		final TimeInstant blockTime = new TimeInstant(parent.getTimeStamp().getRawTime() + seconds);
		block = new Block(harvester, parent, blockTime);
		block.setDifficulty(difficulty);
		this.addTransactions(block, numTransactions);
		block.sign();
		return block;
	}

	public Block createSibling(final Block block, final Block parentBlock, final int timeDiff) {
		// add a new transaction to the sibling so that it has a different block hash even when timeDiff is 0
		final Block sibling = new Block(block.getSigner(), parentBlock, block.getTimeStamp().addSeconds(timeDiff));
		sibling.setDifficulty(block.getDifficulty());
		this.addTransactions(sibling, 1);
		sibling.sign();
		return sibling;
	}

	private List<Block> createChain(final Block startBlock, final int size) {
		final List<Block> chain = new ArrayList<>();
		chain.add(startBlock);
		chain.addAll(this.newChainPart(chain, size));
		return chain;
	}

	public List<Block> newChainPart(final List<Block> chain, final int size) {
		return this.newChainPart(chain, size, 0);
	}

	public List<Block> newChainPart(final List<Block> chain, final int size, final int numTransactionsPerBlock) {
		final List<Block> newChain = new ArrayList<>();
		newChain.addAll(chain);
		for (int i = 0; i < size; i++) {
			final Block block = this.createChild(newChain, numTransactionsPerBlock);
			newChain.add(block);
		}

		return newChain.subList(chain.size(), newChain.size());
	}
}
