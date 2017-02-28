package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class MosaicDefinitionCreationTransactionTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);
	private static final Account CREATION_FEE_SINK = Utils.generateRandomAccount();
	private static final Amount CREATION_FEE = Amount.fromNem(123);

	// region ctor

	@Test
	public void canCreateTransaction() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);

		// Act:
		final MosaicDefinitionCreationTransaction transaction = createTransaction(mosaicDefinition);

		// Assert:
		assertProperties(transaction, mosaicDefinition, CREATION_FEE_SINK, CREATION_FEE);
	}

	@Test
	public void canCreateTransactionWithDefaultCreationFee() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);

		// Act:
		final MosaicDefinitionCreationTransaction transaction = new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition);

		// Assert:
		assertProperties(transaction, mosaicDefinition, MosaicConstants.MOSAIC_CREATION_FEE_SINK, Amount.fromNem(50000));
	}

	private static void assertProperties(
			final MosaicDefinitionCreationTransaction transaction,
			final MosaicDefinition expectedMosaicDefinition,
			final Account expectedCreationFeeSink,
			final Amount expectedCreationFee) {
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MOSAIC_DEFINITION_CREATION));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getMosaicDefinition(), IsEqual.equalTo(expectedMosaicDefinition));
		Assert.assertThat(transaction.getCreationFeeSink(), IsEqual.equalTo(expectedCreationFeeSink));
		Assert.assertThat(transaction.getCreationFee(), IsEqual.equalTo(expectedCreationFee));
	}

	@Test
	public void cannotCreateTransactionWithNullParameter() {
		// Assert:
		assertCannotCreateTransaction(null, CREATION_FEE_SINK, CREATION_FEE);
		assertCannotCreateTransaction(Utils.createMosaicDefinition(SIGNER), null, CREATION_FEE);
		assertCannotCreateTransaction(Utils.createMosaicDefinition(SIGNER), CREATION_FEE_SINK, null);
	}

	@Test
	public void cannotCreateTransactionWithDifferentTransactionSignerAndMosaicDefinitionCreator() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());

		// Assert:
		ExceptionAssert.assertThrows(v -> createTransaction(context.mosaicDefinition), IllegalArgumentException.class);
	}

	private static void assertCannotCreateTransaction(final MosaicDefinition mosaicDefinition, final Account creationFeeSink, final Amount fee) {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition, creationFeeSink, fee),
				IllegalArgumentException.class);
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsCreationFeeSinkWhenLevyIsNull() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(CREATION_FEE_SINK)));
	}

	@Test
	public void getOtherAccountsReturnsCreationFeeSinkWhenLevyFeeRecipientIsEqualToSigner() {
		// Arrange: fee recipient is SIGNER
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithLevyFeeRecipient(SIGNER);

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(CREATION_FEE_SINK)));
	}

	@Test
	public void getOtherAccountsReturnsCreationFeeSinkAndFeeRecipientWhenLevyFeeRecipientIsNotEqualToSigner() {
		// Arrange: fee recipient is random account
		final Account feeRecipient = Utils.generateRandomAccount();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithLevyFeeRecipient(feeRecipient);

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Arrays.asList(CREATION_FEE_SINK, feeRecipient)));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSignerAndCreationFeeSink() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Arrays.asList(SIGNER, CREATION_FEE_SINK)));
	}

	// endregion

	// region round trip

	@Test
	public void canRoundTripTransaction() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		final MosaicDefinitionCreationTransaction original = createTransaction(mosaicDefinition);

		// Act:
		final MosaicDefinitionCreationTransaction transaction = createRoundTrippedTransaction(original);

		// Assert:
		assertProperties(transaction, mosaicDefinition, CREATION_FEE_SINK, CREATION_FEE);
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("mosaicDefinition");
		assertCannotDeserializeWithMissingProperty("creationFeeSink");
		assertCannotDeserializeWithMissingProperty("creationFee");
	}

	private static void assertCannotDeserializeWithMissingProperty(final String propertyName) {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction.asNonVerifiable());
		jsonObject.remove(propertyName);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));
		deserializer.readInt("type");

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicDefinitionCreationTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer),
				MissingRequiredPropertyException.class);
	}

	private static MosaicDefinitionCreationTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MosaicDefinitionCreationTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	// endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(0), expected);
		NotificationUtils.assertBalanceTransferNotification(values.get(1), SIGNER, CREATION_FEE_SINK, CREATION_FEE);
		NotificationUtils.assertBalanceDebitNotification(values.get(2), SIGNER, Amount.fromNem(100));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertBalanceTransferNotification(values.get(1), CREATION_FEE_SINK, SIGNER, CREATION_FEE);
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(2), expected);
	}

	// endregion

	private static MosaicDefinitionCreationTransaction createTransaction() {
		return createTransaction(Utils.createMosaicDefinition(SIGNER));
	}

	private static MosaicDefinitionCreationTransaction createTransactionWithLevyFeeRecipient(final Account feeRecipient) {
		final MosaicLevy levy = new MosaicLevy(
				MosaicTransferFeeType.Absolute,
				feeRecipient,
				Utils.createMosaicId(2),
				Quantity.fromValue(123));
		return createTransaction(Utils.createMosaicDefinition(SIGNER, levy));
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition) {
		return createTransaction(mosaicDefinition, CREATION_FEE_SINK);
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition, final Account creationFeeSink) {
		return new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition, creationFeeSink, CREATION_FEE);
	}

	private class TestContext {
		private final MosaicDefinition mosaicDefinition;

		private TestContext(final Account creator) {
			this.mosaicDefinition = Utils.createMosaicDefinition(creator);
		}
	}
}
