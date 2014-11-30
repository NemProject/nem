package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class TransactionMetaDataTest {
	@Test
	public void canCreateTransactionMetaData() {
		// Arrange:
		final TransactionMetaData metaData = createTransactionMetaData(1234, 321);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
	}

	@Test
	public void canRoundTripTransactionMetaData() {
		// Arrange:
		final TransactionMetaData metaData = createRoundTrippedTransactionMetaData(7546, 456);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(7546)));
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(456L));
	}

	private static TransactionMetaData createTransactionMetaData(final long height, final long id) {
		return new TransactionMetaData(new BlockHeight(height), id);
	}

	private static TransactionMetaData createRoundTrippedTransactionMetaData(final long height, final long id) {
		// Arrange:
		final TransactionMetaData metaData = createTransactionMetaData(height, id);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new TransactionMetaData(deserializer);
	}
}
