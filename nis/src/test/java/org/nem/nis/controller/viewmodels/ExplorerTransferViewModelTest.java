package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.nis.test.RandomTransactionFactory;

public class ExplorerTransferViewModelTest {

	@Test
	public void canSerializeViewModel() {
		// Arrange:
		final Transaction tx = RandomTransactionFactory.createTransfer();
		tx.sign();
		final Hash txHash = HashUtils.calculateHash(tx);

		// Act:
		final ExplorerTransferViewModel viewModel = new ExplorerTransferViewModel(
				tx,
				txHash);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		Assert.assertThat(getDeserializedTxHash((JSONObject)jsonObject.get("tx")), IsEqual.equalTo(txHash));
		Assert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(txHash.toString()));
	}

	private static Hash getDeserializedTxHash(final JSONObject jsonObject) {
		final TransferTransaction deserializedTx = new TransferTransaction(
				VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		return HashUtils.calculateHash(deserializedTx);
	}
}