package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransactionFactoryTest {

	//region size / isSupported

	@Test
	public void allExpectedTransactionTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionFactory.size(), IsEqual.equalTo(2));
	}

	@Test
	public void isSupportedReturnsTrueForSupportedTypes() {
		// Assert:
		Assert.assertThat(TransactionFactory.isSupported(TransactionTypes.TRANSFER), IsEqual.equalTo(true));
		Assert.assertThat(TransactionFactory.isSupported(TransactionTypes.IMPORTANCE_TRANSFER), IsEqual.equalTo(true));
	}

	@Test
	public void isSupportedReturnsFalseForUnsupportedTypes() {
		// Assert:
		Assert.assertThat(TransactionFactory.isSupported(9999), IsEqual.equalTo(false));
		Assert.assertThat(TransactionFactory.isSupported(TransactionTypes.TRANSFER | 0x1000), IsEqual.equalTo(false));
	}

	//endregion

	//region Unknown Transaction Type

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownTransaction() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		TransactionFactory.VERIFIABLE.deserialize(deserializer);
	}

	//endregion

	//region TransferTransaction

	@Test
	public void canDeserializeVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	private static Transaction createTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(100), null);
	}

	//endregion

	//region ImportanceTransferTransaction

	@Test
	public void canDeserializeVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	private static Transaction createImportanceTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				sender,
				ImportanceTransferTransaction.Mode.Activate,
				recipient);
	}

	//endregion

	private static void assertCanDeserializeVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
	}

	private static void assertCanDeserializeNonVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}
}
