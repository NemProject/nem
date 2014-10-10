package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.IsRoundedEqual;
import org.nem.nis.poi.graph.*;

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
		Assert.assertThat(options.getClusteringStrategy(), IsInstanceOf.instanceOf(FastScanClusteringStrategy.class));
		Assert.assertThat(options.getMuClusteringValue(), IsEqual.equalTo(3));
		Assert.assertThat(options.getEpsilonClusteringValue(), IsEqual.equalTo(0.65));
		Assert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.75));
		Assert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.10));
		Assert.assertThat(options.getInverseTeleportationProbability(), IsEqual.equalTo(0.15));
	}

	@Test
	public void canCreateCustomOptionsWithClusteringStrategy() {
		// Act:
		final GraphClusteringStrategy strategy = Mockito.mock(GraphClusteringStrategy.class);
		final PoiOptionsBuilder builder = createBuilderWithCustomOptions();
		builder.setClusteringStrategy(strategy);
		final PoiOptions options = builder.create();

		// Assert:
		assertCommonCustomOptionValues(options);
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
		Assert.assertThat(options.getClusteringStrategy(), IsSame.sameInstance(strategy));
	}

	@Test
	public void canCreateCustomOptionsWithoutClusteringStrategy() {
		// Act:
		final PoiOptionsBuilder builder = createBuilderWithCustomOptions();
		builder.setClusteringStrategy(null);
		final PoiOptions options = builder.create();

		// Assert:
		assertCommonCustomOptionValues(options);
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(false));
		Assert.assertThat(options.getClusteringStrategy(), IsNull.nullValue());
	}

	private static PoiOptionsBuilder createBuilderWithCustomOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setMinHarvesterBalance(Amount.fromNem(123));
		builder.setMinOutlinkWeight(Amount.fromNem(777));
		builder.setMuClusteringValue(5);
		builder.setEpsilonClusteringValue(0.42);
		builder.setTeleportationProbability(0.65);
		builder.setInterLevelTeleportationProbability(0.32);
		return builder;
	}

	private static void assertCommonCustomOptionValues(final PoiOptions options) {
		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		Assert.assertThat(options.getMuClusteringValue(), IsEqual.equalTo(5));
		Assert.assertThat(options.getEpsilonClusteringValue(), IsEqual.equalTo(0.42));
		Assert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.65));
		Assert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.32));
		Assert.assertThat(options.getInverseTeleportationProbability(), IsRoundedEqual.equalTo(0.03));
	}
}