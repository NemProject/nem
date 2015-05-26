package org.nem.core.model;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.Consumer;

@RunWith(Enclosed.class)
public class MultisigAggregateModificationTransactionTest {
	private static final TimeInstant TIME = new TimeInstant(123);
	private static final Amount EXPECTED_ONE_MOD_FEE = Amount.fromNem(2 * (5 + 3));
	private static final Amount EXPECTED_TWO_MOD_FEE = Amount.fromNem(2 * (5 + 2 * 3));

	public static class MultisigAggregateModificationTransactionAddTest extends AbstractMultisigAggregateModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.AddCosignatory;
		}
	}

	public static class MultisigAggregateModificationTransactionDelTest extends AbstractMultisigAggregateModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.DelCosignatory;
		}
	}

	public static class MultisigAggregateModificationTransactionMainTest {
		//region ctor
		@Test
		public void cannotCreateMultisigAggregateModificationWithNullModifications() {
			// Act:
			ExceptionAssert.assertThrows(
					v -> createWithModifications(null),
					IllegalArgumentException.class);
		}

		@Test
		public void cannotCreateMultisigAggregateModificationWithEmptyModifications() {
			// Act:
			ExceptionAssert.assertThrows(
					v -> createWithModifications(new ArrayList<>()),
					IllegalArgumentException.class);
		}

		private static void createWithModifications(final List<MultisigCosignatoryModification> modifications) {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();

			// Act:
			new MultisigAggregateModificationTransaction(MultisigAggregateModificationTransactionTest.TIME, signer, modifications);
		}
		//endregion

		//region deserialization
		@Test
		public void cannotDeserializeMultisigAggregateModificationWithNullModifications() {
			// Act:
			ExceptionAssert.assertThrows(
					v -> createJsonModifications(jsonObject -> jsonObject.remove("modifications")),
					MissingRequiredPropertyException.class);
		}

		@Test
		public void cannotDeserializeMultisigAggregateModificationWithEmptyModifications() {
			// Act:
			ExceptionAssert.assertThrows(
					v -> createJsonModifications(jsonObject -> jsonObject.replace("modifications", new JSONArray())),
					IllegalArgumentException.class);
		}

		private static void createJsonModifications(final Consumer<JSONObject> invalidateJson) {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory = Utils.generateRandomAccount();
			final MultisigCosignatoryModification multisigCosignatoryModification = new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosignatory);
			final List<MultisigCosignatoryModification> modifications = Collections.singletonList(multisigCosignatoryModification);
			final Transaction transaction = new MultisigAggregateModificationTransaction(TimeInstant.ZERO, signer, modifications);
			transaction.sign();

			final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction);
			invalidateJson.accept(jsonObject);

			// Act:
			new MultisigAggregateModificationTransaction(
					VerifiableEntity.DeserializationOptions.NON_VERIFIABLE,
					Utils.createDeserializer(jsonObject));
		}
		//endregion

		@Test
		public void executeRaisesAccountNotificationForAllModifications() {
			// Arrange:
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory1 = Utils.generateRandomAccount();
			final Account cosignatory2 = Utils.generateRandomAccount();
			final List<MultisigCosignatoryModification> modifications = Arrays.asList(
					new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosignatory1),
					new MultisigCosignatoryModification(MultisigModificationType.DelCosignatory, cosignatory2));

			final Transaction transaction = createTransaction(signer, modifications);

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();
			NotificationUtils.assertAccountNotification(notifications.get(0), cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(1), signer, modifications.get(0));
			NotificationUtils.assertAccountNotification(notifications.get(2), cosignatory2);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(3), signer, modifications.get(1));
			NotificationUtils.assertBalanceDebitNotification(notifications.get(4), signer, EXPECTED_TWO_MOD_FEE);
		}
	}

	private static abstract class AbstractMultisigAggregateModificationTransactionTest {
		protected abstract MultisigModificationType getModification();

		//region constructor

		@Test
		public void ctorCanCreateTransactionWithSingleModification() {
			// Assert:
			this.assertCtorCanCreateTransaction(1);
		}

		@Test
		public void ctorCanCreateTransactionWithMultipleModifications() {
			// Assert:
			this.assertCtorCanCreateTransaction(3);
		}

		private void assertCtorCanCreateTransaction(final int numModifications) {
			// Arrange:
			final MultisigModificationType modificationType = this.getModification();
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory = Utils.generateRandomAccount();
			final List<MultisigCosignatoryModification> modifications = createModificationList(modificationType, cosignatory, numModifications);

			// Act:
			final MultisigAggregateModificationTransaction transaction = createTransaction(signer, modifications);

			// Assert:
			Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION));
			Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
			Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getModifications().size(), IsEqual.equalTo(numModifications));
			Assert.assertThat(transaction.getModifications(), IsEquivalent.equivalentTo(modifications));
		}

		@Test
		public void ctorSortsMultisigModificationList() {
			// Arrange:
			final List<MultisigCosignatoryModification> original = new ArrayList<>();
			original.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "C"));
			original.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "D"));
			original.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "A"));
			original.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "F"));
			original.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "B"));
			original.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "E"));

			// Act:
			final MultisigAggregateModificationTransaction transaction = createTransaction(Utils.generateRandomAccount(), original);
			final List<MultisigCosignatoryModification> modifications = new ArrayList<>(transaction.getModifications());

			// Assert:
			for (int i = 0; i < 3; i++) {
				Assert.assertThat(modifications.get(i).getModificationType(), IsEqual.equalTo(MultisigModificationType.AddCosignatory));
				Assert.assertThat(
						modifications.get(i).getCosignatory().getAddress().getEncoded(),
						IsEqual.equalTo(Character.toString((char)(i + (int)'A'))));
				Assert.assertThat(modifications.get(i + 3).getModificationType(), IsEqual.equalTo(MultisigModificationType.DelCosignatory));
				Assert.assertThat(
						modifications.get(i + 3).getCosignatory().getAddress().getEncoded(),
						IsEqual.equalTo(Character.toString((char)(i + (int)'D'))));
			}
		}

		private MultisigCosignatoryModification createMultisigModification(final MultisigModificationType modificationType, final String encodedAddress) {
			return new MultisigCosignatoryModification(modificationType, new Account(Address.fromEncoded(encodedAddress)));
		}

		//endregion

		//region roundtrip

		@Test
		public void canRoundtripTransactionWithSingleModification() {
			// Assert:
			this.assertCanRoundtripTransaction(1);
		}

		@Test
		public void canRoundtripTransactionWithMultipleModifications() {
			// Assert:
			this.assertCanRoundtripTransaction(3);
		}

		private void assertCanRoundtripTransaction(final int numModifications) {
			// Arrange:
			final MultisigModificationType modificationType = this.getModification();
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory = Utils.generateRandomAccount();
			final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);
			final MultisigAggregateModificationTransaction originalTransaction = createTransaction(signer,
					createModificationList(modificationType, cosignatory, numModifications));

			// Act:
			final MultisigAggregateModificationTransaction transaction = this.createRoundTrippedTransaction(originalTransaction, accountLookup);

			// Assert:
			Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION));
			Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME));
			Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(signer));
			Assert.assertThat(transaction.getModifications().size(), IsEqual.equalTo(numModifications));

			final List<MultisigCosignatoryModification> originalModifications = new ArrayList<>(originalTransaction.getModifications());
			final List<MultisigCosignatoryModification> modifications = new ArrayList<>(transaction.getModifications());
			for (int i = 0; i < numModifications; ++i) {
				final MultisigCosignatoryModification originalModification = originalModifications.get(i);
				final MultisigCosignatoryModification modification = modifications.get(i);
				Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(originalModification.getCosignatory()));
				Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(originalModification.getModificationType()));
			}
		}

		private MultisigAggregateModificationTransaction createRoundTrippedTransaction(
				final MultisigAggregateModificationTransaction originalTransaction,
				final AccountLookup accountLookup) {
			// Act:
			final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, accountLookup);
			deserializer.readInt("type");
			return new MultisigAggregateModificationTransaction(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
		}

		//endregion

		//region modifications

		@Test
		public void modificationsIsReadOnly() {
			// Arrange:
			final MultisigModificationType modificationType = this.getModification();
			final Account signer = Utils.generateRandomAccount();
			final Account cosignatory = Utils.generateRandomAccount();
			final MultisigAggregateModificationTransaction transaction = createTransaction(signer,
					createModificationList(modificationType, cosignatory, 1));

			// Act:
			final Collection<MultisigCosignatoryModification> modifications = transaction.getModifications();
			ExceptionAssert.assertThrows(
					v -> modifications.add(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())),
					UnsupportedOperationException.class);
		}

		//endregion

		//region getAccounts

		@Test
		public void getAccountsIncludesSignerAndModifiedAccounts() {
			// Arrange:
			final TestContextForUndoExecuteTests context = new TestContextForUndoExecuteTests(this.getModification());

			// Act:
			final Collection<Account> accounts = context.transactionWithTwoModifications.getAccounts();

			// Assert:
			Assert.assertThat(accounts, IsEquivalent.equivalentTo(context.signer, context.cosignatory1, context.cosignatory2));
		}

		//endregion

		//region execute / undo

		@Test
		public void executeRaisesAppropriateNotificationsForTransactionWithSingleModification() {
			// Arrange:
			final TestContextForUndoExecuteTests context = new TestContextForUndoExecuteTests(this.getModification());

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			context.transactionWithOneModification.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();
			NotificationUtils.assertAccountNotification(notifications.get(0), context.cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(1), context.signer, context.modification1);
			NotificationUtils.assertBalanceDebitNotification(notifications.get(2), context.signer, EXPECTED_ONE_MOD_FEE);
		}

		@Test
		public void undoRaisesAppropriateNotificationsForTransactionWithSingleModification() {
			// Arrange:
			final TestContextForUndoExecuteTests context = new TestContextForUndoExecuteTests(this.getModification());

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			context.transactionWithOneModification.undo(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(3)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();
			NotificationUtils.assertAccountNotification(notifications.get(2), context.cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(1), context.signer, context.modification1);
			NotificationUtils.assertBalanceCreditNotification(notifications.get(0), context.signer, EXPECTED_ONE_MOD_FEE);
		}

		@Test
		public void executeRaisesAppropriateNotificationsForTransactionWithMultipleModification() {
			// Arrange:
			final TestContextForUndoExecuteTests context = new TestContextForUndoExecuteTests(this.getModification());

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			context.transactionWithTwoModifications.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();
			NotificationUtils.assertAccountNotification(notifications.get(0), context.cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(1), context.signer, context.modification1);
			NotificationUtils.assertAccountNotification(notifications.get(2), context.cosignatory2);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(3), context.signer, context.modification2);
			NotificationUtils.assertBalanceDebitNotification(notifications.get(4), context.signer, EXPECTED_TWO_MOD_FEE);
		}

		@Test
		public void undoRaisesAppropriateNotificationsForTransactionWithMultipleModification() {
			// Arrange:
			final TestContextForUndoExecuteTests context = new TestContextForUndoExecuteTests(this.getModification());

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			context.transactionWithTwoModifications.undo(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
			final List<Notification> notifications = notificationCaptor.getAllValues();
			NotificationUtils.assertAccountNotification(notifications.get(4), context.cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(3), context.signer, context.modification1);
			NotificationUtils.assertAccountNotification(notifications.get(2), context.cosignatory2);
			NotificationUtils.assertCosignatoryModificationNotification(notifications.get(1), context.signer, context.modification2);
			NotificationUtils.assertBalanceCreditNotification(notifications.get(0), context.signer, EXPECTED_TWO_MOD_FEE);
		}

		private static class TestContextForUndoExecuteTests {
			private final Account signer = Utils.generateRandomAccount();
			private final Account cosignatory1;
			private final Account cosignatory2;
			private final MultisigCosignatoryModification modification1;
			private final MultisigCosignatoryModification modification2;
			private final Transaction transactionWithOneModification;
			private final Transaction transactionWithTwoModifications;

			public TestContextForUndoExecuteTests(final MultisigModificationType modificationType) {
				// need to order cosignatories because tests rely on special list indices.
				final Account account1 = Utils.generateRandomAccount();
				final Account account2 = Utils.generateRandomAccount();
				final int compareResult = account1.getAddress().compareTo(account2.getAddress());
				this.cosignatory1 = compareResult < 0 ? account1 : account2;
				this.cosignatory2 = compareResult < 0 ? account2 : account1;
				this.modification1 = new MultisigCosignatoryModification(modificationType, this.cosignatory1);
				this.modification2 = new MultisigCosignatoryModification(modificationType, this.cosignatory2);

				this.transactionWithOneModification = createTransaction(this.signer, Collections.singletonList(this.modification1));
				this.transactionWithTwoModifications = createTransaction(this.signer, Arrays.asList(this.modification1, this.modification2));
			}
		}

		// endregion
	}

	private static List<MultisigCosignatoryModification> createModificationList(
			final MultisigModificationType modificationType,
			final Account cosignatory,
			final int numModifications) {
		final List<MultisigCosignatoryModification> modifications = new ArrayList<>();
		for (int i = 0; i < numModifications; ++i) {
			final MultisigCosignatoryModification multisigCosignatoryModification = new MultisigCosignatoryModification(modificationType, cosignatory);
			modifications.add(multisigCosignatoryModification);
		}

		return modifications;
	}

	public static MultisigAggregateModificationTransaction createTransaction(final Account sender, final List<MultisigCosignatoryModification> modifications) {
		return new MultisigAggregateModificationTransaction(TIME, sender, modifications);
	}
}
