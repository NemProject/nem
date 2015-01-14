package org.nem.nis.validators;

import net.minidev.json.*;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.Consumer;

@RunWith(Enclosed.class)
public class MultisigAggregateModificationTransactionEnclosingTest {
	public static class MultisigAggregateModificationTransactionAddTest extends MultisigAggregateModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.Add;
		}
	}

	public static class MultisigAggregateModificationTransactionDelTest extends MultisigAggregateModificationTransactionTest {
		@Override
		protected MultisigModificationType getModification() {
			return MultisigModificationType.Del;
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

		private static void createWithModifications(final Collection<MultisigModification> modifications) {
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
			final MultisigModification multisigModification = new MultisigModification(MultisigModificationType.Add, cosignatory);
			final List<MultisigModification> modifications = Arrays.asList(multisigModification);
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
			final List<MultisigModification> modifications = Arrays.asList(
					new MultisigModification(MultisigModificationType.Add, cosignatory1),
					new MultisigModification(MultisigModificationType.Del, cosignatory2));

			final Transaction transaction = MultisigAggregateModificationTransactionTest.createTransaction(signer, modifications);
			transaction.setFee(Amount.fromNem(10));

			// Act:
			final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
			transaction.execute(observer);

			// Assert:
			final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			Mockito.verify(observer, Mockito.times(5)).notify(notificationCaptor.capture());
			NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), cosignatory1);
			NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(1), signer, modifications.get(0));
			NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(2), cosignatory2);
			NotificationUtils.assertCosignatoryModificationNotification(notificationCaptor.getAllValues().get(3), signer, modifications.get(1));
			NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getAllValues().get(4), signer, Amount.fromNem(300));
		}
	}
}
