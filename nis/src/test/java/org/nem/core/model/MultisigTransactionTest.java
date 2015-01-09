package org.nem.core.model;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class MultisigTransactionTest {

	//region constructor

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

	//endregion

	//region roundtrip

	//region roundtrip (verifiable)

	@Test
	public void canRoundtripTransaction() {
		// Assert:
		final MultisigTransaction transaction = assertCanRoundtripNonVerifiableTransaction(
				MultisigTransactionTest::createRoundTrippedTransaction);
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void canRoundtripTransactionWithSignatures() {

		// Assert:
		assertCanRoundtripWithSignatures(Utils::roundtripSerializableEntity);
	}

	// TODO 20150103 J-G: what is the significance of this one vs the one above?
	@Test
	public void canBinaryRoundtripTransactionWithSignatures() {
		// Assert:
		assertCanRoundtripWithSignatures(Utils::roundtripSerializableEntityWithBinarySerializer);
	}

	private static void assertCanRoundtripWithSignatures(final BiFunction<Transaction, AccountLookup, Deserializer> roundTripEntity) {
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
		final Deserializer deserializer = roundTripEntity.apply(originalTransaction, new MockAccountLookup());
		deserializer.readInt("type");
		final MultisigTransaction transaction = new MultisigTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);

		// Assert:
		Assert.assertThat(originalTransaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().get(0), IsEqual.equalTo(originalTransaction.getSigners().get(0)));
	}

	@Test
	public void cannotDeserializeMultisigTransactionWithMismatchedSignatures() {
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

		// Arrange: invalidate the signature other hash
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalTransaction);
		((JSONObject)((JSONArray)jsonObject.get("signatures")).get(0))
				.replace("otherHash", JsonSerializer.serializeToJson(Utils.generateRandomHash()));
		final Deserializer deserializer = Utils.createDeserializer(jsonObject);
		deserializer.readInt("type");

		// Act:
		ExceptionAssert.assertThrows(
				v -> new MultisigTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer),
				IllegalArgumentException.class);
	}

	//endregion

	//region roundtrip (non-verifiable)

	@Test
	public void canRoundtripNonVerifiableTransaction() {
		// Assert:
		final MultisigTransaction transaction = assertCanRoundtripNonVerifiableTransaction(
				MultisigTransactionTest::createNonVerifiableRoundTrippedTransaction);
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}

	@Test
	public void canRoundtripNonVerifiableTransactionWithSignatures() {
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
		final MultisigTransaction transaction = createNonVerifiableRoundTrippedTransaction(originalTransaction);

		// Assert: signatures are non included in the non-verifiable payload
		Assert.assertThat(originalTransaction.getSigners().size(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getSigners().size(), IsEqual.equalTo(0));
	}

	//endregion

	private static MultisigTransaction assertCanRoundtripNonVerifiableTransaction(
			final Function<Transaction, MultisigTransaction> roundTripEntity) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction();
		innerTransaction.sign();
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigTransaction originalTransaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);
		originalTransaction.sign();

		// Act:
		final MultisigTransaction transaction = roundTripEntity.apply(originalTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(innerTransactionHash));

		// - the other transaction excluding is preserved, excluding its signature
		Assert.assertThat(HashUtils.calculateHash(transaction.getOtherTransaction()), IsEqual.equalTo(innerTransactionHash));
		Assert.assertThat(transaction.getOtherTransaction().getSignature(), IsNull.nullValue());
		return transaction;
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
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction, new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	}

	private static MultisigTransaction createNonVerifiableRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MultisigTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
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
		Assert.assertThat(fee, IsEqual.equalTo(Amount.fromNem(100)));
	}

	//endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotificationsWhenNoSignaturesArePresent() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();
		context.setInnerTransactionFee(Amount.fromMicroNem(222));

		// Act:
		final ArgumentCaptor<Notification> notificationCaptor = context.execute(2);
		final List<Notification> notifications = notificationCaptor.getAllValues();

		// Assert:
		NotificationUtils.assertBalanceDebitNotification(notifications.get(0), context.transaction.getSigner(), Amount.fromNem(100));
		NotificationUtils.assertBalanceDebitNotification(notifications.get(1), context.innerTransaction.getSigner(), Amount.fromMicroNem(222));
	}

	@Test
	public void executeRaisesAppropriateNotificationsWhenSignaturesArePresent() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();
		context.setInnerTransactionFee(Amount.fromMicroNem(222));
		context.addSignatureWithFee(Amount.fromNem(123));
		context.addSignatureWithFee(Amount.fromNem(456));

		// Act:
		final ArgumentCaptor<Notification> notificationCaptor = context.execute(4);

		// Assert:
		final List<Account> expectedSigners = context.getOrderedSigners();
		final List<Amount> expectedAmounts = context.getOrderedFees();
		for (int i = 0; i < 4; ++i) {
			NotificationUtils.assertBalanceDebitNotification(
					notificationCaptor.getAllValues().get(i),
					expectedSigners.get(i),
					expectedAmounts.get(i));
		}
	}

	@Test
	public void undoRaisesAppropriateNotificationsWhenNoSignaturesArePresent() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();
		context.setInnerTransactionFee(Amount.fromMicroNem(222));

		// Act:
		final ArgumentCaptor<Notification> notificationCaptor = context.undo(2);
		final List<Notification> notifications = notificationCaptor.getAllValues();

		// Assert:
		NotificationUtils.assertBalanceCreditNotification(notifications.get(0), context.innerTransaction.getSigner(), Amount.fromMicroNem(222));
		NotificationUtils.assertBalanceCreditNotification(notifications.get(1), context.transaction.getSigner(), Amount.fromNem(100));
	}

	@Test
	public void undoRaisesAppropriateNotificationsWhenSignaturesArePresent() {
		// Arrange:
		final UndoExecuteTestContext context = new UndoExecuteTestContext();
		context.setInnerTransactionFee(Amount.fromMicroNem(222));
		context.addSignatureWithFee(Amount.fromNem(123));
		context.addSignatureWithFee(Amount.fromNem(456));

		// Act:
		final ArgumentCaptor<Notification> notificationCaptor = context.undo(4);

		// Assert:
		final List<Account> expectedSigners = context.getOrderedSigners();
		final List<Amount> expectedAmounts = context.getOrderedFees();
		for (int i = 0; i < 4; ++i) {
			NotificationUtils.assertBalanceCreditNotification(
					notificationCaptor.getAllValues().get(i),
					expectedSigners.get(3 - i),
					expectedAmounts.get(3 - i));
		}
	}

	private static class UndoExecuteTestContext {
		private final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		private MultisigTransaction transaction;

		public void setInnerTransactionFee(final Amount amount) {
			this.innerTransaction.setMinimumFee(amount.getNumMicroNem());
			this.transaction = createDefaultTransaction(this.innerTransaction);
		}

		public void addSignatureWithFee(final Amount amount) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
					this.transaction.getTimeStamp(),
					Utils.generateRandomAccount(),
					HashUtils.calculateHash(this.innerTransaction));
			signatureTransaction.setFee(amount);
			this.transaction.addSignature(signatureTransaction);
		}

		public ArgumentCaptor<Notification> execute(final int expectedNotifications) {
			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			this.transaction.execute(observer);

			// Assert: the inner transaction notifications were bubbled
			Assert.assertThat(this.innerTransaction.getNumTransferCalls(), IsEqual.equalTo(1));

			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(expectedNotifications)).notify(notificationCaptor.capture());
			return notificationCaptor;
		}

		public ArgumentCaptor<Notification> undo(final int expectedNotifications) {
			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			this.transaction.undo(observer);

			// Assert: the inner transaction notifications were bubbled
			Assert.assertThat(this.innerTransaction.getNumTransferCalls(), IsEqual.equalTo(1));

			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(expectedNotifications)).notify(notificationCaptor.capture());
			return notificationCaptor;
		}

		public List<Account> getOrderedSigners() {
			final List<Account> signers = new ArrayList<>();
			signers.add(this.transaction.getSigner());
			signers.addAll(this.transaction.getChildTransactions().stream().map(VerifiableEntity::getSigner).collect(Collectors.toList()));
			return signers;
		}

		public List<Amount> getOrderedFees() {
			final List<Amount> fees = new ArrayList<>();
			fees.add(this.transaction.getFee());
			fees.addAll(this.transaction.getChildTransactions().stream().map(Transaction::getFee).collect(Collectors.toList()));
			return fees;
		}
	}

	//endregion

	//region getAccounts / getChildTransactions

	@Test
	public void getAccountsIncludesAllAccountsInChildAndSignatureTransactions() {
		// Arrange:
		final Account innerTransactionSigner = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final MockTransaction innerTransaction = new MockTransaction(innerTransactionSigner);
		final MultisigTransaction transaction = createDefaultTransaction(innerTransaction);

		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
				transaction.getTimeStamp(),
				cosigner,
				HashUtils.calculateHash(innerTransaction));
		transaction.addSignature(signatureTransaction);

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		final Collection<Account> expectedAccounts = Arrays.asList(innerTransactionSigner, cosigner, transaction.getSigner());
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(expectedAccounts));
	}

	@Test
	public void getChildTransactionsIncludesChildAndSignatureTransactions() {
		// Arrange:
		final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction transaction = createDefaultTransaction(innerTransaction);

		final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
				transaction.getTimeStamp(),
				Utils.generateRandomAccount(),
				HashUtils.calculateHash(innerTransaction));
		transaction.addSignature(signatureTransaction);

		// Act:
		final Collection<Transaction> transactions = transaction.getChildTransactions();

		// Assert:
		final Collection<Transaction> expectedTransactions = Arrays.asList(innerTransaction, signatureTransaction);
		Assert.assertThat(transactions, IsEquivalent.equivalentTo(expectedTransactions));
	}

	//endregion

	//region addSignature / getSigners / getCosignerSignatures

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
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();

		// Assert:
		Assert.assertThat(signers.size(), IsEqual.equalTo(0));
		Assert.assertThat(signatures.size(), IsEqual.equalTo(0));
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
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();

		// Assert:
		Assert.assertThat(signers, IsEquivalent.equivalentTo(sigTransaction.getSigner()));
		Assert.assertThat(signatures, IsEquivalent.equivalentTo(sigTransaction));
	}

	@Test
	public void canAddMultipleSignaturesForMatchingTransaction() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigSignatureTransaction sigTransaction1 = createUnverifiableSignatureTransaction(innerTransactionHash);
		final MultisigSignatureTransaction sigTransaction2 = createUnverifiableSignatureTransaction(innerTransactionHash);
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		msTransaction.addSignature(sigTransaction1);
		msTransaction.addSignature(sigTransaction2);
		final List<Account> signers = msTransaction.getSigners();
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();

		// Assert:
		Assert.assertThat(signers, IsEquivalent.equivalentTo(sigTransaction1.getSigner(), sigTransaction2.getSigner()));
		Assert.assertThat(signatures, IsEquivalent.equivalentTo(sigTransaction1, sigTransaction2));
	}

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
		final MultisigSignatureTransaction result = msTransaction.getCosignerSignatures().iterator().next();

		// Assert:
		Assert.assertThat(result.getFee(), IsEqual.equalTo(Amount.fromNem(6)));
	}

	@Test
	public void getSignersIsReadOnly() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		final List<Account> signers = msTransaction.getSigners();
		ExceptionAssert.assertThrows(
				v -> signers.add(Utils.generateRandomAccount()),
				UnsupportedOperationException.class);
	}

	@Test
	public void getCosignerSignaturesIsReadOnly() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MultisigTransaction msTransaction = createDefaultTransaction(innerTransaction);

		// Act:
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();
		ExceptionAssert.assertThrows(
				v -> signatures.add(createUnverifiableSignatureTransaction(Utils.generateRandomHash())),
				UnsupportedOperationException.class);
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

	@Test
	public void cannotVerifyMultisigTransactionWithAtLeastOneNonVerifiableCosignerSignature() {
		// Assert:
		assertCannotVerifyMultisigTransactionWithAtLeastOneBadCosignerSignature(
				MultisigTransactionTest::createUnverifiableSignatureTransaction);
	}

	@Test
	public void cannotVerifyMultisigTransactionWithAtLeastOneMismatchedCosignerSignature() {
		// Assert:
		assertCannotVerifyMultisigTransactionWithAtLeastOneBadCosignerSignature(
				hash -> {
					// quite contrived and probably impossible to happen in the real world
					final Account account = Utils.generateRandomAccount();
					final MultisigSignatureTransaction mismatchedSignatureTransaction = Mockito.mock(MultisigSignatureTransaction.class);
					Mockito.when(mismatchedSignatureTransaction.verify()).thenReturn(true);
					Mockito.when(mismatchedSignatureTransaction.getSigner()).thenReturn(account);
					Mockito.when(mismatchedSignatureTransaction.getOtherTransactionHash())
							.thenReturn(hash, Utils.generateRandomHash());
					return mismatchedSignatureTransaction;
				});
	}

	private static void assertCannotVerifyMultisigTransactionWithAtLeastOneBadCosignerSignature(
			final Function<Hash, MultisigSignatureTransaction> createBadSignature) {
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
		multisigTransaction.addSignature(createBadSignature.apply(HashUtils.calculateHash(transferTransaction)));
		multisigTransaction.addSignature(createSignatureTransaction(signer3, transferTransaction));

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

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