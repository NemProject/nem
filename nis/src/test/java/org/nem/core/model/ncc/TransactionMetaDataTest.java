package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class TransactionMetaDataTest {
	@Test
	public void canCreateTransactionMetaData() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final TransactionMetaData metaData = createTransactionMetaData(1234, 321, hash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void canRoundTripTransactionMetaData() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final TransactionMetaData metaData = createRoundTrippedTransactionMetaData(7546, 456, hash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(7546)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
	}

	private static TransactionMetaData createTransactionMetaData(final long height, final long id, final Hash hash) {
		return new TransactionMetaData(new BlockHeight(height), id, hash);
	}

	private static TransactionMetaData createRoundTrippedTransactionMetaData(final long height, final long id, final Hash hash) {
		// Arrange:
		final TransactionMetaData metaData = createTransactionMetaData(height, id, hash);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new TransactionMetaData(deserializer);
	}
}
