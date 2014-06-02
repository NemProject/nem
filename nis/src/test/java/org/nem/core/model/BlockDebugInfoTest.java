package org.nem.core.model;

import java.math.BigInteger;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class BlockDebugInfoTest {

	//region constructor

	@Test
	public void construtorParametersAreApplied() {
		// Arrange:
		BlockHeight height = new BlockHeight(10);
		Address address = Utils.generateRandomAddress();
		TimeInstant timestamp = new TimeInstant(1000);
		BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		BigInteger hit = BigInteger.valueOf(1234);
		
		// Act:
		BlockDebugInfo blockDebugInfo = new BlockDebugInfo(height, address, timestamp, difficulty, hit);
		
		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight().getRaw(), IsEqual.equalTo(10L));
		Assert.assertThat(blockDebugInfo.getForagerAddress().getEncoded(), IsEqual.equalTo(address.getEncoded()));
		Assert.assertThat(blockDebugInfo.getTimeInstant().getRawTime(), IsEqual.equalTo(1000));
		Assert.assertThat(blockDebugInfo.getDifficulty().getRaw(), IsEqual.equalTo(123_000_000_000_000L));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(new BigInteger("1234")));
	}
	
	//end region

	//region serialization

	@Test
	public void canRoundtripBlockHeight() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		BlockHeight height = new BlockHeight(10);
		Address address = Utils.generateRandomAddress();
		TimeInstant timestamp = new TimeInstant(1000);
		BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		BigInteger hit = BigInteger.valueOf(1234);
		BlockDebugInfo blockDebugInfo = new BlockDebugInfo(height, address, timestamp, difficulty, hit);

		// Act:
		blockDebugInfo.serialize(serializer);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final BlockDebugInfo deserializedBlockDebugInfo = new BlockDebugInfo(deserializer);

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight().getRaw(), IsEqual.equalTo(deserializedBlockDebugInfo.getHeight().getRaw()));
		Assert.assertThat(blockDebugInfo.getForagerAddress().getEncoded(), IsEqual.equalTo(deserializedBlockDebugInfo.getForagerAddress().getEncoded()));
		Assert.assertThat(blockDebugInfo.getTimeInstant().getRawTime(), IsEqual.equalTo(deserializedBlockDebugInfo.getTimeInstant().getRawTime()));
		Assert.assertThat(blockDebugInfo.getDifficulty().getRaw(), IsEqual.equalTo(deserializedBlockDebugInfo.getDifficulty().getRaw()));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(deserializedBlockDebugInfo.getHit()));
	}
	
	//end region
}
