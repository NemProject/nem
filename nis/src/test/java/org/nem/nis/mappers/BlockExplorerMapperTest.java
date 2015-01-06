package org.nem.nis.mappers;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dbmodel.*;

import java.util.ArrayList;

public class BlockExplorerMapperTest {
	private static final BlockExplorerMapper MAPPER = new BlockExplorerMapper();
	private static final String PUBLIC_KEY_STRING = "8888888899999999777777774444444488888888999999997777777744444444";

	@Test
	public void canMapBlockToExplorerBlockViewModelWithoutTransactions() {
		// Assert:
		assertCorrectSerialization();
	}

	@Test
	public void canMapBlockToExplorerBlockViewModelWithTransactions() {
		assertCorrectSerialization(3, 7, 5);
	}

	private static void assertCorrectSerialization(final long... transferFees) {
		// Arrange:
		final Address address = Address.fromPublicKey(PublicKey.fromHexString(PUBLIC_KEY_STRING));
		final Hash hash = Hash.fromHexString("00000000111111112222222233333333");

		final DbBlock block = new DbBlock();
		block.setHeight(60L);
		block.setForger(new DbAccount(address.getEncoded(), address.getPublicKey()));
		block.setTimeStamp(1856002);
		block.setBlockHash(hash);
		block.setBlockTransferTransactions(new ArrayList<>());

		for (final long fee : transferFees) {
			block.getBlockTransferTransactions().add(createTransferWithFee(fee));
		}

		// Act:
		final ExplorerBlockViewModel viewModel = MAPPER.toExplorerViewModel(block);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(6));
		Assert.assertThat(jsonObject.get("height"), IsEqual.equalTo(60L));
		Assert.assertThat(jsonObject.get("harvester"), IsEqual.equalTo(address.getEncoded()));
		Assert.assertThat(jsonObject.get("harvesterPk"), IsEqual.equalTo(PUBLIC_KEY_STRING));
		Assert.assertThat(jsonObject.get("timeStamp"), IsEqual.equalTo(1408966402000L));
		Assert.assertThat(jsonObject.get("hash"), IsEqual.equalTo("00000000111111112222222233333333"));

		final JSONArray jsonTransactions = ((JSONArray)jsonObject.get("txes"));
		Assert.assertThat(jsonTransactions.size(), IsEqual.equalTo(transferFees.length));
		for (int i = 0; i < transferFees.length; ++i) {
			Assert.assertThat(((JSONObject)jsonTransactions.get(i)).get("fee"), IsEqual.equalTo(transferFees[i]));
		}
	}

	private static DbTransferTransaction createTransferWithFee(final long fee) {
		final Address senderAddress = Utils.generateRandomAddressWithPublicKey();
		final Address recipientAddress = Utils.generateRandomAddress();
		final Hash hash = Utils.generateRandomHash();
		final Signature signature = new Signature(Utils.generateRandomBytes(64));
		final byte[] messagePayload = Utils.generateRandomBytes(16);

		final DbTransferTransaction transfer = new DbTransferTransaction();
		transfer.setFee(fee);
		transfer.setTimeStamp(1856002);
		transfer.setSender(new DbAccount(senderAddress.getEncoded(), senderAddress.getPublicKey()));
		transfer.setSenderProof(signature.getBytes());
		transfer.setTransferHash(hash);

		transfer.setRecipient(new DbAccount(recipientAddress.getEncoded(), null));
		transfer.setAmount(888888000000L);
		transfer.setMessageType(2);
		transfer.setMessagePayload(messagePayload);
		return transfer;
	}

	@Test
	public void canMapTransferToExplorerTransferViewModel() {
		// Arrange:
		final Address senderAddress = Address.fromPublicKey(PublicKey.fromHexString(PUBLIC_KEY_STRING));
		final Address recipientAddress = Address.fromEncoded("RECIPIENT");
		final Hash hash = Hash.fromHexString("00000000111111112222222233333333");
		final Signature signature = new Signature(Utils.generateRandomBytes(64));
		final byte[] messagePayload = Utils.generateRandomBytes(16);

		final DbTransferTransaction transfer = new DbTransferTransaction();
		transfer.setFee(123000000L);
		transfer.setTimeStamp(1856002);
		transfer.setSender(new DbAccount(senderAddress.getEncoded(), senderAddress.getPublicKey()));
		transfer.setSenderProof(signature.getBytes());
		transfer.setTransferHash(hash);

		transfer.setRecipient(new DbAccount(recipientAddress.getEncoded(), null));
		transfer.setAmount(888888000000L);
		transfer.setMessageType(2);
		transfer.setMessagePayload(messagePayload);

		// Act:
		final ExplorerTransferViewModel viewModel = MAPPER.toExplorerViewModel(transfer);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(11));
		Assert.assertThat(jsonObject.get("fee"), IsEqual.equalTo(123000000L));
		Assert.assertThat(jsonObject.get("timeStamp"), IsEqual.equalTo(1408966402000L));
		Assert.assertThat(jsonObject.get("sender"), IsEqual.equalTo(senderAddress.getEncoded()));
		Assert.assertThat(jsonObject.get("senderPk"), IsEqual.equalTo(PUBLIC_KEY_STRING));
		Assert.assertThat(jsonObject.get("signature"), IsEqual.equalTo(signature.toString()));
		Assert.assertThat(jsonObject.get("hash"), IsEqual.equalTo("00000000111111112222222233333333"));

		Assert.assertThat(jsonObject.get("recipient"), IsEqual.equalTo("RECIPIENT"));
		Assert.assertThat(jsonObject.get("amount"), IsEqual.equalTo(888888000000L));
		Assert.assertThat(jsonObject.get("msgType"), IsEqual.equalTo(2));
		Assert.assertThat(jsonObject.get("message"), IsEqual.equalTo(HexEncoder.getString(messagePayload)));
	}
}