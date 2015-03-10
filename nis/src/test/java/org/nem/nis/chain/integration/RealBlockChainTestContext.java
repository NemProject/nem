package org.nem.nis.chain.integration;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.harvesting.*;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

/**
 * A test context for testing an almost real block-chain.
 * The only mocks are the daos.
 */
public class RealBlockChainTestContext {
	private final MockAccountDao accountDao = new MockAccountDao();
	private final BlockDao blockDao = new MockBlockDao(MockBlockDao.MockBlockDaoMode.MultipleBlocks, this.accountDao);
	private final NisModelToDbModelMapper nisModelToDbModelMapper =
			new NisModelToDbModelMapper(new DefaultMapperFactory().createModelToDbModelMapper(new AccountDaoLookupAdapter(this.accountDao)));
	private final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(
			this.blockDao,
			this.nisModelToDbModelMapper);

	private final BlockTransactionObserverFactory blockTransactionObserverFactory = new BlockTransactionObserverFactory();
	private final BlockValidatorFactory blockValidatorFactory = NisUtils.createBlockValidatorFactory();
	private final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
	private final NisMapperFactory nisMapperFactory = MapperUtils.createNisMapperFactory();
	private final TimeProvider timeProvider = new SystemTimeProvider();
	private final NisConfiguration nisConfiguration = new NisConfiguration();
	private final PoiOptions poiOptions = new PoiOptionsBuilder(BlockHeight.MAX).create();

	private final ReadOnlyNisCache nisCache;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final UnlockedAccounts unlockedAccounts;
	private final BlockChain blockChain;
	private final Harvester harvester;

	private final Account harvesterAccount;
	private final BlockHeight initialBlockHeight;
	private final Block initialBlock;
	private int timeOffset;

	public RealBlockChainTestContext() {
		this(NisCacheFactory.createReal());
	}

	public RealBlockChainTestContext(final ReadOnlyNisCache nisCache) {
		this.nisCache = nisCache;

		// initialize primary objects
		this.unconfirmedTransactions = this.createUnconfirmedTransactions();
		this.unlockedAccounts = this.createUnlockedAccounts();
		this.blockChain = this.createBlockChain();
		this.harvester = this.createHarvester();

		// create a harvesting-eligible account
		this.initialBlockHeight = new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK + 100);
		this.harvesterAccount = this.createAccount(Amount.fromNem(1_000_000));
		this.unlockedAccounts.addUnlockedAccount(harvesterAccount);

		// create and save the initial block
		this.initialBlock = this.createInitialBlock(this.harvesterAccount, this.initialBlockHeight);
		this.saveBlock(this.initialBlock);
	}

	private Block createInitialBlock(final Account harvesterAccount, final BlockHeight blockHeight) {
		final Block block = new Block(
				harvesterAccount,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				this.timeProvider.getCurrentTime().addMinutes(-2),
				blockHeight);
		block.sign();
		return block;
	}

	private void saveBlock(final Block block) {
		final DbBlock dbBlock = this.nisModelToDbModelMapper.map(block);
		this.blockDao.save(dbBlock);
		this.blockChainLastBlockLayer.setLoaded();
		this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
	}

	//region factory functions

	private UnconfirmedTransactions createUnconfirmedTransactions() {
		return new UnconfirmedTransactions(
				this.transactionValidatorFactory,
				this.nisCache,
				this.timeProvider);
	}

	private UnlockedAccounts createUnlockedAccounts() {
		return new UnlockedAccounts(
				this.nisCache.getAccountCache(),
				this.nisCache.getAccountStateCache(),
				this.blockChainLastBlockLayer,
				new CanHarvestPredicate(h -> this.poiOptions.getMinHarvesterBalance()),
				this.nisConfiguration.getUnlockedLimit());
	}

	private BlockChain createBlockChain() {
		final BlockChainServices blockChainServices = new BlockChainServices(
				this.blockDao,
				this.blockTransactionObserverFactory,
				this.blockValidatorFactory,
				this.transactionValidatorFactory,
				this.nisMapperFactory);

		final BlockChainContextFactory blockChainContextFactory = new BlockChainContextFactory(
				this.nisCache,
				this.blockChainLastBlockLayer,
				this.blockDao,
				blockChainServices,
				this.unconfirmedTransactions);

		final BlockChainUpdater blockChainUpdater = new BlockChainUpdater(
				this.nisCache,
				this.blockChainLastBlockLayer,
				this.blockDao,
				blockChainContextFactory,
				this.unconfirmedTransactions,
				this.nisConfiguration);

		return new BlockChain(
				this.blockChainLastBlockLayer,
				blockChainUpdater);
	}

	private Harvester createHarvester() {
		final NewBlockTransactionsProvider transactionsProvider = new BlockAwareNewBlockTransactionsProvider(
				this.nisCache,
				this.transactionValidatorFactory,
				this.blockValidatorFactory,
				this.blockTransactionObserverFactory,
				this.unconfirmedTransactions);

		final BlockGenerator generator = new BlockGenerator(
				this.nisCache,
				transactionsProvider,
				this.blockDao,
				new BlockScorer(this.nisCache.getAccountStateCache()),
				this.blockValidatorFactory.create(this.nisCache));
		return new Harvester(
				this.timeProvider,
				this.blockChainLastBlockLayer,
				this.unlockedAccounts,
				this.nisMapperFactory.createDbModelToModelNisMapper(this.nisCache.getAccountCache()),
				generator);
	}

	//endregion

	//region helper function

	/**
	 * Creates a new account.
	 *
	 * @param balance The account balance.
	 * @return The account.
	 */
	public Account createAccount(final Amount balance) {
		final NisCache copyCache = this.nisCache.copy();
		final Account account = Utils.generateRandomAccount();
		copyCache.getAccountCache().addAccountToCache(account.getAddress());
		final AccountState accountState = copyCache.getAccountStateCache().findStateByAddress(account.getAddress());
		accountState.getAccountInfo().incrementBalance(balance);
		accountState.setHeight(BlockHeight.ONE);
		accountState.getImportanceInfo().setImportance(GroupedHeight.fromHeight(this.initialBlockHeight), 1.0);
		accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
		copyCache.commit();
		return account;
	}

	/**
	 * Creates a new transfer.
	 *
	 * @param signer The signer.
	 * @param amount The amount.
	 * @return The transfer.
	 */
	public Transaction createTransfer(final Account signer, final Amount amount) {
		final Transaction t = new TransferTransaction(
				this.timeProvider.getCurrentTime().addSeconds(this.timeOffset - 1),
				signer,
				Utils.generateRandomAccount(),
				amount,
				null);
		t.setDeadline(t.getTimeStamp().addMinutes(10));
		t.sign();
		return t;
	}

	/**
	 * Creates a new importance transfer.
	 *
	 * @param signer The signer.
	 * @param remote The remote.
	 * @param activate true if the importance transfer should be an activate transfer.
	 * @return The transfer.
	 */
	public Transaction createImportanceTransfer(final Account signer, final Account remote, final boolean activate) {
		final Transaction t = new ImportanceTransferTransaction(
				this.timeProvider.getCurrentTime().addSeconds(this.timeOffset - 1),
				signer,
				activate ? ImportanceTransferMode.Activate : ImportanceTransferMode.Deactivate,
				remote);
		t.setDeadline(t.getTimeStamp().addMinutes(10));
		t.sign();
		return t;
	}


	/**
	 * Creates the next block relative to the last block.
	 *
	 * @return The new block.
	 */
	public Block createNextBlock() {
		return new Block(
				this.harvesterAccount,
				this.initialBlock,
				this.timeProvider.getCurrentTime().addSeconds(this.timeOffset));
	}

	/**
	 * Adds an unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 */
	public void addUnconfirmed(final Transaction transaction) {
		final ValidationResult result = this.unconfirmedTransactions.addNew(transaction);

		// Assert: that the add was successful (if this fails, a test is probably wrong)
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	/**
	 * Processes a block.
	 *
	 * @param block The block.
	 * @return The process result.
	 */
	public ValidationResult processBlock(final Block block) {
		return this.blockChain.processBlock(block);
	}

	/**
	 * Harvests a block.
	 *
	 * @return The block.
	 */
	public Block harvestBlock() {
		return this.harvester.harvestBlock();
	}

	/**
	 * Sets the time offset.
	 *
	 * @param timeOffset The time offset.
	 */
	public void setTimeOffset(final int timeOffset) {
		this.timeOffset = timeOffset;
	}

	/**
	 * Gets the account balance.
	 *
	 * @param account The account.
	 * @return The balance.
	 */
	public Amount getBalance(final Account account) {
		return this.nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
	}

	//endregion
}

//endregion