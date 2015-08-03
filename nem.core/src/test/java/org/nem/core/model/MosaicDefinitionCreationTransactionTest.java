package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class MosaicDefinitionCreationTransactionTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);
	private static final Account ADMITTER = Utils.generateRandomAccount();
	private static final Amount CREATION_FEE = Amount.fromNem(123);

	// region ctor

	@Test
	public void canCreateTransactionFromValidParameters() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicDefinitionCreationTransaction transaction = createTransaction(context.mosaicDefinition);

		// Assert
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MOSAIC_DEFINITION_CREATION));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getMosaicDefinition(), IsSame.sameInstance(context.mosaicDefinition));
		Assert.assertThat(transaction.getAdmitter(), IsEqual.equalTo(ADMITTER));
		Assert.assertThat(transaction.getCreationFee(), IsEqual.equalTo(CREATION_FEE));
	}

	@Test
	public void cannotCreateTransactionWhenLessorHasNoPublicKey() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		ExceptionAssert.assertThrows(
				v -> createTransaction(context.mosaicDefinition, new Account(Utils.generateRandomAddress())),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateTransactionWithNullParameter() {
		// Assert:
		assertCannotCreateTransaction(null, ADMITTER, CREATION_FEE);
		assertCannotCreateTransaction(Utils.createMosaicDefinition(SIGNER), null, CREATION_FEE);
		assertCannotCreateTransaction(Utils.createMosaicDefinition(SIGNER), ADMITTER, null);
	}

	@Test
	public void cannotCreateTransactionWithDifferentTransactionSignerAndMosaicDefinitionCreator() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());

		// Assert:
		ExceptionAssert.assertThrows(v -> createTransaction(context.mosaicDefinition), IllegalArgumentException.class);
	}

	private void assertCannotCreateTransaction(final MosaicDefinition mosaicDefinition, final Account admitter, final Amount fee) {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition, admitter, fee),
				IllegalArgumentException.class);
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsAdmitter() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(ADMITTER)));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSignerAndAdmitter() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Arrays.asList(SIGNER, ADMITTER)));
	}

	// endregion

	// region round trip

	@Test
	public void canRoundTripTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinitionCreationTransaction original = createTransaction(context.mosaicDefinition);

		// Act:
		final MosaicDefinitionCreationTransaction transaction = createRoundTrippedTransaction(original);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MOSAIC_DEFINITION_CREATION));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getMosaicDefinition(), IsEqual.equalTo(context.mosaicDefinition));
		Assert.assertThat(transaction.getAdmitter(), IsEqual.equalTo(ADMITTER));
		Assert.assertThat(transaction.getCreationFee(), IsEqual.equalTo(CREATION_FEE));
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("mosaicDefinition");
		assertCannotDeserializeWithMissingProperty("admitter");
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
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(0), expected);
		NotificationUtils.assertBalanceTransferNotification(values.get(1), SIGNER, ADMITTER, CREATION_FEE);
		NotificationUtils.assertBalanceDebitNotification(values.get(2), SIGNER, Amount.fromNem(100));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertBalanceTransferNotification(values.get(1), ADMITTER, SIGNER, CREATION_FEE);
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(2), expected);
	}

	// endregion

	private static MosaicDefinitionCreationTransaction createTransaction() {
		return createTransaction(Utils.createMosaicDefinition(SIGNER));
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition) {
		return createTransaction(mosaicDefinition, ADMITTER);
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition, final Account admitter) {
		return new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition, admitter, CREATION_FEE);
	}

	private class TestContext {
		private final MosaicDefinition mosaicDefinition;

		private TestContext() {
			this(SIGNER);
		}

		private TestContext(final Account creator) {
			this.mosaicDefinition = Utils.createMosaicDefinition(creator);
		}
	}
}
