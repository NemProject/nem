package org.nem.peer.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class ChainRequestTest {

	// region construction

	@Test
	public void canCreateChainRequestAroundAllParameters() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 222, 333);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(333));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void canCreateChainRequestAroundHeight() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL + 100));
	}

	@Test
	public void canCreateChainRequestAroundHeightAndMinBlocks() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 222);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void chainRequestConstructionCorrectsMinBlocksToMinimumIfNeeded() {
		// Assert:
		Assert.assertThat(getMinBlocks(1), IsEqual.equalTo(10));
	}

	@Test
	public void chainRequestConstructionCorrectsMinBlocksToMaximumIfNeeded() {
		// Assert:
		Assert.assertThat(getMinBlocks(99999999), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	private static int getMinBlocks(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), initialValue);
		return request.getMinBlocks();
	}

	@Test
	public void chainRequestConstructionCorrectsMaxTransactionsToMinimumIfNeeded() {
		// Assert:
		Assert.assertThat(getMaxTransactions(1), IsEqual.equalTo(120));
	}

	@Test
	public void chainRequestConstructionCorrectsMaxTransactionsToMaximumIfNeeded() {
		// Assert:
		Assert.assertThat(getMaxTransactions(99999999), IsEqual.equalTo(BlockChainConstants.DEFAULT_TRANSACTIONS_LIMIT));
	}

	private static int getMaxTransactions(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 200, initialValue);
		return request.getMaxTransactions();
	}

	@Test
	public void chainRequestConstructionCorrectsNumBlocksToMaximumIfNeeded() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 99999999);

		// Assert:
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	// endregion

	//region serialization

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final ChainRequest original = new ChainRequest(new BlockHeight(111), 123, 1234);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);
		final ChainRequest request = new ChainRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo((new BlockHeight(111))));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(123));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(1234));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(223));
	}

	@Test
	public void canDeserializeChainRequest() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);
		jsonObject.put("minBlocks", 222);
		jsonObject.put("maxTransactions", 333);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(333));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void canDeserializeBlockHeightAsChainRequest() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL + 100));
	}

	@Test
	public void chainRequestDeserializationCorrectsMinBlocksToMinimumIfNeeded() {
		// Assert:
		Assert.assertThat(getDeserializedMinBlocks(1), IsEqual.equalTo(10));
	}

	@Test
	public void chainRequestDeserializationCorrectsMinBlocksToMaximumIfNeeded() {
		// Assert:
		Assert.assertThat(getDeserializedMinBlocks(99999999), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	private static int getDeserializedMinBlocks(final int initialValue) {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);
		jsonObject.put("minBlocks", initialValue);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));
		return request.getMinBlocks();
	}

	@Test
	public void chainRequestDeserializationCorrectsMaxTransactionsToMinimumIfNeeded() {
		// Assert:
		Assert.assertThat(getDeserializedMaxTransactions(1), IsEqual.equalTo(120));
	}

	@Test
	public void chainRequestDeserializationCorrectsMaxTransactionsToMaximumIfNeeded() {
		// Assert:
		Assert.assertThat(getDeserializedMaxTransactions(99999999), IsEqual.equalTo(BlockChainConstants.DEFAULT_TRANSACTIONS_LIMIT));
	}

	private static int getDeserializedMaxTransactions(final int initialValue) {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);
		jsonObject.put("maxTransactions", initialValue);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));
		return request.getMaxTransactions();
	}

	@Test
	public void chainRequestDeserializationCorrectsNumBlocksToMaximumIfNeeded() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);
		jsonObject.put("minBlocks", 99999999);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(request.getNumBlocks(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	//endregion
}
