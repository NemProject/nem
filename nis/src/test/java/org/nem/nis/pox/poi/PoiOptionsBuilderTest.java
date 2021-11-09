package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsRoundedEqual;
import org.nem.nis.pox.poi.graph.*;

public class PoiOptionsBuilderTest {

	@Test
	public void canCreateDefaultOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		final PoiOptions options = builder.create();

		// Assert:
		assertPostForkOptions(options);
	}

	@Test
	public void canCreateDefaultOptionsAtHeightOne() {
		// Act:
		final PoiOptions options = createOptionsAtHeight(1);

		// Assert:
		assertPostForkOptions(options);
	}

	private static PoiOptions createOptionsAtHeight(final long height) {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder(new BlockHeight(height));
		return builder.create();
	}

	private static void assertPostForkOptions(final PoiOptions options) {
		// Assert:
		MatcherAssert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(10000)));
		MatcherAssert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(1000)));
		MatcherAssert.assertThat(options.getNegativeOutlinkWeight(), IsEqual.equalTo(0.60));
		MatcherAssert.assertThat(options.getOutlierWeight(), IsEqual.equalTo(0.90));
		MatcherAssert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(options.getClusteringStrategy(), IsInstanceOf.instanceOf(FastScanClusteringStrategy.class));
		MatcherAssert.assertThat(options.getMuClusteringValue(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(options.getEpsilonClusteringValue(), IsEqual.equalTo(0.30));
		MatcherAssert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.70));
		MatcherAssert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.10));
		MatcherAssert.assertThat(options.getInverseTeleportationProbability(), IsEqual.equalTo(1.00 - 0.70 - 0.10));
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
		MatcherAssert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(options.getClusteringStrategy(), IsSame.sameInstance(strategy));
	}

	@Test
	public void canCreateCustomOptionsWithoutClusteringStrategy() {
		// Act:
		final PoiOptionsBuilder builder = createBuilderWithCustomOptions();
		builder.setClusteringStrategy(null);
		final PoiOptions options = builder.create();

		// Assert:
		assertCommonCustomOptionValues(options);
		MatcherAssert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(options.getClusteringStrategy(), IsNull.nullValue());
	}

	private static PoiOptionsBuilder createBuilderWithCustomOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setMinHarvesterBalance(Amount.fromNem(123));
		builder.setMinOutlinkWeight(Amount.fromNem(777));
		builder.setNegativeOutlinkWeight(0.76);
		builder.setOutlierWeight(0.82);
		builder.setMuClusteringValue(5);
		builder.setEpsilonClusteringValue(0.42);
		builder.setTeleportationProbability(0.65);
		builder.setInterLevelTeleportationProbability(0.32);
		return builder;
	}

	private static void assertCommonCustomOptionValues(final PoiOptions options) {
		// Assert:
		MatcherAssert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		MatcherAssert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		MatcherAssert.assertThat(options.getNegativeOutlinkWeight(), IsEqual.equalTo(0.76));
		MatcherAssert.assertThat(options.getOutlierWeight(), IsEqual.equalTo(0.82));
		MatcherAssert.assertThat(options.getMuClusteringValue(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(options.getEpsilonClusteringValue(), IsEqual.equalTo(0.42));
		MatcherAssert.assertThat(options.getTeleportationProbability(), IsEqual.equalTo(0.65));
		MatcherAssert.assertThat(options.getInterLevelTeleportationProbability(), IsEqual.equalTo(0.32));
		MatcherAssert.assertThat(options.getInverseTeleportationProbability(), IsRoundedEqual.equalTo(0.03));
	}
}
