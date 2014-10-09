package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Amount;

public class PoiOptionsTest {

	@Test
	public void optionsCanBeCreated() {
		// Act:
		final PoiOptions options = new PoiOptions(
				Amount.fromNem(123),
				Amount.fromNem(777),
				true);

		// Assert:
		Assert.assertThat(options.getMinHarvesterBalance(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(options.getMinOutlinkWeight(), IsEqual.equalTo(Amount.fromNem(777)));
		Assert.assertThat(options.isClusteringEnabled(), IsEqual.equalTo(true));
	}
}