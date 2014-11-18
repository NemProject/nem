package org.nem.nis.dao;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.*;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoTest {
	private static final Logger LOGGER = Logger.getLogger(TransferDaoTest.class.getName());
	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	@Test
	public void savingTransferSavesAccounts() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 123);
		final Transfer entity = TransferMapper.toDbModel(transferTransaction, 0, 0, accountDaoLookup);

		// TODO 20141005 J-G since you are doing this everywhere, you might want to consider a TestContext class
		final org.nem.nis.dbmodel.Account dbAccount = accountDaoLookup.findByAddress(sender.getAddress());
		this.addToDummyBlock(dbAccount, entity);

		// Act
		this.transferDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getSender().getId(), notNullValue());
		Assert.assertThat(entity.getRecipient().getId(), notNullValue());
	}

	@Test
	public void canReadSavedData() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 0);
		final Transfer dbTransfer = TransferMapper.toDbModel(transferTransaction, 12345, 0, accountDaoLookup);

		final org.nem.nis.dbmodel.Account dbAccount = accountDaoLookup.findByAddress(sender.getAddress());
		this.addToDummyBlock(dbAccount, dbTransfer);

		// Act
		this.transferDao.save(dbTransfer);
		final Transfer entity = this.transferDao.findByHash(HashUtils.calculateHash(transferTransaction).getRaw());

		// Assert:
		Assert.assertThat(entity, notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbTransfer.getId()));
		Assert.assertThat(entity.getSender().getPublicKey(), equalTo(sender.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getRecipient().getPublicKey(), equalTo(recipient.getKeyPair().getPublicKey()));
		Assert.assertThat(entity.getAmount(), equalTo(transferTransaction.getAmount().getNumMicroNem()));
		Assert.assertThat(entity.getSenderProof(), equalTo(transferTransaction.getSignature().getBytes()));
		// TODO 20141010 J-G why did you remove the blockindex assert (and should we add one for order index)?
	}

	@Test
	public void countReturnsProperValue() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final AccountDaoLookup accountDaoLookup = this.prepareMapping(sender, recipient);
		final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 123);
		final Transfer dbTransfer1 = TransferMapper.toDbModel(transferTransaction, 12345, 0, accountDaoLookup);
		final Transfer dbTransfer2 = TransferMapper.toDbModel(transferTransaction, 12345, 0, accountDaoLookup);
		final Transfer dbTransfer3 = TransferMapper.toDbModel(transferTransaction, 12345, 0, accountDaoLookup);
		final Long initialCount = this.transferDao.count();

		final org.nem.nis.dbmodel.Account dbAccount = accountDaoLookup.findByAddress(sender.getAddress());
		this.addToDummyBlock(dbAccount, dbTransfer1, dbTransfer2, dbTransfer3);

		// Act
		this.transferDao.save(dbTransfer1);
		final Long count1 = this.transferDao.count();
		this.transferDao.save(dbTransfer2);
		final Long count2 = this.transferDao.count();
		this.transferDao.save(dbTransfer3);
		final Long count3 = this.transferDao.count();

		// Assert:
		Assert.assertThat(count1, equalTo(initialCount + 1));
		Assert.assertThat(count2, equalTo(initialCount + 2));
		Assert.assertThat(count3, equalTo(initialCount + 3));
	}

	@Test
	public void getTransactionsForAccountUsingHashRespectsHash() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

		for (int i = 0; i < 30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 123);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccountUsingHash(sender, null, ReadOnlyTransferDao.TransferType.ALL, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccountUsingHash(sender,
				dbBlock.getBlockTransfers().get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.ALL,
				25);
		final Collection<Object[]> entities3 = this.transferDao.getTransactionsForAccountUsingHash(sender,
				dbBlock.getBlockTransfers().get(29).getTransferHash(),
				ReadOnlyTransferDao.TransferType.ALL,
				25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(5));
		Assert.assertThat(entities3.size(), equalTo(0));
	}

	@Test(expected = MissingResourceException.class)
	public void getTransactionsForAccountUsingHashThrowsWhenHashNotFound() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		this.transferDao.getTransactionsForAccountUsingHash(sender, new Hash(new byte[] { 6, 66 }), ReadOnlyTransferDao.TransferType.ALL, 25);
	}

	// TODO-CR: tests like this with a lot of setup can be hard to follow (i know i don't always follow this rule,
	// but it might be consider commenting what this utility function does)
	private Account prepareIncomingOutgoingData() {
		final Account testedAccount = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, testedAccount);
		final Block dummyBlock = new Block(testedAccount, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

		for (int i = 0; i < 100; i++) {
			final Account otherAccount = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, otherAccount);
			if (i % 2 == 0) {
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(testedAccount, otherAccount, 10, 500 + i);
				dummyBlock.addTransaction(transferTransaction);
			} else {
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(otherAccount, testedAccount, 10, 500 + i);
				dummyBlock.addTransaction(transferTransaction);
			}
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);
		return testedAccount;
	}

	// TODO-CR: consider refactoring the three following tests; for these tests, it's probably best to have
	// one test for each entity group (e.g. getTransactionsForAccountUsingHashStrategyIncoming could be split up into three tests - from start, from hash, from end)

	@Test
	public void getTransactionsForAccountUsingHashStrategyIncoming() {
		// Arrange:
		final List<Integer> incomingTimestamps = IntStream.range(0, 50)
				.map(o -> 500 + 2 * (49 - o) + 1)
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = this.prepareIncomingOutgoingData();

		// Act
		final List<Transfer> incomingEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				null,
				ReadOnlyTransferDao.TransferType.INCOMING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> incomingTimeStamps1 = incomingEntities1.stream()
				.map(obj -> obj.getTimeStamp())
				.collect(Collectors.toList());

		final List<Transfer> incomingEntities2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				incomingEntities1.get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.INCOMING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> incomingTimeStamps2 = incomingEntities2.stream()
				.map(obj -> obj.getTimeStamp())
				.collect(Collectors.toList());

		final List<Transfer> incomingEntities3 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				incomingEntities2.get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.INCOMING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(incomingTimeStamps1.size(), equalTo(25));
		Assert.assertThat(incomingTimeStamps1, equalTo(incomingTimestamps.subList(0, 25)));
		Assert.assertThat(incomingTimeStamps2.size(), equalTo(25));
		Assert.assertThat(incomingTimeStamps2, equalTo(incomingTimestamps.subList(25, 50)));
		Assert.assertThat(incomingEntities3.size(), equalTo(0));
	}

	@Test
	public void getTransactionsForAccountUsingHashStrategyOutgoing() {
		// Arrange:
		final List<Integer> outgoingTimestamps = IntStream.range(0, 50)
				.map(o -> 500 + 2 * (49 - o))
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = this.prepareIncomingOutgoingData();

		// Act
		final List<Transfer> outgoingEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				null,
				ReadOnlyTransferDao.TransferType.OUTGOING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> outgoingTimeStamps1 = outgoingEntities1.stream()
				.map(obj -> obj.getTimeStamp())
				.collect(Collectors.toList());

		final List<Transfer> outgoingEntities2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				outgoingEntities1.get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.OUTGOING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> outgoingTimeStamps2 = outgoingEntities2.stream()
				.map(obj -> obj.getTimeStamp())
				.collect(Collectors.toList());

		final List<Transfer> outgoingEntities3 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				outgoingEntities2.get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.OUTGOING,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(outgoingTimeStamps1.size(), equalTo(25));
		Assert.assertThat(outgoingTimeStamps1, equalTo(outgoingTimestamps.subList(0, 25)));
		Assert.assertThat(outgoingTimeStamps2.size(), equalTo(25));
		Assert.assertThat(outgoingTimeStamps2, equalTo(outgoingTimestamps.subList(25, 50)));
		Assert.assertThat(outgoingEntities3.size(), equalTo(0));
	}

	@Test
	public void getTransactionsForAccountUsingHashStrategyAll() {
		// Arrange:
		final List<Integer> outgoingTimestamps = IntStream.range(0, 100)
				.map(o -> 500 + (99 - o))
				.boxed()
				.collect(Collectors.toList());

		final Account testedAccount = this.prepareIncomingOutgoingData();

		// Act
		final List<Transfer> allEntities1 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				null,
				ReadOnlyTransferDao.TransferType.ALL,
				25).stream()
				.map(obj -> (Transfer)obj[0])
				.collect(Collectors.toList());

		final Collection<Integer> allTimeStamps1 = allEntities1.stream()
				.map(obj -> obj.getTimeStamp())
				.collect(Collectors.toList());

		final List<Integer> allTimeStamps2 = this.transferDao.getTransactionsForAccountUsingHash(testedAccount,
				allEntities1.get(24).getTransferHash(),
				ReadOnlyTransferDao.TransferType.ALL,
				25).stream()
				.map(obj -> ((Transfer)obj[0]).getTimeStamp())
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(allTimeStamps1.size(), equalTo(25));
		Assert.assertThat(allTimeStamps1, equalTo(outgoingTimestamps.subList(0, 25)));
		Assert.assertThat(allTimeStamps2.size(), equalTo(25));
		Assert.assertThat(allTimeStamps2, equalTo(outgoingTimestamps.subList(25, 50)));
	}

	private void createTestBlocks(
			final long[] heights,
			final int[] blockTimestamp,
			final int[][] txTimestamps,
			final Account sender,
			final boolean useSenderAsRecipient) {
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);

		this.addMapping(mockAccountDao, sender);

		for (int i = 0; i < heights.length; ++i) {
			final long height = heights[i];
			final int blockTs = blockTimestamp[i];

			final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(blockTs), new BlockHeight(height));

			for (final int txTimestamp : txTimestamps[i]) {
				final Account recipient = useSenderAsRecipient? sender : Utils.generateRandomAccount();
				this.addMapping(mockAccountDao, recipient);
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, txTimestamp);

				// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
				dummyBlock.addTransaction(transferTransaction);
			}
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

			// Act
			this.blockDao.save(dbBlock);
		}
	}

	@Test
	public void getTransactionsForAccountUsingHashReturnsSortedResults() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();

		final List<Long> expectedHeights = Arrays.asList(4l, 4l, 4l, 3l, 3l, 2l, 2l, 1l, 1l);
		final List<Integer> expectedTimestamps = Arrays.asList(1900, 1700, 1700, 1800, 1600, 1500, 1300, 1400, 1200);
		final long heights[] = { 3, 4, 1, 2 };
		final int blockTimestamp[] = { 1801, 1901, 1401, 1501 };
		final int txTimestamps[][] = { { 1800, 1600 }, { 1900, 1700, 1700 }, { 1200, 1400 }, { 1300, 1500 } };
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, sender, false);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccountUsingHash(sender, null, ReadOnlyTransferDao.TransferType.ALL, 25);

		final List<Long> resultHeights = entities1.stream().map(obj -> (Long)obj[1]).collect(Collectors.toList());
		final List<Integer> resultTimestamps = entities1.stream().map(obj -> ((Transfer)obj[0]).getTimeStamp()).collect(Collectors.toList());

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(9));
		Assert.assertThat(resultHeights, equalTo(expectedHeights));
		Assert.assertThat(resultTimestamps, equalTo(expectedTimestamps));
	}

	@Test
	public void getTransactionsForAccountUsingHashFiltersDuplicatesIfTransferTypeIsAll() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final long heights[] = { 3 };
		final int blockTimestamp[] = { 1801 };
		final int txTimestamps[][] = { { 1800 } };
		this.createTestBlocks(heights, blockTimestamp, txTimestamps, sender, true);

		// Act (transaction is pulled 2 times from db, one should get filtered):
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccountUsingHash(sender, null, ReadOnlyTransferDao.TransferType.ALL, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(1));
	}

	@Test
	public void getTransactionsForAccountRespectsTimestamp() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);
		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123), BlockHeight.ONE);

		for (int i = 0; i < 30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 123);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccount(sender, 123, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccount(sender, 122, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(0));
	}

	@Test
	public void getTransactionsForAccountReturnsTransactionsSortedByTime() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);

		final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(123 + 30), BlockHeight.ONE);
		for (int i = 0; i < 30; i++) {
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			// pseudorandom times
			final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, 100 + (i * 23 + 3) % 30);

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.addTransaction(transferTransaction);
		}
		dummyBlock.sign();
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);

		// Act
		this.blockDao.save(dbBlock);

		// Act
		final Collection<Object[]> entities1 = this.transferDao.getTransactionsForAccount(sender, 100 + 30, 25);
		final Collection<Object[]> entities2 = this.transferDao.getTransactionsForAccount(sender, 99, 25);

		// Assert:
		Assert.assertThat(entities1.size(), equalTo(25));
		Assert.assertThat(entities2.size(), equalTo(0));
		int lastTimestamp = 100 + 29;
		for (final Object[] entity : entities1) {
			Assert.assertThat(((Transfer)entity[0]).getTimeStamp(), equalTo(lastTimestamp));
			lastTimestamp = lastTimestamp - 1;
		}
	}

	@Test
	public void findByHashReturnsTransferIfMaxBlockHeightIsGreaterOrEqualToTransferBlockHeight() {
		// Arrange:
		final List<Hash> hashes = this.saveThreeBlocksWithTransactionsInDatabase(1);

		// Act: second parameter is maximum block height
		final Transfer transfer1_1 = this.transferDao.findByHash(hashes.get(0).getRaw(), 1);
		final Transfer transfer1_2 = this.transferDao.findByHash(hashes.get(0).getRaw(), 2);
		final Transfer transfer1_3 = this.transferDao.findByHash(hashes.get(0).getRaw(), 3);
		final Transfer transfer2 = this.transferDao.findByHash(hashes.get(1).getRaw(), 2);
		final Transfer transfer3 = this.transferDao.findByHash(hashes.get(2).getRaw(), 3);

		// Assert:
		Assert.assertThat(transfer1_1, IsNull.notNullValue());
		Assert.assertThat(transfer1_2, IsNull.notNullValue());
		Assert.assertThat(transfer1_3, IsNull.notNullValue());
		Assert.assertThat(transfer2, IsNull.notNullValue());
		Assert.assertThat(transfer3, IsNull.notNullValue());
	}

	@Test
	public void findByHashReturnsNullIfMaxBlockHeightIsLessThanTransferBlockHeight() {
		// Arrange:
		final List<Hash> hashes = this.saveThreeBlocksWithTransactionsInDatabase(1);

		// Act: second parameter is maximum block height
		final Transfer transfer1 = this.transferDao.findByHash(hashes.get(1).getRaw(), 1);
		final Transfer transfer2 = this.transferDao.findByHash(hashes.get(2).getRaw(), 2);

		// Assert:
		Assert.assertThat(transfer1, IsNull.nullValue());
		Assert.assertThat(transfer2, IsNull.nullValue());
	}

	@Test
	public void findByHashReturnsNullIfHashDoesNotExistInDatabase() {
		// Arrange:
		this.saveThreeBlocksWithTransactionsInDatabase(1);

		// Act: second parameter is maximum block height
		final Transfer transfer = this.transferDao.findByHash(Utils.generateRandomHash().getRaw(), 3);

		// Assert:
		Assert.assertThat(transfer, IsNull.nullValue());
	}

	// TODO 20141029 BR -> J: you want the same tests for importance transfer dao
	// TODO 20141029 J -> BR: yes ... i think we need some refactoring of the transfer daos
	@Test
	public void anyHashExistsReturnsFalseIfNoneOfTheHashesExistInDatabase() {
		// Arrange:
		this.saveThreeBlocksWithTransactionsInDatabase(3);
		final Collection<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			// TODO 20141029 BR -> all: why does Utils.generateRandomHash() create a 64 byte hash?
			hashes.add(new Hash(Utils.generateRandomBytes(32)));
		}

		// Act: second parameter is maximum block height
		final boolean exists = this.transferDao.anyHashExists(hashes, new BlockHeight(3));

		// Assert:
		Assert.assertThat(exists, IsEqual.equalTo(false));
	}

	@Test
	public void anyHashExistsReturnsTrueIfAtLeastOneOfTheHashesExistInDatabase() {
		// Arrange:
		final List<Hash> transactionHashes = this.saveThreeBlocksWithTransactionsInDatabase(3);
		final Collection<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			hashes.add(new Hash(Utils.generateRandomBytes(32)));
		}
		hashes.add(transactionHashes.get(3));

		// Act: second parameter is maximum block height
		final boolean exists = this.transferDao.anyHashExists(hashes, new BlockHeight(3));

		// Assert:
		Assert.assertThat(exists, IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsFalseIfNoneOfTheHashesIsContainedInABlockUpToMaxBlockHeight() {
		// Arrange:
		final List<Hash> transactionHashes = this.saveThreeBlocksWithTransactionsInDatabase(3);
		final Collection<Hash> hashes = new ArrayList<>();
		hashes.add(transactionHashes.get(6));
		hashes.add(transactionHashes.get(7));
		hashes.add(transactionHashes.get(8));

		// Act: second parameter is maximum block height
		final boolean exists = this.transferDao.anyHashExists(hashes, new BlockHeight(2));

		// Assert:
		Assert.assertThat(exists, IsEqual.equalTo(false));
	}

	@Test
	public void anyHashExistsReturnsFalseIfHashCollectionIsEmpty() {
		// Arrange:
		this.saveThreeBlocksWithTransactionsInDatabase(3);
		final Collection<Hash> hashes = new ArrayList<>();

		// Act: second parameter is maximum block height
		final boolean exists = this.transferDao.anyHashExists(hashes, new BlockHeight(3));

		// Assert:
		Assert.assertThat(exists, IsEqual.equalTo(false));
	}

	// TODO: Move to integration test?
	@Test
	public void anyHashExistsIsFastEnough() {
		// Arrange:
		this.saveThreeBlocksWithTransactionsInDatabase(10000);
		final Collection<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			hashes.add(new Hash(Utils.generateRandomBytes(32)));
		}

		// Act: second parameter is maximum block height
		final long start = System.currentTimeMillis();
		final boolean exists = this.transferDao.anyHashExists(hashes, new BlockHeight(3));
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("anyHashExists need %dms.", stop - start));

		// Assert:
		Assert.assertThat(exists, IsEqual.equalTo(false));
		Assert.assertThat(stop - start < 1000, IsEqual.equalTo(true));
	}

	private List<Hash> saveThreeBlocksWithTransactionsInDatabase(final int transactionsPerBlock) {
		final List<Hash> hashes = new ArrayList<>();
		final Account sender = Utils.generateRandomAccount();
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final AccountDaoLookup accountDaoLookup = new AccountDaoLookupAdapter(mockAccountDao);
		this.addMapping(mockAccountDao, sender);

		for (int i = 1; i < 4; i++) {
			final Block dummyBlock = new Block(sender, Hash.ZERO, Hash.ZERO, new TimeInstant(i * 123), new BlockHeight(i));
			final Account recipient = Utils.generateRandomAccount();
			this.addMapping(mockAccountDao, recipient);
			for (int j = 0; j < transactionsPerBlock; j++) {
				final TransferTransaction transferTransaction = this.prepareTransferTransaction(sender, recipient, 10, i * 123);
				final Transfer dbTransfer = TransferMapper.toDbModel(transferTransaction, 12345, i - 1, accountDaoLookup);
				hashes.add(dbTransfer.getTransferHash());
				dummyBlock.addTransaction(transferTransaction);
			}

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.sign();
			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(dummyBlock, accountDaoLookup);
			this.blockDao.save(dbBlock);
		}

		return hashes;
	}

	private TransferTransaction prepareTransferTransaction(final Account sender, final Account recipient, final long amount, final int i) {
		// Arrange:
		final TransferTransaction transferTransaction = new TransferTransaction(
				new TimeInstant(i),
				sender,
				recipient,
				Amount.fromNem(amount),
				null);
		transferTransaction.sign();
		return transferTransaction;
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
	}

	private AccountDaoLookup prepareMapping(final Account sender, final Account recipient) {
		// Arrange:
		final MockAccountDao mockAccountDao = new MockAccountDao();
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
		final org.nem.nis.dbmodel.Account dbRecipient = new org.nem.nis.dbmodel.Account(recipient.getAddress().getEncoded(),
				recipient.getKeyPair().getPublicKey());
		mockAccountDao.addMapping(sender, dbSender);
		mockAccountDao.addMapping(recipient, dbRecipient);
		return new AccountDaoLookupAdapter(mockAccountDao);
	}

	private void addToDummyBlock(final org.nem.nis.dbmodel.Account account, final Transfer... dbTransfers) {
		final org.nem.nis.dbmodel.Block block = new org.nem.nis.dbmodel.Block(Hash.ZERO, 1, Hash.ZERO, Hash.ZERO, 1,
				account, new byte[] { 1, 2, 3, 4 },
				1L, 1L, 1L, 123L, null);
		this.blockDao.save(block);

		for (final Transfer transfer : dbTransfers) {
			transfer.setBlock(block);
		}
	}
}
