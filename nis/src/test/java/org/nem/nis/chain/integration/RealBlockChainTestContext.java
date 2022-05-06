package org.nem.nis.chain.integration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.mappers.*;
import org.nem.nis.pox.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.*;
import org.nem.nis.sync.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.specific.deploy.NisConfiguration;

import java.util.Collections;
import java.util.function.Consumer;

/**
 * A test context for testing an almost real block-chain. The only mocks are the daos.
 */
public class RealBlockChainTestContext {
	private static final int MAX_TRANSACTIONS_PER_BLOCK = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;
	private final MockAccountDao accountDao = new MockAccountDao();
	private final BlockDao blockDao = new MockBlockDao(MockBlockDao.MockBlockDaoMode.MultipleBlocks, this.accountDao);
	private final MosaicIdCache mosaicIdCache = new DefaultMosaicIdCache();
	private final DefaultMapperFactory mapperFactory = new DefaultMapperFactory(this.mosaicIdCache);
	private final NisModelToDbModelMapper nisModelToDbModelMapper = new NisModelToDbModelMapper(
			this.mapperFactory.createModelToDbModelMapper(new AccountDaoLookupAdapter(this.accountDao)));
	private final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(this.blockDao,
			this.nisModelToDbModelMapper);

	private final BlockTransactionObserverFactory blockTransactionObserverFactory = new BlockTransactionObserverFactory();
	private final BlockValidatorFactory blockValidatorFactory = NisUtils.createBlockValidatorFactory();
	private final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
	private final NisMapperFactory nisMapperFactory = new NisMapperFactory(this.mapperFactory);
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
		this.initialBlockHeight = new BlockHeight(BlockMarkerConstants.MOSAICS_FORK(NetworkInfos.getDefault().getVersion() << 24));
		this.harvesterAccount = this.createAccount(Amount.fromNem(1_000_000));
		this.unlockedAccounts.addUnlockedAccount(this.harvesterAccount);

		// create and save the initial block
		this.initialBlock = this.createInitialBlock(this.harvesterAccount, this.initialBlockHeight);
		this.saveBlock(this.initialBlock);
	}

	private Block createInitialBlock(final Account harvesterAccount, final BlockHeight blockHeight) {
		final Block block = new Block(harvesterAccount, Utils.generateRandomHash(), Utils.generateRandomHash(),
				this.timeProvider.getCurrentTime().addMinutes(-2), blockHeight);
		block.sign();
		return block;
	}

	private void saveBlock(final Block block) {
		final DbBlock dbBlock = this.nisModelToDbModelMapper.map(block);
		this.blockDao.save(dbBlock);
		this.blockChainLastBlockLayer.setLoaded();
		this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
	}

	// region mapping helpers

	/**
	 * Adds a mosaic id mapping.
	 *
	 * @param mosaicId The mosaic id.
	 * @param dbMosaicId The db mosaic id.
	 */
	public void addMosaicIdMapping(final MosaicId mosaicId, final DbMosaicId dbMosaicId) {
		this.mosaicIdCache.add(mosaicId, dbMosaicId);
	}

	// endregion

	// region factory functions

	private UnconfirmedTransactions createUnconfirmedTransactions() {
		final UnconfirmedStateFactory unconfirmedStateFactory = new UnconfirmedStateFactory(this.transactionValidatorFactory,
				this.blockTransactionObserverFactory::createExecuteCommitObserver, this.timeProvider,
				this.blockChainLastBlockLayer::getLastBlockHeight, MAX_TRANSACTIONS_PER_BLOCK,
				this.nisConfiguration.getForkConfiguration());
		return new DefaultUnconfirmedTransactions(unconfirmedStateFactory, this.nisCache);
	}

	private UnlockedAccounts createUnlockedAccounts() {
		return new UnlockedAccounts(this.nisCache.getAccountCache(), this.nisCache.getAccountStateCache(), this.blockChainLastBlockLayer,
				new CanHarvestPredicate(h -> this.poiOptions.getMinHarvesterBalance()), this.nisConfiguration.getUnlockedLimit());
	}

	private BlockChain createBlockChain() {
		final BlockChainServices blockChainServices = new BlockChainServices(this.blockDao, this.blockTransactionObserverFactory,
				this.blockValidatorFactory, this.transactionValidatorFactory, this.nisMapperFactory,
				this.nisConfiguration.getForkConfiguration());

		final BlockChainContextFactory blockChainContextFactory = new BlockChainContextFactory(this.nisCache, this.blockChainLastBlockLayer,
				this.blockDao, blockChainServices, this.unconfirmedTransactions);

		final BlockChainUpdater blockChainUpdater = new BlockChainUpdater(this.nisCache, this.blockChainLastBlockLayer, this.blockDao,
				blockChainContextFactory, this.unconfirmedTransactions, this.nisConfiguration);

		return new BlockChain(this.blockChainLastBlockLayer, blockChainUpdater);
	}

	private Harvester createHarvester() {
		final NewBlockTransactionsProvider transactionsProvider = new DefaultNewBlockTransactionsProvider(this.nisCache,
				this.transactionValidatorFactory, this.blockValidatorFactory, this.blockTransactionObserverFactory,
				this.unconfirmedTransactions.asFilter(), this.nisConfiguration.getForkConfiguration());

		final BlockGenerator generator = new BlockGenerator(this.nisCache, transactionsProvider, this.blockDao,
				new BlockScorer(this.nisCache.getAccountStateCache()), this.blockValidatorFactory.create(this.nisCache));
		return new Harvester(this.timeProvider, this.blockChainLastBlockLayer, this.unlockedAccounts,
				this.nisMapperFactory.createDbModelToModelNisMapper(this.nisCache.getAccountCache()), generator);
	}

	// endregion

	// region helper function

	/**
	 * Creates a new account.
	 *
	 * @param balance The account balance.
	 * @return The account.
	 */
	public Account createAccount(final Amount balance) {
		final Account account = Utils.generateRandomAccount();
		this.modifyCache(copyCache -> {
			copyCache.getAccountCache().addAccountToCache(account.getAddress());
			final AccountState accountState = copyCache.getAccountStateCache().findStateByAddress(account.getAddress());
			accountState.getAccountInfo().incrementBalance(balance);
			accountState.setHeight(BlockHeight.ONE);
			accountState.getImportanceInfo().setImportance(GroupedHeight.fromHeight(this.initialBlockHeight), 1.0);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
		});

		return account;
	}

	/**
	 * Adds a mosaic balance to an account.
	 *
	 * @param account The account.
	 * @param mosaicId The mosaic id.
	 * @param balance The mosaic balance.
	 */
	public void addMosaicBalance(final Account account, final MosaicId mosaicId, final Quantity balance) {
		this.modifyCache(copyCache -> {
			copyCache.getNamespaceCache().add(new Namespace(mosaicId.getNamespaceId(), account, BlockHeight.ONE));
			final Mosaics mosaics = copyCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics();
			final MosaicEntry mosaicEntry = mosaics.add(Utils.createMosaicDefinition(account, mosaicId, Utils.createMosaicProperties()));
			mosaicEntry.getBalances().incrementBalance(account.getAddress(), balance);
		});
	}

	private void modifyCache(final Consumer<NisCache> modify) {
		final NisCache copyCache = this.nisCache.copy();
		modify.accept(copyCache);
		copyCache.commit();

		this.rebuildUnconfirmedCache();
	}

	/**
	 * Creates a new transfer.
	 *
	 * @param signer The signer.
	 * @param amount The amount.
	 * @return The transfer.
	 */
	public Transaction createTransfer(final Account signer, final Amount amount) {
		final Transaction t = new TransferTransaction(1, this.getTimeStamp(), signer, Utils.generateRandomAccount(), amount, null);
		return prepare(t);
	}

	/**
	 * Creates a new mosaic transfer.
	 *
	 * @param signer The signer.
	 * @param mosaicId The mosaic id.
	 * @param amount The amount.
	 * @return The transfer.
	 */
	public Transaction createMosaicTransfer(final Account signer, final MosaicId mosaicId, final Quantity amount) {
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, amount);
		final Transaction t = new TransferTransaction(2, this.getTimeStamp(), signer, Utils.generateRandomAccount(), Amount.fromNem(1),
				attachment);
		t.setFee(Amount.fromNem(15));
		return prepare(t);
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
		final Transaction t = new ImportanceTransferTransaction(this.getTimeStamp(), signer,
				activate ? ImportanceTransferMode.Activate : ImportanceTransferMode.Deactivate, remote);
		return prepare(t);
	}

	private TimeInstant getTimeStamp() {
		return this.timeProvider.getCurrentTime().addSeconds(this.timeOffset - 1);
	}

	private static Transaction prepare(final Transaction t) {
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
		return this.createNextBlock(this.harvesterAccount);
	}

	/**
	 * Creates the next block relative to the last block.
	 *
	 * @return The new block.
	 */
	public Block createNextBlock(final Account account) {
		return new Block(account, this.initialBlock, this.timeProvider.getCurrentTime().addSeconds(this.timeOffset));
	}

	/**
	 * Adds an unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 */
	public void addUnconfirmed(final Transaction transaction) {
		final ValidationResult result = this.unconfirmedTransactions.addNew(transaction);

		// Assert: that the add was successful (if this fails, a test is probably wrong)
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
	 * Rebuilds the unconfirmed cache.
	 */
	private void rebuildUnconfirmedCache() {
		this.unconfirmedTransactions.removeAll(Collections.emptyList());
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

	/**
	 * Gets the account mosaic balance.
	 *
	 * @param account The account.
	 * @param mosaicId The mosaic id.
	 * @return The mosaic balance.
	 */
	public Quantity getMosaicBalance(final Account account, final MosaicId mosaicId) {
		return this.nisCache.getNamespaceCache().get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getBalances()
				.getBalance(account.getAddress());
	}

	// endregion
}
