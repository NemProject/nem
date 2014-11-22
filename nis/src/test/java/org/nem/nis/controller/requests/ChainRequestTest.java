package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.nis.BlockChainConstants;

public class ChainRequestTest {

	// region construction

	@Test
	public void chainRequestUsesDefaultValueWhenMaxTransactionsIsMissing() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 100);

		// Assert:
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
	}

	@Test
	public void chainRequestUsesDefaultValuesWhenMinBlocksAndMaxTransactionsIsMissing() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100));

		// Assert:
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
	}

	@Test
	public void chainRequestCorrectsMinBlocksToMinimumIfNeeded() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 1);

		// Assert:
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(10));
	}

	@Test
	public void chainRequestCorrectsMinBlocksToMaximumIfNeeded() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 99999999);

		// Assert:
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	@Test
	public void chainRequestCorrectsMaxTransactionsToMinimumIfNeeded() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 100, 1);

		// Assert:
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(100));
	}

	@Test
	public void chainRequestCorrectsMaxTransactionsToMaximumIfNeeded() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 100, 99999999);

		// Assert:
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.TRANSACTIONS_LIMIT));
	}

	// endregion

	// region serialization

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final ChainRequest original = new ChainRequest(new BlockHeight(100), 123, 1234);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);
		final ChainRequest request = new ChainRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo((new BlockHeight(100))));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(123));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(1234));
	}

	// endregion
}
