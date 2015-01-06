package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;

public class BlockDebugInfoTest {

	//region constructor

	@Test
	public void constructorParametersAreApplied() {
		// Arrange:
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddress();
		final TimeInstant timeStamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		final BigInteger target = BigInteger.valueOf(4321);
		final int interBlockTime = 45;

		// Act:
		final BlockDebugInfo blockDebugInfo = new BlockDebugInfo(height, timeStamp, address, difficulty, hit, target, interBlockTime);

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getHarvesterAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeStamp(), IsEqual.equalTo(timeStamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(interBlockTime));
	}

	//endregion

	//region serialization

	@Test
	public void canRoundtripBlockDebugInfo() {
		// Arrange:
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddress();
		final TimeInstant timeStamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		final BigInteger target = BigInteger.valueOf(4321);
		final int interBlockTime = 45;
		final BlockDebugInfo originalBlockDebugInfo = new BlockDebugInfo(height, timeStamp, address, difficulty, hit, target, interBlockTime);

		final TimeInstant timeStamp2 = new TimeInstant(1000);
		final TimeInstant deadline = new TimeInstant(1720);
		final Address sender = Utils.generateRandomAddress();
		final Address recipient = Utils.generateRandomAddress();
		final Amount amount = Amount.fromNem(100);
		final Amount fee = Amount.fromNem(10);
		final String message = "Test message";
		final TransactionDebugInfo originalTransactionDebugInfo = new TransactionDebugInfo(timeStamp2, deadline, sender, recipient, amount, fee, message);
		originalBlockDebugInfo.addTransactionDebugInfo(originalTransactionDebugInfo);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalBlockDebugInfo, null);
		final BlockDebugInfo blockDebugInfo = new BlockDebugInfo(deserializer);

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getHarvesterAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeStamp(), IsEqual.equalTo(timeStamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(interBlockTime));

		Assert.assertThat(blockDebugInfo.getTransactionDebugInfos().size(), IsEqual.equalTo(1));

		final TransactionDebugInfo transactionDebugInfo = blockDebugInfo.getTransactionDebugInfos().get(0);
		Assert.assertThat(transactionDebugInfo.getTimeStamp(), IsEqual.equalTo(timeStamp2));
		Assert.assertThat(transactionDebugInfo.getDeadline(), IsEqual.equalTo(deadline));
		Assert.assertThat(transactionDebugInfo.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transactionDebugInfo.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(transactionDebugInfo.getAmount(), IsEqual.equalTo(amount));
		Assert.assertThat(transactionDebugInfo.getFee(), IsEqual.equalTo(fee));
	}

	//endregion
}
