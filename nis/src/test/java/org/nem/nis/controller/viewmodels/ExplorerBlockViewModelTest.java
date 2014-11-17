package org.nem.nis.controller.viewmodels;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.time.UnixTime;

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
		final Address address = Address.fromPublicKey(PublicKey.fromHexString("88888888999999997777777744444444"));
		final Hash hash = Hash.fromHexString("00000000111111112222222233333333");

		// Act:
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(
				new BlockHeight(60),
				address,
				UnixTime.fromUnixTimeInMillis(1408966402000L),
				hash);
		addTransactions.accept(viewModel);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(6));
		Assert.assertThat(jsonObject.get("height"), IsEqual.equalTo(60L));
		Assert.assertThat(jsonObject.get("harvester"), IsEqual.equalTo(address.getEncoded()));
		Assert.assertThat(jsonObject.get("harvesterPk"), IsEqual.equalTo("88888888999999997777777744444444"));
		Assert.assertThat(jsonObject.get("timeStamp"), IsEqual.equalTo(1408966402000L));
		Assert.assertThat(jsonObject.get("hash"), IsEqual.equalTo("00000000111111112222222233333333"));
		Assert.assertThat(((JSONArray)jsonObject.get("txes")).size(), IsEqual.equalTo(numExpectedTransactions));
	}

	private static ExplorerTransferViewModel createTransferViewModel() {
		return new ExplorerTransferViewModel(
				7,
				Amount.fromNem(123),
				UnixTime.fromUnixTimeInMillis(1408966402000L),
				Utils.generateRandomAddressWithPublicKey(),
				new Signature(Utils.generateRandomBytes(64)),
				Utils.generateRandomHash(),
				Utils.generateRandomAddress(),
				Amount.fromNem(888888),
				7,
				Utils.generateRandomBytes(16));
	}
}