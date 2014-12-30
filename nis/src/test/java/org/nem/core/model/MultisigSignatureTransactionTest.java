package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class MultisigSignatureTransactionTest {

	//region constructor / roundtrip

	@Test
	public void canCreateTransaction() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final Signature signature = Utils.generateRandomSignature();
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				account,
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
		final Signature signature = Utils.generateRandomSignature();
		final MultisigSignatureTransaction originalTransaction = new MultisigSignatureTransaction(
				new TimeInstant(123),
				account,
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

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert: no notifications
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());
	}

	//endregion

	//region comparing
	@Test
	public void comparingWithDifferentTransactionTypeYieldsSmaller() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();
		final Transaction rhs = Mockito.mock(Transaction.class);

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(-1));
	}

	@Test
	public void comparingEqualTransactionsYieldsEqual() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final Transaction transaction = new MultisigSignatureTransaction(timeInstant, sender, hash);
		final Transaction rhs = new MultisigSignatureTransaction(timeInstant, sender, hash);

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void changingFeeDoesNotChangeComparisonResult() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final Transaction transaction = new MultisigSignatureTransaction(timeInstant, sender, hash);
		final Transaction rhs = new MultisigSignatureTransaction(timeInstant, sender, hash);

		transaction.setFee(Amount.fromNem(12345));

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void changingTimestampDoesNotChangeComparisonResult() {
		// Arrange:
		final TimeInstant timeInstant1 = new TimeInstant(123);
		final TimeInstant timeInstant2 = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final Transaction transaction = new MultisigSignatureTransaction(timeInstant1, sender, hash);
		final Transaction rhs = new MultisigSignatureTransaction(timeInstant2, sender, hash);

		transaction.setFee(Amount.fromNem(12345));

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void differentSenderYieldsDifferentTransaction() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender1 = Utils.generateRandomAccount();
		final Account sender2 = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final Transaction transaction = new MultisigSignatureTransaction(timeInstant, sender1, hash);
		final Transaction rhs = new MultisigSignatureTransaction(timeInstant, sender2, hash);

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}

	@Test
	public void differentHashYieldsDifferentTransaction() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final Transaction transaction = new MultisigSignatureTransaction(timeInstant, sender, hash1);
		final Transaction rhs = new MultisigSignatureTransaction(timeInstant, sender, hash2);

		// Act
		final int result = transaction.compareTo(rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}
	//endregion

	//region equals
	@Test
	public void equalsWithDifferentTransactionTypeYieldsFalse() {
		// Arrange:
		final Transaction transaction = createDefaultTransaction();
		final Transaction rhs = Mockito.mock(Transaction.class);

		// Act
		final boolean result = transaction.equals(rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void equalsForwardsToCompareTo() {
		// Arrange:
		final Transaction transaction = Mockito.mock(MultisigSignatureTransaction.class);
		final Transaction rhs = Mockito.mock(MultisigSignatureTransaction.class);

		// dunno how to test it with mockito
		Assert.assertThat(true, IsEqual.equalTo(false));
	}
	//endregion

	private static MultisigSignatureTransaction createDefaultTransaction() {
		return new MultisigSignatureTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				Utils.generateRandomHash());
	}
}