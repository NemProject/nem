package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.TransferTransaction;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.AccountDaoLookup;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.mappers.TransferMapper;
import org.nem.nis.test.MockAccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@ContextConfiguration(classes = IntegrationTestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TransferDaoITCase {
	private static final Logger LOGGER = Logger.getLogger(TransferDaoITCase.class.getName());

	@Autowired
	TransferDao transferDao;

	@Autowired
	BlockDao blockDao;

	// TODO: Move to integration test?
	// TODO 20141205 BR: I guess we can delete this since we have the transaction cache
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

	private void addMapping(final MockAccountDao mockAccountDao, final Account account) {
		final org.nem.nis.dbmodel.Account dbSender = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getAddress().getPublicKey());
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
