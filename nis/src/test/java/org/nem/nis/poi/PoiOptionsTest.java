package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Amount;

public class PoiOptionsTest {

	@Test
	public void canCreateDefaultOptions() {
		// Act:
		final PoiOptions options = new PoiOptions();

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(1000)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateCustomOptions() {
		// Act:
		final PoiOptions options = new PoiOptions(
				Amount.fromNem(123),
				Amount.fromNem(777),
				false);

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(false));
	}
}