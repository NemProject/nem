package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransactionTest {

	//region Constructors

	@Test
	public void ctorCanCreateTransactionForAccountWithSignerPrivateKey() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer, 6);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(MockTransaction.TIMESTAMP));
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(MockTransaction.DEADLINE));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
	}

	@Test
	public void transactionCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
		final MockTransaction originalTransaction = new MockTransaction(signer, 7);
		originalTransaction.setFee(new Amount(130));
		final MockTransaction transaction = createRoundTrippedTransaction(originalTransaction, signerPublicKeyOnly);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(new Amount(130L)));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(MockTransaction.TIMESTAMP));
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(MockTransaction.DEADLINE));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(7));
	}

	//endregion

	//region Deadline

	@Test
	public void transactionDeadlineCanBeSet() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(726));

		// Assert:
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(new TimeInstant(726)));
	}

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionWithLessThanMinimumDeadlineIsInvalid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp());

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
	}

	@Test
	public void transactionWithMinimumDeadlineIsValid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionWithMaximumDeadlineIsValid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1));

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addDays(1).addSeconds(1));

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
	}

	//endregion

	//region Comparable

	@Test
	public void compareResultIsZeroForTransactionsThatHaveAllPrimaryFieldsEqual() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(7, 12, new TimeInstant(124), 70);
		final MockTransaction transaction2 = new MockTransaction(7, 12, new TimeInstant(124), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(0));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(0));
	}

	@Test
	public void compareResultIsInfluencedByType() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(7, 14, new TimeInstant(150), 90);
		final MockTransaction transaction2 = new MockTransaction(11, 12, new TimeInstant(124), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(-1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(1));
	}

	@Test
	public void compareResultIsInfluencedByVersion() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(150), 90);
		final MockTransaction transaction2 = new MockTransaction(11, 20, new TimeInstant(124), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(-1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(1));
	}

	@Test
	public void compareResultIsInfluencedByTimeStamp() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(150), 90);
		final MockTransaction transaction2 = new MockTransaction(11, 14, new TimeInstant(200), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(-1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(1));
	}

	@Test
	public void compareResultIsInfluencedByFee() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(200), 50);
		final MockTransaction transaction2 = new MockTransaction(11, 14, new TimeInstant(200), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(-1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(1));
	}

	//endregion

	//region Fees

	@Test
	public void feeIsMaximumOfMinimumFeeAndCurrentFee() {
		// Assert:
		Assert.assertThat(getFee(15L, 50L), IsEqual.equalTo(50L));
		Assert.assertThat(getFee(130L, 50L), IsEqual.equalTo(130L));
	}

	private long getFee(long minimumFee, long fee) {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer);
		transaction.setMinimumFee(minimumFee);
		transaction.setFee(new Amount(fee));
		return transaction.getFee().getNumMicroNem();
	}

	//endregion

	//region Execute and Undo

	@Test
	public void executeCommitDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getNumExecuteCommitCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void executeNonCommitDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.execute(Mockito.mock(TransferObserver.class));

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getNumExecuteCommitCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void undoCommitDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.undo();

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getNumUndoCommitCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void undoNonCommitDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.execute(Mockito.mock(TransferObserver.class));

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getNumUndoCommitCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void undoCommitChangesAccountBalances() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		account2.incrementBalance(Amount.fromNem(25));
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		// Act:
		transaction.undo();

		// Assert:
		Assert.assertThat(account1.getBalance(), IsEqual.equalTo(Amount.fromNem(14)));
		Assert.assertThat(account2.getBalance(), IsEqual.equalTo(Amount.fromNem(13)));
	}

	@Test
	public void executeCommitChangesAccountBalances() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		account1.incrementBalance(Amount.fromNem(25));
		final Account account2 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setTransferAction(to -> {
			to.notifyTransfer(account1, account2, Amount.fromNem(12));
			to.notifyCredit(account1, Amount.fromNem(9));
			to.notifyDebit(account1, Amount.fromNem(11));
		});

		// Act:
		transaction.execute();

		// Assert:
		Assert.assertThat(account1.getBalance(), IsEqual.equalTo(Amount.fromNem(11)));
		Assert.assertThat(account2.getBalance(), IsEqual.equalTo(Amount.fromNem(12)));
	}

	//endregion

	private MockTransaction createRoundTrippedTransaction(
			Transaction originalTransaction,
			final Account deserializedSigner) {
		// Act:
		Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, deserializedSigner);
		return new MockTransaction(deserializer);
	}
}