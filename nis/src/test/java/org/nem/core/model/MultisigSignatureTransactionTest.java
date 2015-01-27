package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.Collection;

public class MultisigSignatureTransactionTest {

	//region constructor / roundtrip

	@Test
	public void canCreateTransaction() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				account,
				Utils.generateRandomAccount(),
				hash);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNATURE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void canRoundtripTransaction() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction originalTransaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				account,
				Utils.generateRandomAccount(),
				hash);

		// Act:
		final MultisigSignatureTransaction transaction = createRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNATURE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(hash));
	}

	private static MultisigSignatureTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigSignatureTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	//endregion

	//region getFee

	@Test
	public void minimumFeeIsZero() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();

		// Assert:
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));
	}

	//endregion

	//region getAccounts

	@Test
	public void getAccountsIncludesOnlySigner() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Transaction transaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				signer,
				Utils.generateRandomAccount(),
				Utils.generateRandomHash());

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(signer));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();
		transaction.setFee(Amount.fromNem(12));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getValue(), transaction.getSigner(), Amount.fromNem(12));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();
		transaction.setFee(Amount.fromNem(12));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getValue(), transaction.getSigner(), Amount.fromNem(12));
	}

	//endregion

	private static MultisigSignatureTransaction createDefaultTransaction() {
		return new MultisigSignatureTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomHash());
	}
}