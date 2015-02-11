package org.nem.nis.controller.viewmodels;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.nis.test.*;

import java.util.function.Consumer;

public class ExplorerBlockViewModelTest {

	@Test
	public void canSerializeViewModelWithoutTransactions() {
		// Assert:
		assertCorrectSerialization(viewModel -> { }, 0);
	}

	@Test
	public void canSerializeViewModelWithTransactions() {
		// Assert:
		assertCorrectSerialization(
				viewModel -> {
					for (int i = 0; i < 3; ++i) {
						viewModel.addTransaction(createTransferViewModel());
					}
				},
				3);
	}

	private static void assertCorrectSerialization(
			final Consumer<ExplorerBlockViewModel> addTransactions,
			final int numExpectedTransactions) {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(101);
		block.sign();
		final Hash blockHash = HashUtils.calculateHash(block);

		// Act:
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(block, blockHash);
		addTransactions.accept(viewModel);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(3));
		Assert.assertThat(getDeserializedBlockHash((JSONObject)jsonObject.get("block")), IsEqual.equalTo(blockHash));
		Assert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(blockHash.toString()));
		Assert.assertThat(((JSONArray)jsonObject.get("txes")).size(), IsEqual.equalTo(numExpectedTransactions));
	}

	private static ExplorerTransferViewModel createTransferViewModel() {
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		transaction.sign();
		return new ExplorerTransferViewModel(
				transaction,
				HashUtils.calculateHash(transaction));
	}

	private static Hash getDeserializedBlockHash(final JSONObject jsonObject) {
		final Block deserializedBlock = new Block(
				BlockTypes.REGULAR,
				VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		return HashUtils.calculateHash(deserializedBlock);
	}
}