package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.IsRoundedEqual;

public class PoiOptionsBuilderTest {

	@Test
	public void canCreateDefaultOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		final PoiOptions options = builder.create();

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
		Assert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.75));
		Assert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.10));
		Assert.assertThat(options.getInverseTeleportationProbability(), IsEqual.equalTo(0.15));
	}

	@Test
	public void canCreateCustomOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setMinHarvesterBalance(Amount.fromNem(123));
		builder.setMinOutlinkWeight(Amount.fromNem(777));
		builder.setIsClusteringEnabled(false);
		builder.setTeleportationProbability(0.65);
		builder.setInterLevelTeleportationProbability(0.32);
		final PoiOptions options = builder.create();

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		Assert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.65));
		Assert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.32));
		Assert.assertThat(options.getInverseTeleportationProbability(), IsRoundedEqual.equalTo(0.03));
	}
}