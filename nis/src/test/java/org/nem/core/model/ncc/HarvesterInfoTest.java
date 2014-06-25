package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.Hash;
import org.nem.core.model.ncc.HarvesterInfo;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class HarvesterInfoTest {
	@Test
	public void harvesterInfoCtorSetsProperFields() {
		// Arrange + Act:
		final HarvesterInfo result = new HarvesterInfo(Hash.fromHexString("aabbcc"), 
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
	public void canRoundTripHarvesterInfo() {
		// Arrange:
		final HarvesterInfo entity = new HarvesterInfo(Hash.fromHexString("aabbcc"), 
													   new BlockHeight(123), 
													   new TimeInstant(654), 
													   Amount.fromMicroNem(45678));

		// Assert:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, null);
		final HarvesterInfo result = new HarvesterInfo(deserializer);

		// Assera:
		Assert.assertThat(result.getHash(), IsEqual.equalTo(Hash.fromHexString("aabbcc")));
		Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(result.getTimestamp(), IsEqual.equalTo(new TimeInstant(654)));
		Assert.assertThat(result.getTotalFee(), IsEqual.equalTo(Amount.fromMicroNem(45678)));
	}
}
