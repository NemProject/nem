package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.nis.poi.PoiImportanceGenerator;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.MockForaging;
import org.nem.core.test.Utils;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ForagingTest {
	private static org.nem.core.model.Account RECIPIENT1 = new org.nem.core.model.Account(Utils.generateRandomAddress());

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
	public void processTransactionsSavesTransactions() throws InterruptedException {
		// Arrange:
		final Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		final Foraging foraging = createMockForaging();
		tx.sign();

		// Act:
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDuplicates() throws InterruptedException {
		// Arrange:
		final Transaction tx = dummyTransaction(RECIPIENT1, 12345);
		final Foraging foraging = createMockForaging();
		tx.sign();

		// Act:
		boolean result1 = foraging.processTransaction(tx);
		boolean result2 = foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(false));
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void processTransactionsDoesNotSaveDoubleSpendTransaction() throws InterruptedException {
		// Arrange (category boost trust attack, stop foraging attack):
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();
		final Account signer = createAccountWithBalance(1000);
		final Foraging foraging = createMockForaging();
		Transaction tx = new TransferTransaction(now, signer, RECIPIENT1, Amount.fromNem(800), null);
		tx.setFee(Amount.fromNem(1));
		tx.setDeadline(now.addMinutes(100));
		tx.sign();

		// Assert:
		Assert.assertThat(tx.isValid(), IsEqual.equalTo(true));

		// Act:
		foraging.processTransaction(tx);
		tx = new TransferTransaction(now.addSeconds(5), signer, RECIPIENT1, Amount.fromNem(800), null);
		tx.setFee(Amount.fromNem(1));
		tx.setDeadline(now.addMinutes(100));
		tx.sign();
		foraging.processTransaction(tx);

		// Assert:
		Assert.assertThat(foraging.getNumUnconfirmedTransactions(), IsEqual.equalTo(1));
	}

	@Test
	public void canProcessTransaction() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();

		// Act:
		Transaction transaction = dummyTransaction(recipient, 123);
		transaction.sign();
		boolean result = foraging.processTransaction(transaction);

		// Assert:
		Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	@Test
	public void cannotProcessSameTransaction() {
		// Arrange:
		final Account signer = createAccountWithBalance(150);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		TransferTransaction transaction = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));

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
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		TransferTransaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now);
		TransferTransaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(20));

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
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));
		Transaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(10), now.addSeconds(2));

		final Hash transactionHash1 = HashUtils.calculateHash(transaction1);
		final Hash transactionHash2 = HashUtils.calculateHash(transaction2);

		boolean result1 = foraging.processTransaction(transaction1);
		boolean result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		// this indicates wrong amounts or fees
		Assert.assertThat(transactionHash1, IsNot.not(IsEqual.equalTo(transactionHash2)));

		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		// higher fee goes first
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	@Test
	public void transactionsForNewBlockAreSortedByTime() {
		// Arrange:
		final Account signer = createAccountWithBalance(400);
		final Account recipient = Utils.generateRandomAccount();
		final Foraging foraging = createMockForaging();
		final TimeInstant now = (new SystemTimeProvider()).getCurrentTime();

		// Act:
		Transaction transaction1 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(2));
		Transaction transaction2 = createSignedTransactionWithTime(signer, recipient, Amount.fromNem(5), now.addSeconds(-2));

		boolean result1 = foraging.processTransaction(transaction1);
		boolean result2 = foraging.processTransaction(transaction2);

		List<Transaction> transactionsList = foraging.getUnconfirmedTransactionsForNewBlock(now.addSeconds(20));

		// Assert
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
		Assert.assertThat(transactionsList.size(), IsEqual.equalTo(2));
		// earlier transaction goes first
		Assert.assertThat(transactionsList.get(0), IsEqual.equalTo(transaction2));
		Assert.assertThat(transactionsList.get(1), IsEqual.equalTo(transaction1));
	}

	private TransferTransaction createSignedTransactionWithTime(Account signer, Account recipient, Amount fee, TimeInstant now) {
		TransferTransaction transaction1 = new TransferTransaction(now, signer, recipient, Amount.fromNem(123), null);
		transaction1.setDeadline(now.addHours(1));
		transaction1.setFee(fee);
		transaction1.sign();
		return transaction1;
	}

	@Test
	public void canSignBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = mock(BlockChainLastBlockLayer.class);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
		final MockForaging foraging = new MockForaging(accountAnalyzer, lastBlockLayer);
		final Account account = Utils.generateRandomAccount();
		accountAnalyzer.addAccountToCache(account.getAddress());
		final Account accountWithoutSecret = accountAnalyzer.findByAddress(account.getAddress());
		accountWithoutSecret.incrementBalance(Amount.fromNem(100));

		final Account signer = createAccountWithBalance(100);
		final TimeInstant parentTime = new TimeInstant(0);
		final Block parent = new Block(
				signer,
				Hash.ZERO,
				Hash.ZERO,
				parentTime,
				BlockHeight.ONE);
		parent.sign();

		// Act:
		final Block block = foraging.createSignedBlock(
				new TimeInstant(10),
				new LinkedList<>(),
				parent,
				account,
				BlockDifficulty.INITIAL_DIFFICULTY);

		// Assert:
		Assert.assertThat(accountWithoutSecret.getKeyPair().getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(accountWithoutSecret));
	}

	private Transaction dummyTransaction(org.nem.core.model.Account recipient, long amount) {
		Transaction transaction = new TransferTransaction(
				(new SystemTimeProvider()).getCurrentTime(),
				createAccountWithBalance(amount*3),
				recipient,
				new Amount(amount),
				null);
		transaction.setDeadline(transaction.getTimeStamp().addHours(1));
		return transaction;
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}

	private Foraging createMockForaging() {
		final BlockChainLastBlockLayer lastBlockLayer = mock(BlockChainLastBlockLayer.class);
		return new MockForaging(null, lastBlockLayer);
	}
}
