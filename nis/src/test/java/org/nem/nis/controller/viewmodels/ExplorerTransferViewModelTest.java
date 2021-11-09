package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.BiFunction;

public class ExplorerTransferViewModelTest {

	@Test
	public void canSerializeNonMultisigViewModel() {
		// Arrange:
		final Transaction tx = RandomTransactionFactory.createTransfer();
		tx.sign();
		final Hash txHash = HashUtils.calculateHash(tx);

		// Act:
		final ExplorerTransferViewModel viewModel = new ExplorerTransferViewModel(tx, txHash);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(getDeserializedTxHash((JSONObject) jsonObject.get("tx")), IsEqual.equalTo(txHash));
		MatcherAssert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(txHash.toString()));
	}

	@Test
	public void canSerializeMultisigViewModel() {
		// Arrange:
		final MultisigTransaction tx = RandomTransactionFactory.createMultisigTransfer();
		tx.sign();
		final Hash txHash = HashUtils.calculateHash(tx);
		final Hash innerTxHash = HashUtils.calculateHash(tx.getOtherTransaction());

		// Act:
		final ExplorerTransferViewModel viewModel = new ExplorerTransferViewModel(tx, txHash);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(getDeserializedMultisigTxHash((JSONObject) jsonObject.get("tx")), IsEqual.equalTo(txHash));
		MatcherAssert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(txHash.toString()));
		MatcherAssert.assertThat(jsonObject.get("innerHash"), IsEqual.equalTo(innerTxHash.toString()));
	}

	private static Hash getDeserializedTxHash(final JSONObject jsonObject) {
		return getDeserializedTxHash(jsonObject, TransferTransaction::new);
	}

	private static Hash getDeserializedMultisigTxHash(final JSONObject jsonObject) {
		return getDeserializedTxHash(jsonObject, MultisigTransaction::new);
	}

	private static Hash getDeserializedTxHash(final JSONObject jsonObject,
			final BiFunction<VerifiableEntity.DeserializationOptions, Deserializer, Transaction> deserialize) {
		final Transaction deserializedTx = deserialize.apply(VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		return HashUtils.calculateHash(deserializedTx);
	}
}
