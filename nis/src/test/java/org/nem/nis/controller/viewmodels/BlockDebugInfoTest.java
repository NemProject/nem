package org.nem.nis.controller.viewmodels;

import java.math.BigInteger;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class BlockDebugInfoTest {

	//region constructor

	@Test
	public void constructorParametersAreApplied() {
		// Arrange:
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddress();
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		
		// Act:
		final BlockDebugInfo blockDebugInfo = new BlockDebugInfo(height, address, timestamp, difficulty, hit);
		
		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeInstant(), IsEqual.equalTo(timestamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
	}
	
	//endregion

	//region serialization

	@Test
	public void canRoundtripBlockDebugInfo() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddress();
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		final BlockDebugInfo originalBlockDebugInfo = new BlockDebugInfo(height, address, timestamp, difficulty, hit);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalBlockDebugInfo, null);
		final BlockDebugInfo blockDebugInfo = new BlockDebugInfo(deserializer);

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeInstant(), IsEqual.equalTo(timestamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
	}
	
	//endregion
}
