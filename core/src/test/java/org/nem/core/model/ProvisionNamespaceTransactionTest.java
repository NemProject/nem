package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.mosaic.MosaicConstants;
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
	private static final Account RENTAL_FEE_SINK = Utils.generateRandomAccount();
	private static final Amount RENTAL_FEE = Amount.fromNem(123);

	// region ctor

	@Test
	public void canCreateTransactionWithNonNullParentParameter() {
		// Act:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Assert:
		assertProperties(transaction, RENTAL_FEE_SINK, RENTAL_FEE, new NamespaceIdPart("bar"), new NamespaceId("foo"));
	}

	@Test
	public void canCreateTransactionWithNullParentParameter() {
		// Act:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", null);

		// Assert:
		assertProperties(transaction, RENTAL_FEE_SINK, RENTAL_FEE, new NamespaceIdPart("bar"), null);
	}

	@Test
	public void canCreateTransactionWithDefaultRentalFeeAndNullParent() {
		// Act:
		final ProvisionNamespaceTransaction transaction = createTransactionWithDefaultRentalFee("bar", null);

		// Assert:
		assertProperties(transaction, MosaicConstants.NAMESPACE_OWNER_NEM, Amount.fromNem(50000), new NamespaceIdPart("bar"), null);
	}

	@Test
	public void canCreateTransactionWithDefaultRentalFeeAndNonNullParent() {
		// Act:
		final ProvisionNamespaceTransaction transaction = createTransactionWithDefaultRentalFee("bar", "foo");

		// Assert:
		assertProperties(transaction, MosaicConstants.NAMESPACE_OWNER_NEM, Amount.fromNem(5000), new NamespaceIdPart("bar"),
				new NamespaceId("foo"));
	}

	private static void assertProperties(final ProvisionNamespaceTransaction transaction, final Account expectedRentalFeeSink,
			final Amount expectedRentalFee, final NamespaceIdPart expectedNewPart, final NamespaceId expectedParent) {
		MatcherAssert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.PROVISION_NAMESPACE));
		MatcherAssert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		MatcherAssert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		MatcherAssert.assertThat(transaction.getDebtor(), IsEqual.equalTo(SIGNER));
		MatcherAssert.assertThat(transaction.getRentalFeeSink(), IsEqual.equalTo(expectedRentalFeeSink));
		MatcherAssert.assertThat(transaction.getRentalFee(), IsEqual.equalTo(expectedRentalFee));
		MatcherAssert.assertThat(transaction.getNewPart(), IsEqual.equalTo(expectedNewPart));
		MatcherAssert.assertThat(transaction.getParent(), IsEqual.equalTo(expectedParent));
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
		MatcherAssert.assertThat(result, IsEqual.equalTo(new NamespaceId("foo.bar")));
	}

	@Test
	public void getResultingNamespaceIdReturnsNamespaceIdOfNewPartIfParentIsNull() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", null);

		// Act:
		final NamespaceId result = transaction.getResultingNamespaceId();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(new NamespaceId("bar")));
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsRentalFeeSink() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		MatcherAssert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(RENTAL_FEE_SINK)));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSignerAndRentalFeeSink() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		MatcherAssert.assertThat(accounts, IsEquivalent.equivalentTo(Arrays.asList(SIGNER, RENTAL_FEE_SINK)));
	}

	// endregion

	// region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createTransaction("bar", "foo");
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertAccountNotification(values.get(0), RENTAL_FEE_SINK);
		NotificationUtils.assertBalanceTransferNotification(values.get(1), SIGNER, RENTAL_FEE_SINK, RENTAL_FEE);
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
		transaction.undo(observer, null);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(4)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertProvisionNamespaceNotification(values.get(1), SIGNER, new NamespaceId("foo.bar"));
		NotificationUtils.assertBalanceTransferNotification(values.get(2), RENTAL_FEE_SINK, SIGNER, RENTAL_FEE);
		NotificationUtils.assertAccountNotification(values.get(3), RENTAL_FEE_SINK);
	}

	// endregion

	// region round trip

	@Test
	public void canRoundTripTransaction() {
		// Assert:
		assertCanRoundTripTransaction("bar", "foo");
	}

	@Test
	public void canRoundTripTransactionWithNullParent() {
		// Assert:
		assertCanRoundTripTransaction("bar", null);
	}

	private static void assertCanRoundTripTransaction(final String newPart, final String parent) {
		// Act:
		final ProvisionNamespaceTransaction transaction = createRoundTrippedTransaction(createTransaction(newPart, parent));

		// Assert:
		MatcherAssert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.PROVISION_NAMESPACE));
		MatcherAssert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		MatcherAssert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		MatcherAssert.assertThat(transaction.getRentalFeeSink(), IsEqual.equalTo(RENTAL_FEE_SINK));
		MatcherAssert.assertThat(transaction.getRentalFee(), IsEqual.equalTo(RENTAL_FEE));
		MatcherAssert.assertThat(transaction.getNewPart(), IsEqual.equalTo(new NamespaceIdPart(newPart)));
		MatcherAssert.assertThat(transaction.getParent(), null == parent ? IsNull.nullValue() : IsEqual.equalTo(new NamespaceId(parent)));
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("rentalFeeSink");
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

	private static ProvisionNamespaceTransaction createTransactionWithDefaultRentalFee(final String newPart, final String parent) {
		return new ProvisionNamespaceTransaction(TIME_INSTANT, SIGNER, new NamespaceIdPart(newPart),
				null == parent ? null : new NamespaceId(parent));
	}

	private static ProvisionNamespaceTransaction createTransaction(final String newPart, final String parent) {
		return new ProvisionNamespaceTransaction(TIME_INSTANT, SIGNER, RENTAL_FEE_SINK, RENTAL_FEE, new NamespaceIdPart(newPart),
				null == parent ? null : new NamespaceId(parent));
	}
}
