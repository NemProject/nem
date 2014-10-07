package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.*;
import org.nem.nis.validators.TransactionValidatorFactory;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlockChainTest {
	public static final long RECIPIENT1_AMOUNT = 3 * 1000000L;
	public static final long RECIPIENT2_AMOUNT = 5 * 1000000L;
	private static final org.nem.core.model.Account SENDER = Utils.generateRandomAccount();
	private static final org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static final org.nem.core.model.Account RECIPIENT2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static final org.nem.nis.dbmodel.Account DB_SENDER = new org.nem.nis.dbmodel.Account(SENDER.getAddress().getEncoded(),
			SENDER.getKeyPair().getPublicKey());
	private static final org.nem.nis.dbmodel.Account DB_RECIPIENT1 = new org.nem.nis.dbmodel.Account(RECIPIENT1.getAddress().getEncoded(), null);
	private static final org.nem.nis.dbmodel.Account DB_RECIPIENT2 = new org.nem.nis.dbmodel.Account(RECIPIENT2.getAddress().getEncoded(), null);
	private static final SystemTimeProvider time = new SystemTimeProvider();

	final static Hash DUMMY_PREVIOUS_HASH = Utils.generateRandomHash();
	private static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	static void setFinalStatic(final Field field, final Object newValue) throws Exception {
		field.setAccessible(true);

		// remove final modifier from field
		final Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	@Test
	public void analyzeSavesResults() {
		// Arrange:
		final org.nem.nis.dbmodel.Block block = this.createDummyDbBlock();
		final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(new MockAccountDao(), new MockBlockDao(null));

		// Act:
		blockChainLastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(blockChainLastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
	}

	private Vector<Account> prepareSigners(final AccountAnalyzer accountAnalyzer) {
		final AccountCache accountCache = accountAnalyzer.getAccountCache();
		final PoiFacade poiFacade = accountAnalyzer.getPoiFacade();
		final Vector<Account> accounts = new Vector<>();

		Account a;

		// 0 = signer
		a = Utils.generateRandomAccount();
		accounts.add(a);
		setBalance(a, Amount.fromNem(1_000_000_000), accountCache, poiFacade);

		// 1st sender
		a = Utils.generateRandomAccount();
		accounts.add(a);
		setBalance(a, Amount.fromNem(1_000), accountCache, poiFacade);

		// 1st recipient
		a = Utils.generateRandomAccount();
		accounts.add(a);
		accountCache.addAccountToCache(a.getAddress());

		// 2nd sender
		a = Utils.generateRandomAccount();
		accounts.add(a);
		setBalance(a, Amount.fromNem(1_000), accountCache, poiFacade);

		// 2nd recipient
		a = Utils.generateRandomAccount();
		accounts.add(a);
		accountCache.addAccountToCache(a.getAddress());

		return accounts;
	}

	private static void setBalance(final Account account, final Amount amount, final AccountCache accountCache, final PoiFacade poiFacade) {
		account.incrementBalance(amount);
		final Account cachedAccount = accountCache.addAccountToCache(account.getAddress());

		// since we are assuming that the forager has a balance at block parent, it seems reasonable to assume that
		// it has been added to the cache before and has a non-zero reference count
		cachedAccount.incrementReferenceCount();

		cachedAccount.incrementBalance(amount);
		poiFacade.findStateByAddress(account.getAddress()).getWeightedBalances().addReceive(BlockHeight.ONE, amount);
	}

	@Test
	public void canSuccessfullyProcessBlockAndSiblingWithSameScoreGetsRejectedAfterwards() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final PoiFacade poiFacade = new PoiFacade((blockHeight, accountStates) ->
				accountStates.stream()
						.forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size())));

		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(new AccountCache(), poiFacade);
		final List<Account> accounts = this.prepareSigners(accountAnalyzer);
		for (final Account account : accounts) {
			accountAnalyzer.getPoiFacade().findStateByAddress(account.getAddress()).setHeight(BlockHeight.ONE);
		}

		final Account signer = accounts.get(0);

		final Block parentBlock = createBlock(signer, accountAnalyzer.getAccountCache());
		final BlockScorer scorer = new BlockScorer(accountAnalyzer.getPoiFacade());
		final List<Block> blocks = new LinkedList<>();
		blocks.add(parentBlock);
		final Block block = this.createBlockForTests(accounts, accountAnalyzer, blocks, scorer);

		final AccountDao accountDao = mock(AccountDao.class);
		when(accountDao.getAccountByPrintableAddress(parentBlock.getSigner().getAddress().getEncoded())).thenReturn(
				this.retrieveAccount(1, parentBlock.getSigner())
		);
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);
		final org.nem.nis.dbmodel.Block parent = BlockMapper.toDbModel(parentBlock, accountDaoLookup);

		final MockBlockDao mockBlockDao = new MockBlockDao(parent, null, MockBlockDao.MockBlockDaoMode.MultipleBlocks);
		mockBlockDao.save(parent);
		final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(accountDao, mockBlockDao);
		final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
		final BlockChainServices services =
				new BlockChainServices(
						mockBlockDao,
						new BlockTransactionObserverFactory(),
						NisUtils.createBlockValidatorFactory(),
						transactionValidatorFactory);
		final BlockChain blockChain = new BlockChain(
				accountAnalyzer,
				accountDao,
				blockChainLastBlockLayer,
				mockBlockDao,
				services,
				new UnconfirmedTransactions(new SystemTimeProvider(), transactionValidatorFactory.create(poiFacade)));

		// Act:
		final ValidationResult result = blockChain.processBlock(block);
		final Block savedBlock = BlockMapper.toModel(mockBlockDao.getLastSavedBlock(), accountAnalyzer.getAccountCache());
		TransferTransaction transaction;

		// Assert:
		// TODO: clean up all the accounts and check amount of nems
		// TODO: add all sorts of different checks
		Assert.assertTrue(result == ValidationResult.SUCCESS);
		transaction = (TransferTransaction)savedBlock.getTransactions().get(0);
		Assert.assertThat(transaction.getRecipient().getBalance(), IsEqual.equalTo(Amount.fromNem(17)));
		transaction = (TransferTransaction)savedBlock.getTransactions().get(1);
		Assert.assertThat(transaction.getRecipient().getBalance(), IsEqual.equalTo(Amount.fromNem(290)));

		// siblings with same score must be rejected
		// Act:
		final Block sibling = this.createBlockSiblingWithSameScore(block, parentBlock, accounts);
		final ValidationResult siblingResult = blockChain.processBlock(sibling);

		// Assert:
		Assert.assertTrue(siblingResult == ValidationResult.NEUTRAL);
	}

	private org.nem.nis.dbmodel.Account retrieveAccount(final long i, final Account signer) {
		final org.nem.nis.dbmodel.Account ret = new org.nem.nis.dbmodel.Account(signer.getAddress().getEncoded(), signer.getKeyPair().getPublicKey());
		ret.setId(i);
		return ret;
	}

	private static Block createBlock(final Account forger, final AccountLookup accountLookup) throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final Block block = new Block(forger, DUMMY_PREVIOUS_HASH, DUMMY_GENERATION_HASH, time.getCurrentTime().addHours(-1), new BlockHeight(3));
		block.sign();

		return roundTripBlock(accountLookup, block);
	}

	private static Block roundTripBlock(final AccountLookup accountLookup, final Block block) throws NoSuchFieldException, IllegalAccessException {
		final SerializableEntity entity = block;
		final VerifiableEntity.DeserializationOptions options = VerifiableEntity.DeserializationOptions.VERIFIABLE;

		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, accountLookup);
		final Block b = new Block(deserializer.readInt("type"), options, deserializer);

		Field field = b.getClass().getDeclaredField("generationHash");
		field.setAccessible(true);
		field.set(b, block.getGenerationHash());

		field = b.getClass().getDeclaredField("prevBlockHash");
		field.setAccessible(true);
		field.set(b, block.getPreviousBlockHash());

		return b;
	}

	private TransferTransaction createSignedTransactionWithAmount(final List<Account> accounts, final int i, final long amount, final TimeInstant timeInstant) {
		final TransferTransaction transaction = new TransferTransaction(
				timeInstant,
				accounts.get(i),
				accounts.get(i + 1),
				Amount.fromNem(amount),
				null);
		transaction.setDeadline(timeInstant.addHours(2));
		transaction.sign();

		return transaction;
	}

	private List<BlockDifficulty> createDifficultiesList(final List<Block> blocks) {
		return blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
	}

	private List<TimeInstant> createTimestampsList(final List<Block> blocks) {
		return blocks.stream().map(VerifiableEntity::getTimeStamp).collect(Collectors.toList());
	}

	private Block createBlockForTests(final List<Account> accounts, final AccountAnalyzer accountAnalyzer, final List<Block> blocks, final BlockScorer scorer) throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final Block lastBlock = blocks.get(blocks.size() - 1);
		final Account forger = accounts.get(0);
		Block block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

		final List<Block> historicalBlocks = blocks.subList(Math.max(0, blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION), blocks.size());
		final BlockDifficulty difficulty = scorer.getDifficultyScorer().calculateDifficulty(this.createDifficultiesList(historicalBlocks),
				this.createTimestampsList(historicalBlocks), block.getHeight().getRaw());
		block.setDifficulty(difficulty);
		final BigInteger hit = scorer.calculateHit(block);
		int seconds = hit.multiply(block.getDifficulty().asBigInteger())
				.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BigInteger.valueOf(800000000L)) // this is value of calculateForgerBalance() for current forager
				.intValue();
		seconds += 1;

		final TimeInstant blockTime = new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds);
		block = new Block(forger, lastBlock, blockTime);
		block.setDifficulty(difficulty);

		final TransferTransaction transaction1 = this.createSignedTransactionWithAmount(accounts, 1, 17, blockTime.addMinutes(-2));
		block.addTransaction(transaction1);

		final TransferTransaction transaction2 = this.createSignedTransactionWithAmount(accounts, 3, 290, blockTime.addMinutes(-5));
		block.addTransaction(transaction2);
		block.setDifficulty(new BlockDifficulty(22_222_222_222L));
		block.sign();

		return roundTripBlock(accountAnalyzer.getAccountCache(), block);
	}

	private Block createBlockSiblingWithSameScore(final Block block, final Block parentBlock, final List<Account> accounts) {
		final Account signer = accounts.get(accounts.indexOf(block.getSigner()));
		final Block sibling = new Block(signer, parentBlock, block.getTimeStamp());
		sibling.setDifficulty(block.getDifficulty());
		final TransferTransaction transaction1 = this.createSignedTransactionWithAmount(accounts, 1, 123, block.getTimeStamp().addMinutes(-2));
		sibling.addTransaction(transaction1);
		sibling.sign();

		return sibling;
	}

	private Transaction dummyTransaction(final org.nem.core.model.Account recipient, final long amount) {
		return new TransferTransaction((new SystemTimeProvider()).getCurrentTime(), SENDER, recipient, new Amount(amount), null);
	}

	private org.nem.nis.dbmodel.Block createDummyDbBlock() {
		final Transaction tx1 = this.dummyTransaction(RECIPIENT1, RECIPIENT1_AMOUNT);
		final Transaction tx2 = this.dummyTransaction(RECIPIENT2, RECIPIENT2_AMOUNT);
		tx1.sign();
		tx2.sign();

		final org.nem.core.model.Block b = new org.nem.core.model.Block(
				SENDER,
				Hash.ZERO,
				Hash.ZERO,
				time.getCurrentTime(),
				BlockHeight.ONE);

		b.addTransaction(tx1);
		b.addTransaction(tx2);

		b.sign();

		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
				HashUtils.calculateHash(b),
				1,
				// generation hash
				Hash.ZERO,
				// prev hash
				Hash.ZERO,
				0, // timestamp
				DB_SENDER,
				// proof
				b.getSignature().getBytes(),
				b.getHeight().getRaw(), // height
				RECIPIENT1_AMOUNT + RECIPIENT2_AMOUNT,
				0L,
				123L,
				null
		);

		final Transfer dbTransaction1 = new Transfer(
				HashUtils.calculateHash(tx1),
				tx1.getVersion(),
				tx1.getType(),
				0L,
				0, // timestamp
				0, // deadline
				DB_SENDER,
				// proof
				tx1.getSignature().getBytes(),
				DB_RECIPIENT1,
				0, // index
				RECIPIENT1_AMOUNT,
				0L // referenced tx
		);

		final Transfer dbTransaction2 = new Transfer(
				HashUtils.calculateHash(tx2),
				tx2.getVersion(),
				tx2.getType(),
				0L,
				0, // timestamp
				0, // deadline
				DB_SENDER,
				// proof
				tx1.getSignature().getBytes(),
				DB_RECIPIENT2,
				0, // index
				RECIPIENT2_AMOUNT,
				0L // referenced tx
		);
		dbBlock.setBlockTransfers(Arrays.asList(dbTransaction1, dbTransaction2));

		return dbBlock;
	}
}
