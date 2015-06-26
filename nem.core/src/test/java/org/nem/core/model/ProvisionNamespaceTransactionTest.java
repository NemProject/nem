package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class ProvisionNamespaceTransactionTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);
	private static final Account LESSOR = Utils.generateRandomAccount();
	private static final Amount RENTAL_FEE = Amount.fromNem(123);

	// region ctor

	@Test
	public void canCreateTransactionWithNonNullParentParameter() {
		assertCanCreateTransaction("bar", "foo");
	}

	@Test
	public void canCreateTransactionWithNullParentParameter() {
		assertCanCreateTransaction("bar", null);
	}

	private static void assertCanCreateTransaction(final String newPart, final String parent) {
		// Act:
		final ProvisionNamespaceTransaction transaction = createTransaction(newPart, parent);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.PROVISION_NAMESPACE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getLessor(), IsEqual.equalTo(LESSOR));
		Assert.assertThat(transaction.getRentalFee(), IsEqual.equalTo(RENTAL_FEE));
		Assert.assertThat(transaction.getNewPart(), IsEqual.equalTo(new NamespaceIdPart(newPart)));
		Assert.assertThat(transaction.getParent(), null == parent ? IsNull.nullValue() : IsEqual.equalTo(new NamespaceId(parent)));
	}

	@Test
	public void cannotCreateTransactionWhenLessorHasNoPublicKey() {
		ExceptionAssert.assertThrows(v -> new ProvisionNamespaceTransaction(
				TIME_INSTANT,
				SIGNER,
				new Account(Utils.generateRandomAddress()),
				RENTAL_FEE,
				new NamespaceIdPart("ber"),
				new NamespaceId("foo")), IllegalArgumentException.class);
	}

	// endregion

	// region getResultingNamespace

	@Test
	public void getResultingNamespaceIdReturnsConcatenatedNamespaceIdIfParentIsNotNull() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final NamespaceId result = transaction.getResultingNamespaceId();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new NamespaceId("foo.bar")));
	}

	@Test
	public void getResultingNamespaceIdReturnsNamespaceIdOfNewPartIfParentIsNull() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", null);

		// Act:
		final NamespaceId result = transaction.getResultingNamespaceId();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new NamespaceId("bar")));
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsLessor() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(LESSOR)));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSignerAndLessor() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Arrays.asList(SIGNER, LESSOR)));
	}

	// endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createTransaction("bar", "foo");
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertAccountNotification(values.get(0), LESSOR);
		NotificationUtils.assertBalanceTransferNotification(values.get(1), SIGNER, LESSOR, RENTAL_FEE);
		NotificationUtils.assertProvisionNamespaceNotification(values.get(2), SIGNER, new NamespaceId("foo.bar"));
		NotificationUtils.assertBalanceDebitNotification(values.get(3), SIGNER, Amount.fromNem(100));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createTransaction("bar", "foo");
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertProvisionNamespaceNotification(values.get(1), SIGNER, new NamespaceId("foo.bar"));
		NotificationUtils.assertBalanceTransferNotification(values.get(2), LESSOR, SIGNER, RENTAL_FEE);
		NotificationUtils.assertAccountNotification(values.get(3), LESSOR);
	}

	// endregion

	// region round trip

	@Test
	public void canRoundtripTransaction() {
		// Assert:
		assertCanRoundTripTransaction("bar", "foo");
	}

	@Test
	public void canRoundtripTransactionWithNullParent() {
		// Assert:
		assertCanRoundTripTransaction("bar", null);
	}

	private static void assertCanRoundTripTransaction(final String newPart, final String parent) {
		// Act:
		final ProvisionNamespaceTransaction transaction = createRoundTrippedTransaction(createTransaction(newPart, parent));

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.PROVISION_NAMESPACE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getLessor(), IsEqual.equalTo(LESSOR));
		Assert.assertThat(transaction.getRentalFee(), IsEqual.equalTo(RENTAL_FEE));
		Assert.assertThat(transaction.getNewPart(), IsEqual.equalTo(new NamespaceIdPart(newPart)));
		Assert.assertThat(transaction.getParent(), null == parent ? IsNull.nullValue() : IsEqual.equalTo(new NamespaceId(parent)));
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("lessor");
		assertCannotDeserializeWithMissingProperty("rentalFee");
		assertCannotDeserializeWithMissingProperty("newPart");
	}

	private static void assertCannotDeserializeWithMissingProperty(final String propertyName) {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("foo", "bar");
		final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction.asNonVerifiable());
		jsonObject.remove(propertyName);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));
		deserializer.readInt("type");

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new ProvisionNamespaceTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer),
				MissingRequiredPropertyException.class);
	}

	private static ProvisionNamespaceTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new ProvisionNamespaceTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	// endregion

	private static ProvisionNamespaceTransaction createTransaction(
			final String newPart,
			final String parent) {
		return new ProvisionNamespaceTransaction(
				TIME_INSTANT,
				SIGNER,
				LESSOR,
				RENTAL_FEE,
				new NamespaceIdPart(newPart),
				null == parent ? null : new NamespaceId(parent));
	}
}
