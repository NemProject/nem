package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class MultisigTransactionTest {

	//region constructor / roundtrip

	@Test
	public void canCreateTransaction() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigTransaction transaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(innerTransactionHash));
		Assert.assertThat(transaction.getOtherTransaction(), IsEqual.equalTo(innerTransaction));
	}

	@Test
	public void canRoundtripTransaction() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigTransaction originalTransaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);

		// Act:
		final MultisigTransaction transaction = createRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(innerTransactionHash));
	}

	private static TransferTransaction createDefaultTransferTransaction() {
		return new TransferTransaction(
				new TimeInstant(111),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(789),
				null);
	}

	private static MultisigTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	//endregion

	//region getFee

	@Test
	public void minimumFeeDelegatesToInnerTransaction() {
		// Arrange:
		final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		innerTransaction.setMinimumFee(444);
		final Transaction transaction = createDefaultTransaction(innerTransaction);

		// Act:
		final Amount fee = transaction.getFee();

		// Assert:
		// (because getMinimumFee is protected we can't use mockito to directly test delegation)
		Assert.assertThat(fee, IsEqual.equalTo(Amount.fromNem(100).add(Amount.fromMicroNem(444))));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		innerTransaction.setMinimumFee(222);
		final Transaction transaction = createDefaultTransaction(innerTransaction);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert: the inner transaction notifications were bubbled
		Assert.assertThat(innerTransaction.getNumTransferCalls(), IsEqual.equalTo(1));

		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(0), innerTransaction.getSigner(), Amount.fromMicroNem(222));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		innerTransaction.setMinimumFee(222);
		final Transaction transaction = createDefaultTransaction(innerTransaction);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert: the inner transaction notifications were bubbled
		Assert.assertThat(innerTransaction.getNumTransferCalls(), IsEqual.equalTo(1));

		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), innerTransaction.getSigner(), Amount.fromMicroNem(222));
	}

	//endregion

	//region addSignature / getSigners

	@Test
	public void cannotAddSignatureForDifferentTransaction() {
		// Arrange:
		final MultisigSignatureTransaction sigTransaction = createSignatureTransaction(Utils.generateRandomHash());
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		ExceptionAssert.assertThrows(
				v -> msTransaction.addSignature(sigTransaction),
				IllegalArgumentException.class);
	}

	@Test
	public void getSignersIncludesMultisigTransactionSigner() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		final List<Account> signers = msTransaction.getSigners();

		// Assert:
		Assert.assertThat(signers, IsEquivalent.equivalentTo(Arrays.asList(msTransaction.getSigner())));
	}

	@Test
	public void canAddSignatureForMatchingTransaction() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigSignatureTransaction sigTransaction = createSignatureTransaction(innerTransactionHash);
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		msTransaction.addSignature(sigTransaction);
		final List<Account> signers = msTransaction.getSigners();

		// Assert:
		Assert.assertThat(signers, IsEquivalent.equivalentTo(Arrays.asList(msTransaction.getSigner(), sigTransaction.getSigner())));
	}

	//endregion

	private static MultisigSignatureTransaction createSignatureTransaction(final Hash hash) {
		return new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				hash,
				Utils.generateRandomSignature());
	}

	private static MultisigTransaction createDefaultTransaction(final Transaction innerTransaction) {
		return new MultisigTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				innerTransaction);
	}
}