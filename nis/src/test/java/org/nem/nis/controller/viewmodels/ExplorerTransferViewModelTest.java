package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.time.UnixTime;
import org.nem.core.utils.HexEncoder;

public class ExplorerTransferViewModelTest {
	private static final String PUBLIC_KEY_STRING = "8888888899999999777777774444444488888888999999997777777744444444";
	private static final long UNIX_TIME = 1424604802000L;

	@Test
	public void canSerializeViewModel() {
		// Arrange:
		final Address senderAddress = Address.fromPublicKey(PublicKey.fromHexString(PUBLIC_KEY_STRING));
		final Address recipientAddress = Address.fromEncoded("RECIPIENT");
		final Hash hash = Hash.fromHexString("00000000111111112222222233333333");
		final Signature signature = new Signature(Utils.generateRandomBytes(64));
		final byte[] messagePayload = Utils.generateRandomBytes(16);

		// Act:
		final ExplorerTransferViewModel viewModel = new ExplorerTransferViewModel(
				7,
				Amount.fromNem(123),
				UnixTime.fromUnixTimeInMillis(UNIX_TIME),
				senderAddress,
				signature,
				hash,
				recipientAddress,
				Amount.fromNem(888888),
				2,
				messagePayload);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(11));
		Assert.assertThat(jsonObject.get("type"), IsEqual.equalTo(7));
		Assert.assertThat(jsonObject.get("fee"), IsEqual.equalTo(123000000L));
		Assert.assertThat(jsonObject.get("timeStamp"), IsEqual.equalTo(UNIX_TIME));
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