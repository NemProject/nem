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
		final TransactionMetaData metaData = createTransactionMetaData(1234);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
	}

	@Test
	public void canRoundTripTransactionMetaData() {
		// Arrange:
		final TransactionMetaData metaData = createRoundTrippedTransactionMetaData(7546);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(7546)));
	}

	private static TransactionMetaData createTransactionMetaData(long height) {
		return new TransactionMetaData(new BlockHeight(height));
	}

	private static TransactionMetaData createRoundTrippedTransactionMetaData(long height) {
		// Arrange:
		final TransactionMetaData metaData = createTransactionMetaData(height);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new TransactionMetaData(deserializer);
	}
}
