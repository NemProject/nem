package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

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
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(MockTransaction.DEFAULT_FEE));
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
		final MockTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, signerPublicKeyOnly);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(new Amount(130L)));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(MockTransaction.TIMESTAMP));
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(MockTransaction.DEADLINE));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(7));
	}

	//endregion

	//region Deadline

	@Test
	public void transactionDeadlineCanBeSet() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(726));

		// Assert:
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(new TimeInstant(726)));
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
	public void compareResultIsNotInfluencedByType() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(7, 12, new TimeInstant(124), 70);
		final MockTransaction transaction2 = new MockTransaction(11, 12, new TimeInstant(124), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(0));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(0));
	}

	@Test
	public void compareResultIsNotInfluencedByVersion() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(124), 70);
		final MockTransaction transaction2 = new MockTransaction(11, 20, new TimeInstant(124), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(0));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(0));
	}

	@Test
	public void compareResultIsSecondlyInfluencedByTimeStamp() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(150), 70);
		final MockTransaction transaction2 = new MockTransaction(11, 14, new TimeInstant(200), 70);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(-1));
	}

	@Test
	public void compareResultIsFirstlyInfluencedByFee() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction(11, 14, new TimeInstant(150), 70);
		final MockTransaction transaction2 = new MockTransaction(11, 14, new TimeInstant(200), 90);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(-1));
		Assert.assertThat(transaction2.compareTo(transaction1), IsEqual.equalTo(1));
	}

	//endregion

	//region Fees

	@Test
	public void nemesisTransactionFeeIsAlwaysZero() {
		// Arrange:
		final Address nemesisAddress = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress();
		final MockTransaction transaction = new MockTransaction(new Account(nemesisAddress));
		transaction.setFee(Amount.fromNem(200));

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void feeIsMinimumFeeIfUnset() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(MockTransaction.DEFAULT_FEE));
	}

	@Test
	public void feeCanBeSetAboveMinimumFee() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		transaction.setFee(Amount.fromNem(1000));

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1000)));
	}

	@Test
	public void feeCanBeSetBelowMinimumFee() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		transaction.setFee(Amount.fromNem(1));

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.fromNem(1)));
	}

	@Test
	public void feeCanBeResetToMinimum() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setFee(Amount.fromMicroNem(4444));

		// Act:
		transaction.setFee(null);

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(MockTransaction.DEFAULT_FEE));
	}

	//endregion

	//region Accounts

	@Test
	public void accountsAlwaysIncludeSigner() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer));
	}

	@Test
	public void accountsIncludeSignerAndNonSignerAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account other1 = Utils.generateRandomAccount();
		final Account other2 = Utils.generateRandomAccount();
		final Account other3 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.setOtherAccounts(Arrays.asList(other1, other2, other3));

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer, other1, other2, other3));
	}

	@Test
	public void accountsFiltersOutDuplicateAccounts() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account other1 = Utils.generateRandomAccount();
		final Account other2 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.setOtherAccounts(Arrays.asList(other1, other1, other2, signer));

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer, other1, other2));
	}

	//endregion

	//region Child Transactions

	@Test
	public void defaultGetChildTransactionsImplementationReturnsEmptyCollection() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		final Collection<Transaction> childTransactions = transaction.getChildTransactions();

		// Assert:
		Assert.assertThat(childTransactions.isEmpty(), IsEqual.equalTo(true));
	}

	//endregion

	//region Execute and Undo

	@Test
	public void executeDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.execute(Mockito.mock(TransactionObserver.class), null);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void undoDelegatesToDerivedClass() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		// Act:
		transaction.undo(Mockito.mock(TransactionObserver.class), null);

		// Assert:
		Assert.assertThat(transaction.getNumTransferCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void executeExposesAllDerivedClassNotificationsInOrder() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		transaction.setTransferAction(o -> {
			o.notify(new Notification(NotificationType.BalanceTransfer) {
			});
			o.notify(new Notification(NotificationType.ImportanceTransfer) {
			});
			o.notify(new Notification(NotificationType.BalanceDebit) {
			});
		});

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.atLeastOnce()).notify(notificationCaptor.capture());
		Assert.assertThat(
				notificationCaptor.getAllValues().stream().map(Notification::getType).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(NotificationType.BalanceTransfer, NotificationType.ImportanceTransfer, NotificationType.BalanceDebit)));
	}

	@Test
	public void undoExposesAllDerivedClassNotificationsInReverseOrder() {
		// Arrange:
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);

		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(12345);
		transaction.setTransferAction(o -> {
			o.notify(new BalanceTransferNotification(account1, account2, amount) {
			});
			o.notify(new Notification(NotificationType.ImportanceTransfer) {
			});
			o.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account1, amount) {
			});
		});

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.atLeastOnce()).notify(notificationCaptor.capture());
		Assert.assertThat(
				notificationCaptor.getAllValues().stream().map(Notification::getType).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(NotificationType.BalanceCredit, NotificationType.ImportanceTransfer, NotificationType.BalanceTransfer)));
	}

	@Test
	public void undoAppliesNotificationsInReverseOrder() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setTransferAction(to -> {
			// for the account to say in the black, the debit (reverse credit) must occur before the credit (reverse debit)
			NotificationUtils.notifyCredit(to, account1, Amount.fromNem(9));
			NotificationUtils.notifyDebit(to, account1, Amount.fromNem(11));
		});

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.atLeastOnce()).notify(notificationCaptor.capture());
		Assert.assertThat(
				notificationCaptor.getAllValues().stream()
						.filter(n -> NotificationType.Account != n.getType())
						.map(n -> ((BalanceAdjustmentNotification)n).getAmount().getNumNem())
						.collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(11L, 9L)));
	}

	@Test
	public void executeAppliesNotificationsInForwardOrder() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 6);
		transaction.setTransferAction(to -> {
			// for the account to say in the black, the credit must occur before the debit
			NotificationUtils.notifyCredit(to, account1, Amount.fromNem(11));
			NotificationUtils.notifyDebit(to, account1, Amount.fromNem(9));
		});

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.atLeastOnce()).notify(notificationCaptor.capture());
		Assert.assertThat(
				notificationCaptor.getAllValues().stream()
						.filter(n -> NotificationType.Account != n.getType())
						.map(n -> ((BalanceAdjustmentNotification)n).getAmount().getNumNem())
						.collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(11L, 9L)));
	}

	//endregion

	private MockTransaction createRoundTrippedTransaction(
			final Transaction originalTransaction,
			final Account deserializedSigner) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, deserializedSigner);
		return new MockTransaction(deserializer);
	}
}