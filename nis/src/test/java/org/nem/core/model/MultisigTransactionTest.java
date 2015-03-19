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
	private static final Amount EXPECTED_FEE = Amount.fromNem(6);

	//region constructor

	@Test
	public void canCreateTransaction() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction(multisig);
		final Hash innerTransactionHash = HashUtils.calculateHash(innerTransaction.asNonVerifiable());
		final MultisigTransaction transaction = new MultisigTransaction(
				new TimeInstant(123),
				account,
				innerTransaction);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(multisig));
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

	@Test
	public void canBinaryRoundtripTransactionWithSignatures() {
		// Assert:
		assertCanRoundtripWithSignatures(Utils::roundtripSerializableEntityWithBinarySerializer);
	}

	private static void assertCanRoundtripWithSignatures(final BiFunction<Transaction, AccountLookup, Deserializer> roundTripEntity) {
		// Arrange:
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction originalTransaction = context.createMultisig();
		final MultisigSignatureTransaction signature = context.createSignature();
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
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction originalTransaction = context.createMultisig();
		final MultisigSignatureTransaction signature = context.createSignature();
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
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction originalTransaction = context.createMultisig();
		final MultisigSignatureTransaction signature = context.createSignature();
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
		final Account multisig = Utils.generateRandomAccount();
		final Transaction innerTransaction = createDefaultTransferTransaction(multisig);
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
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(account));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(multisig));
		Assert.assertThat(transaction.getOtherTransactionHash(), IsEqual.equalTo(innerTransactionHash));

		// - the other transaction excluding is preserved, excluding its signature
		Assert.assertThat(HashUtils.calculateHash(transaction.getOtherTransaction()), IsEqual.equalTo(innerTransactionHash));
		Assert.assertThat(transaction.getOtherTransaction().getSignature(), IsNull.nullValue());
		return transaction;
	}

	private static TransferTransaction createDefaultTransferTransaction() {
		return createDefaultTransferTransaction(Utils.generateRandomAccount());
	}

	private static TransferTransaction createDefaultTransferTransaction(final Account signer) {
		return new TransferTransaction(
				new TimeInstant(111),
				signer,
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
		final Transaction innerTransaction = createDefaultTransferTransaction();
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction originalTransaction = context.createMultisig();

		// Act:
		final Hash expectedHash = HashUtils.calculateHash(originalTransaction);
		final MultisigSignatureTransaction signature = context.createSignature();
		signature.sign();
		originalTransaction.addSignature(signature);

		// Assert:
		final Hash actualHash = HashUtils.calculateHash(originalTransaction);
		Assert.assertThat(actualHash, IsEqual.equalTo(expectedHash));
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
		NotificationUtils.assertBalanceDebitNotification(notifications.get(0), context.multisig, EXPECTED_FEE);
		NotificationUtils.assertBalanceDebitNotification(notifications.get(1), context.multisig, Amount.fromMicroNem(222));
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
		final List<Amount> expectedAmounts = context.getOrderedFees();
		for (int i = 0; i < 4; ++i) {
			NotificationUtils.assertBalanceDebitNotification(
					notificationCaptor.getAllValues().get(i),
					context.multisig,
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
		NotificationUtils.assertBalanceCreditNotification(notifications.get(0), context.multisig, Amount.fromMicroNem(222));
		NotificationUtils.assertBalanceCreditNotification(notifications.get(1), context.multisig, EXPECTED_FEE);
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
		final List<Amount> expectedAmounts = context.getOrderedFees();
		for (int i = 0; i < 4; ++i) {
			NotificationUtils.assertBalanceCreditNotification(
					notificationCaptor.getAllValues().get(i),
					context.multisig,
					expectedAmounts.get(3 - i));
		}
	}

	private static class UndoExecuteTestContext {
		private final MockTransaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		private final Account multisig = innerTransaction.getSigner();
		private MultisigTransaction transaction;

		public void setInnerTransactionFee(final Amount amount) {
			this.innerTransaction.setFee(amount);
			this.transaction = createDefaultTransaction(this.innerTransaction);
		}

		public void addSignatureWithFee(final Amount amount) {
			final MultisigSignatureTransaction signatureTransaction = new MultisigSignatureTransaction(
					this.transaction.getTimeStamp(),
					Utils.generateRandomAccount(),
					this.innerTransaction.getSigner(),
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
				innerTransaction.getSigner(),
				innerTransaction);
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
				innerTransaction.getSigner(),
				innerTransaction);
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
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();
		final MultisigSignatureTransaction sigTransaction = context.createSignatureWithHash(Utils.generateRandomHash());

		// Act:
		ExceptionAssert.assertThrows(
				v -> msTransaction.addSignature(sigTransaction),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotAddSignatureWithInvalidDebtor() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();
		final MultisigSignatureTransaction sigTransaction = context.createSignatureWithMultisig(Utils.generateRandomAccount());

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
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();
		final MultisigSignatureTransaction sigTransaction = context.createSignature();

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
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigSignatureTransaction sigTransaction1 = context.createSignature();
		final MultisigSignatureTransaction sigTransaction2 = context.createSignature();
		final MultisigTransaction msTransaction = context.createMultisig();

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
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();

		final MultisigSignatureTransaction sigTransaction = context.createSignature();
		sigTransaction.setFee(Amount.fromNem(6));
		msTransaction.addSignature(sigTransaction);

		final MultisigSignatureTransaction nextSignature = context.createSignature(sigTransaction.getSigner());
		nextSignature.setFee(Amount.fromNem(12345));

		// Act:
		msTransaction.addSignature(nextSignature);
		final MultisigSignatureTransaction result = msTransaction.getCosignerSignatures().iterator().next();

		// Assert:
		Assert.assertThat(result.getFee(), IsEqual.equalTo(Amount.fromNem(6)));
	}

	@Test
	public void addingExplicitSignatureForOriginalCosignerHasNoEffect() {
		// Arrange:
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();
		final MultisigSignatureTransaction sigTransaction = context.createSignature(msTransaction.getSigner());

		// Act:
		msTransaction.addSignature(sigTransaction);
		final List<Account> signers = msTransaction.getSigners();
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();

		// Assert:
		Assert.assertThat(signers.size(), IsEqual.equalTo(0));
		Assert.assertThat(signatures.size(), IsEqual.equalTo(0));
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
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigTransaction msTransaction = context.createMultisig();

		// Act:
		final Collection<MultisigSignatureTransaction> signatures = msTransaction.getCosignerSignatures();
		ExceptionAssert.assertThrows(
				v -> signatures.add(context.createSignature()),
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

		final SimpleMultisigContext context = new SimpleMultisigContext(transferTransaction);
		final MultisigTransaction multisigTransaction = context.createMultisig();
		multisigTransaction.sign();

		multisigTransaction.addSignature(context.createSignature());
		multisigTransaction.addSignature(context.createSignature());
		multisigTransaction.addSignature(context.createSignature());

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(true));
	}

	@Test
	public void cannotVerifyMultisigTransactionWithAtLeastOneNonVerifiableCosignerSignature() {
		// Assert:
		assertCannotVerifyMultisigTransactionWithAtLeastOneBadCosignerSignature(
				signature -> {
					signature.setSignature(Utils.generateRandomSignature());
					return signature;
				});
	}

	private static void assertCannotVerifyMultisigTransactionWithAtLeastOneBadCosignerSignature(
			final Function<MultisigSignatureTransaction, MultisigSignatureTransaction> createBadSignature) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transferTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(100), null);
		transferTransaction.sign();

		final SimpleMultisigContext context = new SimpleMultisigContext(transferTransaction);
		final MultisigTransaction multisigTransaction = context.createMultisig();

		multisigTransaction.sign();
		multisigTransaction.addSignature(context.createSignature());
		multisigTransaction.addSignature(createBadSignature.apply(context.createSignature()));
		multisigTransaction.addSignature(context.createSignature());

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

		final SimpleMultisigContext context = new SimpleMultisigContext(transferTransaction);
		final MultisigTransaction multisigTransaction = context.createMultisig();

		multisigTransaction.setSignature(Utils.generateRandomSignature());
		multisigTransaction.addSignature(context.createSignature());
		multisigTransaction.addSignature(context.createSignature());
		multisigTransaction.addSignature(context.createSignature());

		// Act:
		final boolean isVerified = multisigTransaction.verify();

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	//endregion

	private static MultisigTransaction createDefaultTransaction(final Transaction innerTransaction) {
		return new MultisigTransaction(
				new TimeInstant(123),
				Utils.generateRandomAccount(),
				innerTransaction);
	}
}