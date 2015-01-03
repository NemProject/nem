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
		originalTransaction.sign();

		// Act:
		final MultisigTransaction transaction = createRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(innerTransactionHash));
		// TODO 20150103 - should assert the hash of the deserialized inner transaction
	}

	@Test
	public void canRoundtripTransactionWithSignatures() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final MultisigTransaction originalTransaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);
		final MultisigSignatureTransaction signature = createUnverifiableSignatureTransaction(HashUtils.calculateHash(innerTransaction));
		signature.sign();
		originalTransaction.addSignature(signature);
		originalTransaction.sign();

		// Act:
		final MultisigTransaction transaction = createRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().get(0), IsEqual.equalTo(originalTransaction.getSigners().get(0)));
	}

	// TODO 20150103 J-G: what is the significance of this one vs the one above?
	@Test
	public void canBinaryRoundtripTransactionWithSignatures() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final MultisigTransaction originalTransaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);
		final MultisigSignatureTransaction signature = createUnverifiableSignatureTransaction(HashUtils.calculateHash(innerTransaction));
		signature.sign();
		originalTransaction.addSignature(signature);
		originalTransaction.sign();

		// Act:
		final MultisigTransaction transaction = createBinaryRoundTrippedTransaction(originalTransaction);

		// Assert:
		Assert.assertThat(originalTransaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().get(0), IsEqual.equalTo(originalTransaction.getSigners().get(0)));
	}

	// TODO 20140103 J-G: we need to test VERIFIABLE deserialization too

	private static TransferTransaction createDefaultTransferTransaction() {
		return new TransferTransaction(
				new TimeInstant(111),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(789),
				null);
	}

	private static MultisigTransaction createBinaryRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntityWithBinarySerializer(originalTransaction, new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	private static MultisigTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction, new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	//endregion

	//region hash
	@Test
	public void addingCosignersDoesNotAffectHash() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final MultisigTransaction originalTransaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);

		// Act:
		final Hash expectedHash = HashUtils.calculateHash(originalTransaction);
		final MultisigSignatureTransaction signature = createUnverifiableSignatureTransaction(HashUtils.calculateHash(innerTransaction));
		signature.sign();
		originalTransaction.addSignature(signature);

		// Assert:
		final Hash actualHash = HashUtils.calculateHash(originalTransaction);
		Assert.assertThat(actualHash, IsEqual.equalTo(expectedHash));
	}
	//endregion

	//region getFee

	@Test
	public void minimumFeeDoesNotIncludeInnerTransactionFee() {
		// Arrange:
		final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		innerTransaction.setMinimumFee(444);
		final Transaction transaction = createDefaultTransaction(innerTransaction);

		// Act:
		final Amount fee = transaction.getFee();

		// Assert:
		// (because getMinimumFee is protected we can't use mockito to directly test delegation)
		Assert.assertThat(fee, IsEqual.equalTo(Amount.fromNem(100)));
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
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(0), transaction.getSigner(), Amount.fromNem(100));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), innerTransaction.getSigner(), Amount.fromMicroNem(222));
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
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), innerTransaction.getSigner(), Amount.fromMicroNem(222));
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), transaction.getSigner(), Amount.fromNem(100));
	}

	//endregion

	//region addSignature / getSigners

	@Test
	public void cannotAddSignatureForDifferentTransaction() {
		// Arrange:
		final MultisigSignatureTransaction sigTransaction = createUnverifiableSignatureTransaction(Utils.generateRandomHash());
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		ExceptionAssert.assertThrows(
				v -> msTransaction.addSignature(sigTransaction),
				IllegalArgumentException.class);
	}

	@Test
	public void getSignersDoesNotIncludeMultisigTransactionSigner() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		final List<Account> signers = msTransaction.getSigners();

		// Assert:
		Assert.assertThat(signers.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canAddSignatureForMatchingTransaction() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigSignatureTransaction sigTransaction = createUnverifiableSignatureTransaction(innerTransactionHash);
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		msTransaction.addSignature(sigTransaction);
		final List<Account> signers = msTransaction.getSigners();

		// Assert:
		Assert.assertThat(signers, IsEquivalent.equivalentTo(Arrays.asList(sigTransaction.getSigner())));
	}

	// TODO 20150103 - what is this actually testing?
	@Test
	public void addingSameSignatureDoesNotOverwritePreviousOne() {
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigSignatureTransaction sigTransaction = createUnverifiableSignatureTransaction(innerTransactionHash);
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);
		sigTransaction.setFee(Amount.fromNem(6));

		final MultisigSignatureTransaction nextSignature = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				sigTransaction.getSigner(),
				innerTransactionHash);
		nextSignature.setSignature(Utils.generateRandomSignature());
		nextSignature.setFee(Amount.fromNem(12345));

		msTransaction.addSignature(sigTransaction);

		// Act:
		msTransaction.addSignature(nextSignature);
		final MultisigSignatureTransaction result = msTransaction.getCosignerSignatures().first();

		// Assert:
		Assert.assertThat(result.getFee(), IsEqual.equalTo(Amount.fromNem(6)));
	}
	//endregion

	//region verify

	@Test
	public void canVerifyMultisigTransactionWithThreeCosignerSignatures() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transferTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(100), null);
		transferTransaction.sign();

		final Account signer1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account signer3 = Utils.generateRandomAccount();
		final MultisigTransaction multisigTransaction = new MultisigTransaction(
				new TimeInstant(123),
				sender,
				transferTransaction);

		multisigTransaction.sign();
		multisigTransaction.addSignature(createSignatureTransaction(signer1, transferTransaction));
		multisigTransaction.addSignature(createSignatureTransaction(signer2, transferTransaction));
		multisigTransaction.addSignature(createSignatureTransaction(signer3, transferTransaction));

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(true));
	}

	// TODO 20141213 G-J this currently throws exception, not sure if that's expected
	// TODO 20150103 J-G probably should add another test for the other case checked in verify
	@Test
	public void cannotVerifyMultisigTransactionWithAtLeastOneIncorrectCosignerSignature() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transferTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(100), null);
		transferTransaction.sign();

		final Account signer1 = Utils.generateRandomAccount();
		final Account signer3 = Utils.generateRandomAccount();
		final MultisigTransaction multisigTransaction = new MultisigTransaction(
				new TimeInstant(123),
				sender,
				transferTransaction);

		multisigTransaction.sign();
		multisigTransaction.addSignature(createSignatureTransaction(signer1, transferTransaction));
		multisigTransaction.addSignature(createUnverifiableSignatureTransaction(HashUtils.calculateHash(transferTransaction)));
		multisigTransaction.addSignature(createSignatureTransaction(signer3, transferTransaction));

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	// TODO 20141203 J-G: i'm not sure i follow how the multisig transaction gets signed? is there a separate multisig account?
	@Test
	public void cannotVerifyMultisigTransactionIfMultisigSignatureIsUnverifiable() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transferTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(100), null);
		transferTransaction.sign();

		final Account signer1 = Utils.generateRandomAccount();
		final Account signer2 = Utils.generateRandomAccount();
		final Account signer3 = Utils.generateRandomAccount();
		final MultisigTransaction multisigTransaction = new MultisigTransaction(
				new TimeInstant(123),
				sender,
				transferTransaction);

		multisigTransaction.setSignature(Utils.generateRandomSignature());
		multisigTransaction.addSignature(createSignatureTransaction(signer1, transferTransaction));
		multisigTransaction.addSignature(createSignatureTransaction(signer2, transferTransaction));
		multisigTransaction.addSignature(createSignatureTransaction(signer3, transferTransaction));

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	//endregion

	private static MultisigSignatureTransaction createSignatureTransaction(final Account account, final Transaction transaction) {
		final MultisigSignatureTransaction multisigSignatureTransaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				account,
				HashUtils.calculateHash(transaction.asNonVerifiable()));
		multisigSignatureTransaction.sign();
		return multisigSignatureTransaction;
	}

	private static MultisigSignatureTransaction createUnverifiableSignatureTransaction(final Hash hash) {
		final MultisigSignatureTransaction multisigSignatureTransaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				hash);
		multisigSignatureTransaction.setSignature(Utils.generateRandomSignature());
		return multisigSignatureTransaction;
	}

	private static MultisigTransaction createDefaultTransaction(final Transaction innerTransaction) {
		return new MultisigTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				innerTransaction);
	}
}