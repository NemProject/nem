package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.*;
import org.nem.core.model.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.core.test.Utils;
import org.nem.core.time.SystemTimeProvider;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.MockAccountDao;
import org.nem.nis.test.MockBlockDao;
import org.nem.nis.test.MockForaging;
import org.nem.peer.NodeInteractionResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

public class BlockChainTest {
	public static final long RECIPIENT1_AMOUNT = 3 * 1000000L;
	public static final long RECIPIENT2_AMOUNT = 5 * 1000000L;
	private static org.nem.core.model.Account SENDER = Utils.generateRandomAccount();
	private static org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account RECIPIENT2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.nis.dbmodel.Account DB_SENDER = new org.nem.nis.dbmodel.Account(SENDER.getAddress().getEncoded(), SENDER.getKeyPair().getPublicKey());
	private static org.nem.nis.dbmodel.Account DB_RECIPIENT1 = new org.nem.nis.dbmodel.Account(RECIPIENT1.getAddress().getEncoded(), null);
	private static org.nem.nis.dbmodel.Account DB_RECIPIENT2 = new org.nem.nis.dbmodel.Account(RECIPIENT2.getAddress().getEncoded(), null);
	private static final SystemTimeProvider time = new SystemTimeProvider();

	final static Hash DUMMY_PREVIOUS_HASH = Utils.generateRandomHash();
	private static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	static void setFinalStatic(Field field, Object newValue) throws Exception {
		field.setAccessible(true);

		// remove final modifier from field
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		// TODO: is there some way to use mockito for this?
		setFinalStatic(NisMain.class.getField("TIME_PROVIDER"), new SystemTimeProvider());
	}

	@Test
	public void analyzeSavesResults() {
		// Arrange:
		org.nem.nis.dbmodel.Block block = createDummyDbBlock();
		BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(new MockAccountDao(), new MockBlockDao(null));

		// Act:
		blockChainLastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(blockChainLastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
	}

	private Vector<Account> prepareSigners(AccountAnalyzer accountAnalyzer) {
		Vector<Account> accounts = new Vector<>();

		Account a;
		Account temp;

		// 0 = signer
		a = Utils.generateRandomAccount();
		accounts.add(a);
		a.incrementBalance(Amount.fromNem(1_000_000_000));
		temp = accountAnalyzer.addAccountToCache(a.getAddress());
		temp.incrementBalance(Amount.fromNem(1_000_000_000));
		temp.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(1_000_000_000));

		// 1st sender
		a = Utils.generateRandomAccount();
		accounts.add(a);
		a.incrementBalance(Amount.fromNem(1_000));
		temp = accountAnalyzer.addAccountToCache(a.getAddress());
		temp.incrementBalance(Amount.fromNem(1_000));
		temp.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(1_000));

		// 1st recipient
		a = Utils.generateRandomAccount();
		accounts.add(a);
		accountAnalyzer.addAccountToCache(a.getAddress());

		// 2nd sender
		a = Utils.generateRandomAccount();
		accounts.add(a);
		a.incrementBalance(Amount.fromNem(1_000));
		a.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(1_000));
		temp = accountAnalyzer.addAccountToCache(a.getAddress());
		temp.incrementBalance(Amount.fromNem(1_000));
		temp.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(1_000));

		// 2nd recipient
		a = Utils.generateRandomAccount();
		accounts.add(a);
		accountAnalyzer.addAccountToCache(a.getAddress());

		return accounts;
	}

	@Test
	public void canSuccessfullyProcessBlock() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer((blockHeight, accounts, scoringAlg) -> {
			ColumnVector importances = new ColumnVector(accounts.size());
			importances.setAll(1.0 / accounts.size());
			return importances;
		});

		final List<Account> accounts = prepareSigners(accountAnalyzer);
		final Account signer = accounts.get(0);

		final Block parentBlock = createBlock(signer, accountAnalyzer);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);
		final List<Block> blocks = new LinkedList<>();
		blocks.add(parentBlock);
		final Block block = createBlockForTests(accounts, accountAnalyzer, blocks, scorer);
		for (final Account account : accountAnalyzer)
			account.setHeight(BlockHeight.ONE);

		final AccountDao accountDao = mock(AccountDao.class);
		when(accountDao.getAccountByPrintableAddress(parentBlock.getSigner().getAddress().getEncoded())).thenReturn(
				retrieveAccount(1, parentBlock.getSigner())
		);
		final AccountDaoLookupAdapter accountDaoLookup = new AccountDaoLookupAdapter(accountDao);
		org.nem.nis.dbmodel.Block parent = BlockMapper.toDbModel(parentBlock, accountDaoLookup);

		final MockBlockDao mockBlockDao = new MockBlockDao(parent, null, MockBlockDao.MockBlockDaoMode.MultipleBlocks);
		mockBlockDao.save(parent);
		final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(accountDao, mockBlockDao);
		final Foraging foraging = new MockForaging(accountAnalyzer, blockChainLastBlockLayer);
		final BlockChain blockChain = new BlockChain(accountAnalyzer, accountDao, blockChainLastBlockLayer, mockBlockDao, foraging);

		// Act:
		Assert.assertThat(NisMain.TIME_PROVIDER, IsNot.not( IsNull.nullValue() ));
		NodeInteractionResult result = blockChain.processBlock(block);
		Block savedBlock = BlockMapper.toModel(mockBlockDao.getLastSavedBlock(), accountAnalyzer);
		TransferTransaction transaction;

		// Assert:
		// TODO: clean up all the accounts and check amount of nems
		// TODO: add all sorts of different checks
		Assert.assertTrue(result == NodeInteractionResult.SUCCESS);
		transaction = (TransferTransaction)savedBlock.getTransactions().get(0);
		Assert.assertThat(transaction.getRecipient().getBalance(), IsEqual.equalTo(Amount.fromNem(17)));
		transaction = (TransferTransaction)savedBlock.getTransactions().get(1);
		Assert.assertThat(transaction.getRecipient().getBalance(), IsEqual.equalTo(Amount.fromNem(290)));
	}

	private org.nem.nis.dbmodel.Account retrieveAccount(long i, Account signer) {
		org.nem.nis.dbmodel.Account ret = new org.nem.nis.dbmodel.Account(signer.getAddress().getEncoded(), signer.getKeyPair().getPublicKey());
		ret.setId(i);
		return ret;
	}

	private static Block createBlock(final Account forger, final AccountLookup accountLookup) throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		Block block = new Block(forger, DUMMY_PREVIOUS_HASH, DUMMY_GENERATION_HASH, time.getCurrentTime(), new BlockHeight(3));
		block.sign();

		return roundTripBlock(accountLookup, block);
	}

	private static Block roundTripBlock(AccountLookup accountLookup, Block block) throws NoSuchFieldException, IllegalAccessException {
		final SerializableEntity entity = block;
		final VerifiableEntity.DeserializationOptions options = VerifiableEntity.DeserializationOptions.VERIFIABLE;

		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, accountLookup);
		Block b = new Block(deserializer.readInt("type"), options, deserializer);

		Field field = b.getClass().getDeclaredField("generationHash");
		field.setAccessible(true);
		field.set(b, block.getGenerationHash());

		field = b.getClass().getDeclaredField("prevBlockHash");
		field.setAccessible(true);
		field.set(b, block.getPreviousBlockHash());

		return b;
	}

	private TransferTransaction createSignedTransactionWithAmount(List<Account> accounts, int i, AccountAnalyzer accountAnalyzer, long amount, TimeInstant timeInstant) {
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

	private List<BlockDifficulty> createDifficultiesList(List<Block> blocks) {
		return blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
	}

	private List<TimeInstant> createTimestampsList(List<Block> blocks) {
		return blocks.stream().map(VerifiableEntity::getTimeStamp).collect(Collectors.toList());
	}

	private Block createBlockForTests(List<Account> accounts, final AccountAnalyzer accountAnalyzer, final List<Block> blocks, final BlockScorer scorer) throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final Block lastBlock = blocks.get(blocks.size()-1);
		final Account forger = accounts.get(0);
		Block block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

		List<Block> historicalBlocks = blocks.subList(Math.max(0, blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION), blocks.size());
		final BlockDifficulty difficulty = scorer.calculateDifficulty(createDifficultiesList(historicalBlocks), createTimestampsList(historicalBlocks));
		block.setDifficulty(difficulty);
		BigInteger hit = scorer.calculateHit(block);
		int seconds = hit.multiply(block.getDifficulty().asBigInteger())
				.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BigInteger.valueOf(forger.getBalance().getNumNem()))
				.intValue();
		seconds += 1;

		TimeInstant blockTime = new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds);
		block = new Block(forger, lastBlock, blockTime);
		block.setDifficulty(difficulty);

		final TransferTransaction transaction1 = createSignedTransactionWithAmount(accounts, 1, accountAnalyzer, 17, blockTime.addMinutes(-2));
		block.addTransaction(transaction1);

		final TransferTransaction transaction2 = createSignedTransactionWithAmount(accounts, 3, accountAnalyzer, 290, blockTime.addMinutes(-5));
		block.addTransaction(transaction2);
		block.setDifficulty(new BlockDifficulty(22_222_222_222L));
		block.sign();

		return roundTripBlock(accountAnalyzer, block);
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		return new TransferTransaction((new SystemTimeProvider()).getCurrentTime(), SENDER, recipient, new Amount(amount), null);
	}

	private org.nem.nis.dbmodel.Block createDummyDbBlock() {
		Transaction tx1 = dummyTransaction(RECIPIENT1, RECIPIENT1_AMOUNT);
		Transaction tx2 = dummyTransaction(RECIPIENT2, RECIPIENT2_AMOUNT);
		tx1.sign();
		tx2.sign();

		org.nem.core.model.Block b = new org.nem.core.model.Block(
				SENDER,
				Hash.ZERO,
				Hash.ZERO,
				time.getCurrentTime(),
				BlockHeight.ONE);

		b.addTransaction(tx1);
		b.addTransaction(tx2);

		b.sign();

		org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
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
				123L
		);

		Transfer dbTransaction1 = new Transfer(
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

		Transfer dbTransaction2 = new Transfer(
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
