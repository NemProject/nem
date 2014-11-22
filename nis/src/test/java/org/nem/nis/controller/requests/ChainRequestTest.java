package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.nis.BlockChainConstants;

public class ChainRequestTest {

	// region construction

	@Test
	public void canCreateChainRequestAroundAllParameters() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 200, 300);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(100)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(200));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(300));
	}

	@Test
	public void canCreateChainRequestAroundHeight() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(100)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
	}

	@Test
	public void canCreateChainRequestAroundHeightAndMinBlocks() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 200);

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(100)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(200));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
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
		final ChainRequest request = new ChainRequest(new BlockHeight(100), initialValue);
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
		Assert.assertThat(getMaxTransactions(99999999), IsEqual.equalTo(BlockChainConstants.TRANSACTIONS_LIMIT));
	}

	private static int getMaxTransactions(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(100), 200, initialValue);
		return request.getMaxTransactions();
	}

	// endregion

	//region serialization

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

	@Test
	public void canDeserializeChainRequest() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 100);
		jsonObject.put("minBlocks", 200);
		jsonObject.put("maxTransactions", 300);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(100)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(200));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(300));
	}

	@Test
	public void canDeserializeBlockHeightAsChainRequest() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 100);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		Assert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(100)));
		Assert.assertThat(request.getMinBlocks(), IsEqual.equalTo(BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		Assert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
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
		jsonObject.put("height", 100);
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
		Assert.assertThat(getDeserializedMaxTransactions(99999999), IsEqual.equalTo(BlockChainConstants.TRANSACTIONS_LIMIT));
	}

	private static int getDeserializedMaxTransactions(final int initialValue) {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 100);
		jsonObject.put("maxTransactions", initialValue);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));
		return request.getMaxTransactions();
	}

	//endregion
}
