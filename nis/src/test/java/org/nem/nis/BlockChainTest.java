package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.dbmodel.Block;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.test.MockAccount;
import org.nem.core.test.MockBlockChain;
import org.nem.core.test.Utils;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.BlockChain;

import java.util.Arrays;
import java.util.List;

public class BlockChainTest {
	public static final long RECIPIENT1_AMOUNT = 3 * 1000000L;
	public static final long RECIPIENT2_AMOUNT = 5 * 1000000L;
	private static org.nem.core.model.Account sender = new MockAccount(Address.fromEncoded(GenesisBlock.ACCOUNT.getAddress().getEncoded()));
	private static org.nem.core.model.Account recipient1 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.model.Account recipient2 = new org.nem.core.model.Account(Utils.generateRandomAddress());
	private static org.nem.core.dbmodel.Account dbSender = new org.nem.core.dbmodel.Account(sender.getAddress().getEncoded(), sender.getKeyPair().getPublicKey());
	private static org.nem.core.dbmodel.Account dbRecipient1 = new org.nem.core.dbmodel.Account(recipient1.getAddress().getEncoded(), null);
	private static org.nem.core.dbmodel.Account dbRecipient2 = new org.nem.core.dbmodel.Account(recipient2.getAddress().getEncoded(), null);
	private static final SystemTimeProvider time = new SystemTimeProvider();

	@Test
	public void analyzeSavesResults() {
		// Arrange:
		Block block = createDummyDbBlock();
		BlockChain blockChain = new MockBlockChain();
		blockChain.bootup();

		// Act:
		blockChain.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(blockChain.getLastBlockHeight(), IsEqual.equalTo(1L));
		Assert.assertThat(blockChain.getLastBlockHash(), IsEqual.equalTo(block.getBlockHash()));
		Assert.assertThat(blockChain.getLastBlockSignature(), IsEqual.equalTo(block.getForgerProof()));
	}

	@Test
	public void processTransactionsSavesTransactions() throws InterruptedException {
		// Arrange:
		Transaction tx = dummyTransaction(recipient1, 12345);
		BlockChain blockChain = new MockBlockChain();
		tx.sign();

		// Act:
		blockChain.processTransaction(tx);

		// Assert:
		Assert.assertThat(blockChain.getUnconfirmedTransactions().size(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDuplicates() throws InterruptedException {
		// Arrange:
		Transaction tx = dummyTransaction(recipient1, 12345);
		BlockChain blockChain = new MockBlockChain();
		tx.sign();

		// Act:
		blockChain.processTransaction(tx);
		blockChain.processTransaction(tx);

		// Assert:
		Assert.assertThat(blockChain.getUnconfirmedTransactions().size(), IsEqual.equalTo(1));
	}

	@Test
	public void canProcessTransaction() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new MockBlockChain();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();

		// Act:
		TransferTransaction transaction = new TransferTransaction(systemTimeProvider.getCurrentTime(), signer, recipient, new Amount(123), null);
		transaction.sign();
		boolean result = blockChain.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void cannotProcessSameTransaction() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new MockBlockChain();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();

		// Act:
		TransferTransaction transaction = new TransferTransaction(systemTimeProvider.getCurrentTime(), signer, recipient, new Amount(123), null);
		transaction.sign();

		boolean result1 = blockChain.processTransaction(transaction);
		boolean result2 = blockChain.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void transactionsForNewBlockHappenedBeforeBlock() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new MockBlockChain();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		TransferTransaction transaction1 = new TransferTransaction(now, signer, recipient, new Amount(123), null);
		transaction1.sign();
		TransferTransaction transaction2 = new TransferTransaction(now.addSeconds(20), signer, recipient, new Amount(123), null);
		transaction2.sign();

		boolean result1 = blockChain.processTransaction(transaction1);
		boolean result2 = blockChain.processTransaction(transaction2);

		List<Transaction> transactionsList = blockChain.getUnconfirmedTransactionsForNewBlock(now.addSeconds(10));

		// Assert
		Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));
		Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(1));
	}

	@Test
	public void transactionsForNewBlockAreSortedByFee() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new MockBlockChain();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		Transaction transaction1 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(10));
		transaction1.sign();
		Transaction transaction2 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction2.sign();

		boolean result1 = blockChain.processTransaction(transaction1);
		boolean result2 = blockChain.processTransaction(transaction2);

		List<Transaction> transactionsList = blockChain.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	@Test
	public void transactionsForNewBlockAreSortedByTime() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BlockChain blockChain = new MockBlockChain();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		Transaction transaction1 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction1.sign();
		Transaction transaction2 = new TransferTransaction(now.addSeconds(-2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction2.sign();

		boolean result1 = blockChain.processTransaction(transaction1);
		boolean result2 = blockChain.processTransaction(transaction2);

		List<Transaction> transactionsList = blockChain.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		return new TransferTransaction((new SystemTimeProvider()).getCurrentTime(), sender, recipient, new Amount(amount), null);
	}

	private Block createDummyDbBlock() {
		Transaction tx1 = dummyTransaction(recipient1, RECIPIENT1_AMOUNT);
		Transaction tx2 = dummyTransaction(recipient2, RECIPIENT2_AMOUNT);
		tx1.sign();
		tx2.sign();

		org.nem.core.model.Block b = new org.nem.core.model.Block(
				sender,
				new byte[32],
				time.getCurrentTime(),
				1L
		);

		b.addTransaction(tx1);
		b.addTransaction(tx2);

		b.sign();

		Block dbBlock = new Block(
				123456789L,
				1,
				// prev hash
				new byte[] {
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
				},
				HashUtils.calculateHash(b),
				0, // timestamp
				dbSender,
				// proof
				b.getSignature().getBytes(),
				b.getHeight(), // height
				RECIPIENT1_AMOUNT + RECIPIENT2_AMOUNT,
				0L
		);

		Transfer dbTransaction1 = new Transfer(
				ByteUtils.bytesToLong(HashUtils.calculateHash(tx1)),
				HashUtils.calculateHash(tx1),
				tx1.getVersion(),
				tx1.getType(),
				0L,
				0, // timestamp
				0, // deadline
				dbSender,
				// proof
				tx1.getSignature().getBytes(),
				dbRecipient1,
				0, // index
				RECIPIENT1_AMOUNT,
				0L // referenced tx
		);

		Transfer dbTransaction2 = new Transfer(
				ByteUtils.bytesToLong(HashUtils.calculateHash(tx2)),
				HashUtils.calculateHash(tx2),
				tx2.getVersion(),
				tx2.getType(),
				0L,
				0, // timestamp
				0, // deadline
				dbSender,
				// proof
				tx1.getSignature().getBytes(),
				dbRecipient2,
				0, // index
				RECIPIENT2_AMOUNT,
				0L // referenced tx
		);
		dbBlock.setBlockTransfers(Arrays.asList(dbTransaction1, dbTransaction2));

		return dbBlock;
	}
}
