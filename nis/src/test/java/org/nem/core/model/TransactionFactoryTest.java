package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransactionFactoryTest {

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownTransaction() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		TransactionFactory.VERIFIABLE.deserialize(deserializer);
	}

	@Test
	public void canDeserializeVerifiableTransferTransaction() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Transaction originalTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(100), null);
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());

		// Act:
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
	}

	@Test
	public void canDeserializeNonVerifiableTransferTransaction() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Transaction originalTransaction = new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(100), null);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				originalTransaction.asNonVerifiable(),
				new MockAccountLookup());

		// Act:
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}

	@Test
	public void canDeserializeVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Transaction originalTransaction = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransactionMode.Activate, recipient);
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());

		// Act:
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(ImportanceTransferTransaction.class));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.IMPORTANCE_TRANSFER));
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
		final ImportanceTransferTransaction importanceTransferTransaction = (ImportanceTransferTransaction)transaction;
		Assert.assertThat(importanceTransferTransaction.getDirection(), IsEqual.equalTo(ImportanceTransferTransactionMode.Activate));
		Assert.assertThat(importanceTransferTransaction.getRemote(), IsEqual.equalTo(recipient));
	}

	@Test
	public void canDeserializeNonVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Transaction originalTransaction = new ImportanceTransferTransaction(TimeInstant.ZERO, sender, ImportanceTransferTransactionMode.Activate, recipient);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				originalTransaction.asNonVerifiable(),
				new MockAccountLookup());

		// Act:
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(ImportanceTransferTransaction.class));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.IMPORTANCE_TRANSFER));
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}
}
