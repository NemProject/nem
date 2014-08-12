package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.text.ParseException;

public class TransactionDebugInfoTest {

	//region constructor

	@Test
	public void constructorParametersAreApplied() {
		// Arrange:
		final TimeInstant timestamp = new TimeInstant(1000);
		final TimeInstant deadline = new TimeInstant(1720);
		final Address sender = Utils.generateRandomAddress();
		final Address recipient = Utils.generateRandomAddress();
		final Amount amount = Amount.fromNem(100);
		final Amount fee = Amount.fromNem(10);
		final String message = "Test message";

		// Act:
		final TransactionDebugInfo transactionDebugInfo = new TransactionDebugInfo(timestamp, deadline, sender, recipient, amount, fee, message);

		// Assert:
		Assert.assertThat(transactionDebugInfo.getTimestamp(), IsEqual.equalTo(timestamp));
		Assert.assertThat(transactionDebugInfo.getDeadline(), IsEqual.equalTo(deadline));
		Assert.assertThat(transactionDebugInfo.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transactionDebugInfo.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transactionDebugInfo.getAmount(), IsEqual.equalTo(amount));
		Assert.assertThat(transactionDebugInfo.getFee(), IsEqual.equalTo(fee));
		Assert.assertThat(transactionDebugInfo.getMessage(), IsEqual.equalTo(message));
	}

	//endregion

	//region serialization

	@Test
	public void canRoundtripTransactionDebugInfo() throws ParseException {
		// Arrange:
		final TimeInstant timestamp = new TimeInstant(1000);
		final TimeInstant deadline = new TimeInstant(1720);
		final Address sender = Utils.generateRandomAddress();
		final Address recipient = Utils.generateRandomAddress();
		final Amount amount = Amount.fromNem(100);
		final Amount fee = Amount.fromNem(10);
		final String message = "Test message";
		final TransactionDebugInfo originalTransactionDebugInfo = new TransactionDebugInfo(timestamp, deadline, sender, recipient, amount, fee, message);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransactionDebugInfo, null);
		final TransactionDebugInfo transactionDebugInfo = new TransactionDebugInfo(deserializer);

		// Assert:
		Assert.assertThat(transactionDebugInfo.getTimestamp(), IsEqual.equalTo(timestamp));
		Assert.assertThat(transactionDebugInfo.getDeadline(), IsEqual.equalTo(deadline));
		Assert.assertThat(transactionDebugInfo.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transactionDebugInfo.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transactionDebugInfo.getAmount(), IsEqual.equalTo(amount));
		Assert.assertThat(transactionDebugInfo.getFee(), IsEqual.equalTo(fee));
		Assert.assertThat(transactionDebugInfo.getMessage(), IsEqual.equalTo(message));
	}

	//endregion
}
