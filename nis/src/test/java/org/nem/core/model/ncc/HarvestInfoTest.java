package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class HarvestInfoTest {
	@Test
	public void HarvestInfoCtorSetsProperFields() {
		// Arrange + Act:
		final HarvestInfo result = new HarvestInfo(
				Hash.fromHexString("aabbcc"),
                new BlockHeight(123),
                new TimeInstant(654),
                Amount.fromMicroNem(45678));

		// Assert:
		Assert.assertThat(result.getHash(), IsEqual.equalTo(Hash.fromHexString("aabbcc")));
		Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(result.getTimestamp(), IsEqual.equalTo(new TimeInstant(654)));
		Assert.assertThat(result.getTotalFee(), IsEqual.equalTo(Amount.fromMicroNem(45678)));
	}

	@Test
	public void canRoundTripHarvestInfo() {
		// Arrange:
		final HarvestInfo entity = new HarvestInfo(
				Hash.fromHexString("aabbcc"),
				new BlockHeight(123),
				new TimeInstant(654),
				Amount.fromMicroNem(45678));

		// Assert:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, null);
		final HarvestInfo result = new HarvestInfo(deserializer);

		// Assera:
		Assert.assertThat(result.getHash(), IsEqual.equalTo(Hash.fromHexString("aabbcc")));
		Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(result.getTimestamp(), IsEqual.equalTo(new TimeInstant(654)));
		Assert.assertThat(result.getTotalFee(), IsEqual.equalTo(Amount.fromMicroNem(45678)));
	}
}
