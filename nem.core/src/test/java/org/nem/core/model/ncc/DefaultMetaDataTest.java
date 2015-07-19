package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class DefaultMetaDataTest {

	@Test
	public void canCreateDefaultMetaData() {
		// Arrange:
		final DefaultMetaData metaData = new DefaultMetaData(321L);

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
	}

	@Test
	public void canRoundTripDefaultMetaData() {
		// Arrange:
		final DefaultMetaData metaData = createRoundTrippedMetaData(456L);

		// Assert:
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
	}

	private static DefaultMetaData createRoundTrippedMetaData(final long id) {
		// Arrange:
		final DefaultMetaData metaData = new DefaultMetaData(id);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new DefaultMetaData(deserializer);
	}
}