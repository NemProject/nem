package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

public class NamespaceEntryTest {

	@Test
	public void canCreateEntry() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE);
		final Mosaics mosaics = new Mosaics(namespace.getId());
		mosaics.add(Utils.createMosaic(namespace.getId(), 1));

		// Act:
		final NamespaceEntry entry = new NamespaceEntry(namespace, mosaics);

		// Assert:
		Assert.assertThat(entry.getNamespace(), IsEqual.equalTo(namespace));
		Assert.assertThat(entry.getMosaics(), IsEqual.equalTo(mosaics));
	}

	@Test
	public void canCreateEntryCopy() {
		// Arrange:
		final Namespace namespace = new Namespace(new NamespaceId("foo"), Utils.generateRandomAccount(), BlockHeight.ONE);
		final Mosaics mosaics = new Mosaics(namespace.getId());
		mosaics.add(Utils.createMosaic(namespace.getId(), 1));
		final NamespaceEntry entry = new NamespaceEntry(namespace, mosaics);

		// Act:
		final NamespaceEntry copy = entry.copy();

		// Assert:
		Assert.assertThat(copy.getNamespace(), IsEqual.equalTo(namespace));
		Assert.assertThat(copy.getMosaics(), IsNot.not(IsEqual.equalTo(mosaics)));
		Assert.assertThat(copy.getMosaics().size(), IsEqual.equalTo(1));
		Assert.assertThat(copy.getMosaics().contains(Utils.createMosaicId(namespace.getId(), 1)), IsEqual.equalTo(true));
	}
}