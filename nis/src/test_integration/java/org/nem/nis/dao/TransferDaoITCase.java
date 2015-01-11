package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.logging.Logger;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoITCase {
	private static final Logger LOGGER = Logger.getLogger(TransferDaoITCase.class.getName());

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

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
				final DbTransferTransaction dbTransfer = MapperUtils.createModelToDbModelMapper(accountDaoLookup)
						.map(transferTransaction, DbTransferTransaction.class);
				dbTransfer.setBlkIndex(12345);
				dbTransfer.setOrderId(i - 1);
				hashes.add(dbTransfer.getTransferHash());
				dummyBlock.addTransaction(transferTransaction);
			}

			// need to wrap it in block, cause getTransactionsForAccount returns also "owning" block's height
			dummyBlock.sign();
			final DbBlock dbBlock = MapperUtils.toDbModel(dummyBlock, accountDaoLookup);
			this.blockDao.save(dbBlock);
		}

		return hashes;
	}

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final DbAccount dbSender = new DbAccount(account.getAddress().getEncoded(), account.getAddress().getPublicKey());
		mockAccountDao.addMapping(account, dbSender);
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
}
