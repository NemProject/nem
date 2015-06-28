package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NamespaceMetaDataTest {

	@Test
	public void canCreateNamespaceMetaData() {
		// Arrange:
		final NamespaceMetaData metaData = new NamespaceMetaData(321L);

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
	}

	@Test
	public void canRoundTripNamespaceMetaData() {
		// Arrange:
		final NamespaceMetaData metaData = createRoundTrippedMetaData(456L);

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
	}

	private static NamespaceMetaData createRoundTrippedMetaData(final long id) {
		// Arrange:
		final NamespaceMetaData metaData = new NamespaceMetaData(id);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new NamespaceMetaData(deserializer);
	}
}