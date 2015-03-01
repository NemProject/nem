package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class UnconfirmedTransactionMetaDataTest {
	@Test
	public void canCreateUnconfirmedTransactionMetaDataWithNullParameter() {
		// Arrange:
		final UnconfirmedTransactionMetaData metaData = createUnconfirmedTransactionMetaData(null);

		// Assert:
		Assert.assertThat(metaData.getInnerTransactionHash(), IsNull.nullValue());
	}

	@Test
	public void canCreateUnconfirmedTransactionMetaDataWithHashParameter() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final UnconfirmedTransactionMetaData metaData = createUnconfirmedTransactionMetaData(hash);

		// Assert:
		Assert.assertThat(metaData.getInnerTransactionHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void canRoundTripUnconfirmedTransactionMetaDataWithNullHash() {
		// Arrange:
		final UnconfirmedTransactionMetaData metaData = createRoundTrippedUnconfirmedTransactionMetaData(null);

		// Assert:
		Assert.assertThat(metaData.getInnerTransactionHash(), IsNull.nullValue());
	}

	@Test
	public void canRoundTripUnconfirmedTransactionMetaDataWithNonNullHash() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final UnconfirmedTransactionMetaData metaData = createRoundTrippedUnconfirmedTransactionMetaData(hash);

		// Assert:
		Assert.assertThat(metaData.getInnerTransactionHash(), IsEqual.equalTo(hash));
	}

	private static UnconfirmedTransactionMetaData createUnconfirmedTransactionMetaData(final Hash innerTransactionHash) {
		return new UnconfirmedTransactionMetaData(innerTransactionHash);
	}

	private static UnconfirmedTransactionMetaData createRoundTrippedUnconfirmedTransactionMetaData(final Hash innerTransactionHash) {
		// Arrange:
		final UnconfirmedTransactionMetaData metaData = createUnconfirmedTransactionMetaData(innerTransactionHash);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new UnconfirmedTransactionMetaData(deserializer);
	}
}
