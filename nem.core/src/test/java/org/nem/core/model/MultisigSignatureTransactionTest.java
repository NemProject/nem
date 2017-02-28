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
		final Account cosigner = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				cosigner,
				multisig,
				hash);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNATURE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(cosigner));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(multisig));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void canCreateTransactionAroundOtherTransaction() {
		// Act:
		final Account cosigner = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction otherTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				cosigner,
				multisig,
				otherTransaction);

		// Assert:
		final Hash hash = HashUtils.calculateHash(otherTransaction);
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNATURE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(cosigner));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(multisig));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void canRoundtripTransaction() {
		// Arrange:
		final Account cosigner = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction originalTransaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				cosigner,
				multisig,
				hash);

		// Act:
		final MultisigSignatureTransaction transaction = createRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_SIGNATURE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(cosigner));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(multisig));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(hash));
	}

	private static MultisigSignatureTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigSignatureTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
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
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = createTransactionWithMultisig(multisig);
		transaction.setFee(Amount.fromNem(12));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer, null);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getValue(), multisig, Amount.fromNem(12));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = createTransactionWithMultisig(multisig);
		transaction.setFee(Amount.fromNem(12));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer, null);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getValue(), multisig, Amount.fromNem(12));
	}

	//endregion

	private static MultisigSignatureTransaction createTransactionWithMultisig(final Account multisig) {
		return new MultisigSignatureTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				multisig,
				Utils.generateRandomHash());
	}
}