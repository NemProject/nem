package org.nem.nis.controller.viewmodels;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

import java.util.function.Consumer;

public class ExplorerBlockViewModelTest {

	@Test
	public void canSerializeViewModelWithoutTransactions() {
		// Assert:
		assertCorrectSerialization(viewModel -> {
		}, 0);
	}

	@Test
	public void canSerializeViewModelWithTransactions() {
		// Assert:
		assertCorrectSerialization(viewModel -> {
			for (int i = 0; i < 3; ++i) {
				viewModel.addTransaction(createTransferViewModel());
			}
		}, 3);
	}

	private static void assertCorrectSerialization(final Consumer<ExplorerBlockViewModel> addTransactions,
			final int numExpectedTransactions) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(101);
		block.sign();
		final Hash blockHash = HashUtils.calculateHash(block);
		final BlockDifficulty difficulty = new BlockDifficulty(new BlockDifficulty(0).getRaw() + 12345);
		block.setDifficulty(difficulty);

		// Act:
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(block, blockHash);
		addTransactions.accept(viewModel);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(getDeserializedBlockHash((JSONObject) jsonObject.get("block")), IsEqual.equalTo(blockHash));
		MatcherAssert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(blockHash.toString()));
		MatcherAssert.assertThat(jsonObject.get("difficulty"), IsEqual.equalTo(difficulty.getRaw()));
		MatcherAssert.assertThat(((JSONArray) jsonObject.get("txes")).size(), IsEqual.equalTo(numExpectedTransactions));
	}

	private static ExplorerTransferViewModel createTransferViewModel() {
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		transaction.sign();
		return new ExplorerTransferViewModel(transaction, HashUtils.calculateHash(transaction));
	}

	private static Hash getDeserializedBlockHash(final JSONObject jsonObject) {
		final Block deserializedBlock = new Block(BlockTypes.REGULAR, VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		return HashUtils.calculateHash(deserializedBlock);
	}
}
