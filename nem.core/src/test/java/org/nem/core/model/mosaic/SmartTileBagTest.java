package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.Collections;

public class SmartTileBagTest {

	@Test
	public void canCreateSmartTileBagFromEmptyCollection() {
		// Act:
		final SmartTileBag bag = new SmartTileBag(Collections.emptyList());

		// Assert:
		Assert.assertThat(bag.getSmartTiles().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateSmartTileBagFromNonEmptyCollection() {
		// Act:
		final SmartTile smartTile = new SmartTile(Utils.createMosaicId(5), Quantity.fromValue(123));
		final SmartTileBag bag = new SmartTileBag(Collections.singletonList(smartTile));

		// Assert:
		Assert.assertThat(bag.getSmartTiles().size(), IsEqual.equalTo(1));
		Assert.assertThat(bag.getSmartTiles(), IsEquivalent.equivalentTo(Collections.singletonList(smartTile)));
	}
}
