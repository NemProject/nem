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
	}

	@Test
	public void cannotCreateTransactionWithNullMosaicDefinition() {
		// Assert
		ExceptionAssert.assertThrows(v -> createTransaction(null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateTransactionWithDifferentTransactionSignerAndMosaicDefinitionCreator() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());

		// Assert
		ExceptionAssert.assertThrows(v -> createTransaction(context.mosaicDefinition), IllegalArgumentException.class);
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsEmptyList() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.emptyList()));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSigner() {
		// Arrange:
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Collections.singletonList(SIGNER)));
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
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("mosaic");
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
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(0), expected);
		NotificationUtils.assertBalanceDebitNotification(values.get(1), SIGNER, Amount.fromNem(100));
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
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		final MosaicDefinition expected = Utils.createMosaicDefinition(SIGNER);
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertMosaicDefinitionCreationNotification(values.get(1), expected);
	}

	// endregion

	private static MosaicDefinitionCreationTransaction createTransaction() {
		return new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, Utils.createMosaicDefinition(SIGNER));
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition) {
		return new MosaicDefinitionCreationTransaction(TIME_INSTANT, SIGNER, mosaicDefinition);
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
