package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.Collection;

public class ProvisionNamespaceTransactionTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant((123));

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
		Assert.assertThat(transaction.getNewPart(), IsEqual.equalTo(new NamespaceIdPart(newPart)));
		Assert.assertThat(transaction.getParent(), null == parent ? IsNull.nullValue() : IsEqual.equalTo(new NamespaceId(parent)));
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
	public void getOtherAccountsReturnsEmptyList() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts.isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesOnlySigner() {
		// Arrange:
		final ProvisionNamespaceTransaction transaction = createTransaction("bar", "foo");

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(SIGNER));
	}

	// endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createTransaction("bar", "foo");
		transaction.setFee(Amount.fromNem(25000));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		NotificationUtils.assertProvisionNamespaceNotification(notificationCaptor.getAllValues().get(0), SIGNER, new NamespaceId("foo.bar"));
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(1), SIGNER, Amount.fromNem(25000));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final Transaction transaction = createTransaction("bar", "foo");
		transaction.setFee(Amount.fromNem(25000));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(0), SIGNER, Amount.fromNem(25000));
		NotificationUtils.assertProvisionNamespaceNotification(notificationCaptor.getAllValues().get(1), SIGNER, new NamespaceId("foo.bar"));
	}

	// endregion

	// region round trip

	@Test
	public void canRoundtripTransaction() {
		// Arrange:
		final ProvisionNamespaceTransaction original = createTransaction("bar", "foo");

		// Act:
		final ProvisionNamespaceTransaction transaction = createRoundTrippedTransaction(original);

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.PROVISION_NAMESPACE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getNewPart(), IsEqual.equalTo(new NamespaceIdPart("bar")));
		Assert.assertThat(transaction.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
	}

	private static ProvisionNamespaceTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new ProvisionNamespaceTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	// endregion

	private static ProvisionNamespaceTransaction createTransaction(final String newPart, final String parent) {
		return new ProvisionNamespaceTransaction(
				TIME_INSTANT,
				SIGNER,
				new NamespaceIdPart(newPart),
				null == parent ? null : new NamespaceId(parent));
	}
}
