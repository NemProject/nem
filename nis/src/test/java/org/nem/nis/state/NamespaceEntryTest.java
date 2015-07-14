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

		// Act:
		final NamespaceEntry entry = new NamespaceEntry(namespace);

		// Assert:
		Assert.assertThat(entry.getNamespace(), IsEqual.equalTo(namespace));
		Assert.assertThat(entry.getSmartTiles(), IsNull.notNullValue());
	}
}