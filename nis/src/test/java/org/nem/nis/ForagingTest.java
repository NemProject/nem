package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.*;
import org.nem.core.test.MockAccount;
import org.nem.nis.test.MockAccountAnalyzer;
import org.nem.nis.test.MockForaging;
import org.nem.core.test.Utils;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;
import org.nem.core.model.TransferTransaction;

import java.util.LinkedList;
import java.util.List;

public class ForagingTest {
	private static org.nem.core.model.Account SENDER = new MockAccount(Address.fromEncoded(GenesisBlock.ACCOUNT.getAddress().getEncoded()));
	private static org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());

	@Test
	public void processTransactionsSavesTransactions() throws InterruptedException {
		// Arrange:
		Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		Foraging foraging = new MockForaging();
		tx.sign();

		// Act:
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDuplicates() throws InterruptedException {
		// Arrange:
		Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		Foraging foraging = new MockForaging();
		tx.sign();

		// Act:
		foraging.processTransaction(tx);
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void canProcessTransaction() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = new MockForaging();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();

		// Act:
		TransferTransaction transaction = new TransferTransaction(systemTimeProvider.getCurrentTime(), signer, recipient, new Amount(123), null);
		transaction.sign();
		boolean result = foraging.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void cannotProcessSameTransaction() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = new MockForaging();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();

		// Act:
		TransferTransaction transaction = new TransferTransaction(systemTimeProvider.getCurrentTime(), signer, recipient, new Amount(123), null);
		transaction.sign();

		boolean result1 = foraging.processTransaction(transaction);
		boolean result2 = foraging.processTransaction(transaction);

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
		final Foraging foraging = new MockForaging();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		TransferTransaction transaction1 = new TransferTransaction(now, signer, recipient, new Amount(123), null);
		transaction1.sign();
		TransferTransaction transaction2 = new TransferTransaction(now.addSeconds(20), signer, recipient, new Amount(123), null);
		transaction2.sign();

		boolean result1 = foraging.processTransaction(transaction1);
		boolean result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(10));

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
		final Foraging foraging = new MockForaging();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		Transaction transaction1 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(10));
		transaction1.sign();
		Transaction transaction2 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction2.sign();

		boolean result1 = foraging.processTransaction(transaction1);
		boolean result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

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
		final Foraging foraging = new MockForaging();
		final SystemTimeProvider systemTimeProvider = new SystemTimeProvider();
		final TimeInstant now = systemTimeProvider.getCurrentTime();

		// Act:
		Transaction transaction1 = new TransferTransaction(now.addSeconds(2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction1.sign();
		Transaction transaction2 = new TransferTransaction(now.addSeconds(-2), signer, recipient, new Amount(123), null);
		transaction1.setFee(new Amount(5));
		transaction2.sign();

		boolean result1 = foraging.processTransaction(transaction1);
		boolean result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	@Test
	public void canSignBlock() {
		// Arrange:
		final MockForaging foraging = new MockForaging();
		final MockAccountAnalyzer mockAccountAnalyzer = new MockAccountAnalyzer();
		final Account account = Utils.generateRandomAccount();
		mockAccountAnalyzer.initializeGenesisAccount(account);
		final Account accountWithoutSecret = mockAccountAnalyzer.findByNemAddress(account);
		accountWithoutSecret.incrementBalance(Amount.fromNem(100));

		final Account signer = createAccountWithBalance(100);
		final TimeInstant parentTime = new TimeInstant(0);
		final Block parent = new Block(signer, new Hash(new byte[32]), parentTime, BlockHeight.ONE);
		parent.sign();

		// Act:
		foraging.setAccountAnalyzer(mockAccountAnalyzer);
		final Block block = foraging.createSignedBlock(new TimeInstant(10), new LinkedList<Transaction>(), parent, account, BlockDifficulty.INITIAL_DIFFICULTY);

		// Assert:
		Assert.assertThat(accountWithoutSecret.getKeyPair().getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(accountWithoutSecret));
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		return new TransferTransaction((new SystemTimeProvider()).getCurrentTime(), SENDER, recipient, new Amount(amount), null);
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
