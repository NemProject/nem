package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class HarvestInfoTest {
	@Test
	public void HarvestInfoCtorSetsProperFields() {
		// Arrange + Act:
		final HarvestInfo result = new HarvestInfo(
				123L,
				new BlockHeight(123),
				new TimeInstant(654),
				Amount.fromMicroNem(45678),
				98765L);

		// Assert:
		Assert.assertThat(result.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(result.getTimeStamp(), IsEqual.equalTo(new TimeInstant(654)));
		Assert.assertThat(result.getTotalFee(), IsEqual.equalTo(Amount.fromMicroNem(45678)));
		Assert.assertThat(result.getDifficulty(), IsEqual.equalTo(98765L));
	}

	@Test
	public void canRoundTripHarvestInfo() {
		// Arrange:
		final HarvestInfo entity = new HarvestInfo(
				123L,
				new BlockHeight(123),
				new TimeInstant(654),
				Amount.fromMicroNem(45678),
				98765L);

		// Assert:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, null);
		final HarvestInfo result = new HarvestInfo(deserializer);

		// Assera:
		Assert.assertThat(result.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(result.getTimeStamp(), IsEqual.equalTo(new TimeInstant(654)));
		Assert.assertThat(result.getTotalFee(), IsEqual.equalTo(Amount.fromMicroNem(45678)));
		Assert.assertThat(result.getDifficulty(), IsEqual.equalTo(98765L));
	}
}
