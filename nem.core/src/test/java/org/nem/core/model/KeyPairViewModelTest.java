package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class KeyPairViewModelTest {

	@Test
	public void viewModelCanBeCreated() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final KeyPairViewModel viewModel = new KeyPairViewModel(keyPair, (byte)17);

		// Assert:
		Assert.assertThat(viewModel.getKeyPair(), IsEqual.equalTo(keyPair));
		Assert.assertThat(viewModel.getNetworkVersion(), IsEqual.equalTo((byte)17));
	}

	@Test
	public void ctorThrowsIfPublicKeyIsNotDerivedFromPrivateKey() {
		// Arrange:
		final KeyPairViewModel viewModel = new KeyPairViewModel(new KeyPair(), (byte)0x98);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);
		jsonObject.replace("privateKey", new KeyPair().getPrivateKey().toString());

		// Act:
		ExceptionAssert.assertThrows(
				v -> new KeyPairViewModel(new JsonDeserializer(jsonObject, null)),
				IllegalArgumentException.class);
	}

	@Test
	public void ctorThrowsIfAddressIsNotDerivedFromPublicKey() {
		// Arrange:
		final KeyPairViewModel viewModel = new KeyPairViewModel(new KeyPair(), (byte)0x98);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);
		jsonObject.replace("address", Utils.generateRandomAddress().getEncoded());

		// Act:
		ExceptionAssert.assertThrows(
				v -> new KeyPairViewModel(new JsonDeserializer(jsonObject, null)),
				IllegalArgumentException.class);
	}

	@Test
	public void ctorGivesPrecedenceToAddressMismatchOverKeyMismatch() {
		// Arrange:
		final KeyPairViewModel viewModel = new KeyPairViewModel(new KeyPair(), (byte)0x98);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);
		jsonObject.replace("privateKey", new KeyPair().getPrivateKey().toString());
		jsonObject.replace("address", Utils.generateRandomAddress().getEncoded());

		// Act:
		ExceptionAssert.assertThrows(
				v -> new KeyPairViewModel(new JsonDeserializer(jsonObject, null)),
				IllegalArgumentException.class);
	}

	@Test
	public void viewModelCanBeRoundTripped() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final KeyPairViewModel originalViewModel = new KeyPairViewModel(keyPair, (byte)0x68);

		// Act
		final KeyPairViewModel viewModel = new KeyPairViewModel(Utils.roundtripSerializableEntity(originalViewModel, null));

		// Assert:
		Assert.assertThat(viewModel.getKeyPair().getPrivateKey(), IsEqual.equalTo(originalViewModel.getKeyPair().getPrivateKey()));
		Assert.assertThat(viewModel.getKeyPair().getPublicKey(), IsEqual.equalTo(originalViewModel.getKeyPair().getPublicKey()));
		Assert.assertThat(viewModel.getNetworkVersion(), IsEqual.equalTo(originalViewModel.getNetworkVersion()));
	}

	@Test
	public void viewModelCanBeSerialized() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final KeyPairViewModel viewModel = new KeyPairViewModel(keyPair, (byte)17);

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(3));
		Assert.assertThat(jsonObject.get("privateKey"), IsEqual.equalTo(keyPair.getPrivateKey().toString()));
		Assert.assertThat(jsonObject.get("publicKey"), IsEqual.equalTo(keyPair.getPublicKey().toString()));
		Assert.assertThat(jsonObject.get("address"), IsEqual.equalTo(Address.fromPublicKey((byte)17, keyPair.getPublicKey()).toString()));
	}
}
