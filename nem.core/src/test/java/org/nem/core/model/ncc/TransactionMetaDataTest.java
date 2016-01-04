package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
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
		final TransactionMetaData metaData = createMetaData(1234, 321, hash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(metaData.getInnerHash(), IsNull.nullValue());
	}

	@Test
	public void canCreateTransactionMetaDataWithInnerHash() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Hash innerHash = Utils.generateRandomHash();
		final TransactionMetaData metaData = createMetaData(1234, 321, hash, innerHash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(321L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(metaData.getInnerHash(), IsEqual.equalTo(innerHash));
	}

	@Test
	public void canRoundTripTransactionMetaData() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final TransactionMetaData metaData = createRoundTrippedMetaData(7546, 456, hash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(7546)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(metaData.getInnerHash(), IsNull.nullValue());
	}

	@Test
	public void canRoundTripTransactionMetaDataWithInnerHash() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Hash innerHash = Utils.generateRandomHash();
		final TransactionMetaData metaData = createRoundTrippedMetaData(7546, 456, hash, innerHash);

		// Assert:
		Assert.assertThat(metaData.getHeight(), IsEqual.equalTo(new BlockHeight(7546)));
		Assert.assertThat(metaData.getId(), IsEqual.equalTo(456L));
		Assert.assertThat(metaData.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(metaData.getInnerHash(), IsEqual.equalTo(innerHash));
	}

	private static TransactionMetaData createMetaData(final long height, final long id, final Hash hash) {
		return new TransactionMetaData(new BlockHeight(height), id, hash);
	}

	private static TransactionMetaData createMetaData(final long height, final long id, final Hash hash, final Hash innerHash) {
		return new TransactionMetaData(new BlockHeight(height), id, hash, innerHash);
	}

	private static TransactionMetaData createRoundTrippedMetaData(final long height, final long id, final Hash hash) {
		// Arrange:
		final TransactionMetaData metaData = createMetaData(height, id, hash);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new TransactionMetaData(deserializer);
	}

	private static TransactionMetaData createRoundTrippedMetaData(final long height, final long id, final Hash hash, final Hash innerHash) {
		// Arrange:
		final TransactionMetaData metaData = createMetaData(height, id, hash, innerHash);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new TransactionMetaData(deserializer);
	}
}
