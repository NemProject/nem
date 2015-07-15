package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class NamespaceEntryTest {

	@Test
	public void canCreateEntry() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE);
		final SmartTiles tiles = new SmartTiles();

		// Act:
		final NamespaceEntry entry = new NamespaceEntry(namespace, tiles);

		// Assert:
		Assert.assertThat(entry.getNamespace(), IsEqual.equalTo(namespace));
		Assert.assertThat(entry.getSmartTiles(), IsEqual.equalTo(tiles));
	}

	@Test
	public void canCreateEntryCopy() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE);
		final SmartTiles tiles = new SmartTiles();
		tiles.increaseSupply(new SmartTile(Utils.createMosaicId(1), new Quantity(12)));
		final NamespaceEntry entry = new NamespaceEntry(namespace, tiles);

		// Act:
		final NamespaceEntry copy = entry.copy();

		// Assert:
		Assert.assertThat(copy.getNamespace(), IsEqual.equalTo(namespace));
		Assert.assertThat(copy.getSmartTiles(), IsNot.not(IsEqual.equalTo(tiles)));
		Assert.assertThat(copy.getSmartTiles().size(), IsEqual.equalTo(1));
		Assert.assertThat(copy.getSmartTiles().getCurrentSupply(Utils.createMosaicId(1)), IsEqual.equalTo(new Quantity(12)));
	}
}