package org.nem.core.transactions;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
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
		Assert.assertThat(transaction.getSignature(), IsNot.not(IsEqual.equalTo(null)));
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
		Assert.assertThat(transaction.getSignature(), IsEqual.equalTo(null));
	}
}
