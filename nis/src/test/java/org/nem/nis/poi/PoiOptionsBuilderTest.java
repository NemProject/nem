package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Amount;

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
	}

	@Test
	public void canCreateCustomOptions() {
		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setMinHarvesterBalance(Amount.fromNem(123));
		builder.setMinOutlinkWeight(Amount.fromNem(777));
		builder.setIsClusteringEnabled(false);
		final PoiOptions options = builder.create();

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(false));
	}
}