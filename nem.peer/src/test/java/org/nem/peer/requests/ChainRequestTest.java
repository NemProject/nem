package org.nem.peer.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class ChainRequestTest {
	private static final int DEFAULT_NUMBER_OF_BLOCKS_TO_PULL = 100;
	private static final int DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS = 5000;
	private static final int DEFAULT_BLOCKS_LIMIT = 400;
	private static final int DEFAULT_TRANSACTIONS_LIMIT = 10000;

	// region construction

	@Test
	public void canCreateChainRequestAroundAllParameters() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 222, 333);

		// Assert:
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(333));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void canCreateChainRequestAroundHeight() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111));

		// Assert:
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(DEFAULT_NUMBER_OF_BLOCKS_TO_PULL + 100));
	}

	@Test
	public void canCreateChainRequestAroundHeightAndMinBlocks() {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 222);

		// Assert:
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void chainRequestConstructionCorrectsMinBlocksToMinimumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getMinBlocks(1), IsEqual.equalTo(10));
	}

	@Test
	public void chainRequestConstructionCorrectsMinBlocksToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getMinBlocks(99999999), IsEqual.equalTo(DEFAULT_BLOCKS_LIMIT));
	}

	private static int getMinBlocks(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), initialValue);
		return request.getMinBlocks();
	}

	@Test
	public void chainRequestConstructionCorrectsMaxTransactionsToMinimumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getMaxTransactions(1), IsEqual.equalTo(120));
	}

	@Test
	public void chainRequestConstructionCorrectsMaxTransactionsToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getMaxTransactions(99999999), IsEqual.equalTo(DEFAULT_TRANSACTIONS_LIMIT));
	}

	private static int getMaxTransactions(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), 200, initialValue);
		return request.getMaxTransactions();
	}

	@Test
	public void chainRequestConstructionCorrectsNumBlocksToMinimumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getNumBlocks(1), IsEqual.equalTo(110));
	}

	@Test
	public void chainRequestConstructionCorrectsNumBlocksToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getNumBlocks(99999999), IsEqual.equalTo(DEFAULT_BLOCKS_LIMIT));
	}

	private static int getNumBlocks(final int initialValue) {
		// Act:
		final ChainRequest request = new ChainRequest(new BlockHeight(111), initialValue);
		return request.getNumBlocks();
	}

	// endregion

	// region serialization

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final ChainRequest original = new ChainRequest(new BlockHeight(111), 123, 1234);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);
		final ChainRequest request = new ChainRequest(deserializer);

		// Assert:
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo((new BlockHeight(111))));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(1234));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(223));
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
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(222));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(333));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(322));
	}

	@Test
	public void canDeserializeBlockHeightAsChainRequest() {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));

		// Assert:
		MatcherAssert.assertThat(request.getHeight(), IsEqual.equalTo(new BlockHeight(111)));
		MatcherAssert.assertThat(request.getMinBlocks(), IsEqual.equalTo(DEFAULT_NUMBER_OF_BLOCKS_TO_PULL));
		MatcherAssert.assertThat(request.getMaxTransactions(), IsEqual.equalTo(DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS));
		MatcherAssert.assertThat(request.getNumBlocks(), IsEqual.equalTo(DEFAULT_NUMBER_OF_BLOCKS_TO_PULL + 100));
	}

	@Test
	public void chainRequestDeserializationCorrectsMinBlocksToMinimumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getDeserializedMinBlocks(1), IsEqual.equalTo(10));
	}

	@Test
	public void chainRequestDeserializationCorrectsMinBlocksToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getDeserializedMinBlocks(99999999), IsEqual.equalTo(DEFAULT_BLOCKS_LIMIT));
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
		MatcherAssert.assertThat(getDeserializedMaxTransactions(1), IsEqual.equalTo(120));
	}

	@Test
	public void chainRequestDeserializationCorrectsMaxTransactionsToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getDeserializedMaxTransactions(99999999), IsEqual.equalTo(DEFAULT_TRANSACTIONS_LIMIT));
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
	public void chainRequestDeserializationCorrectsNumBlocksToMinimumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getDeserializedNumBlocks(1), IsEqual.equalTo(110));
	}

	@Test
	public void chainRequestDeserializationCorrectsNumBlocksToMaximumIfNeeded() {
		// Assert:
		MatcherAssert.assertThat(getDeserializedNumBlocks(99999999), IsEqual.equalTo(DEFAULT_BLOCKS_LIMIT));
	}

	private static int getDeserializedNumBlocks(final int initialValue) {
		// Arrange:
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("height", 111);
		jsonObject.put("minBlocks", initialValue);

		// Act:
		final ChainRequest request = new ChainRequest(Utils.createDeserializer(jsonObject));
		return request.getNumBlocks();
	}

	// endregion
}
