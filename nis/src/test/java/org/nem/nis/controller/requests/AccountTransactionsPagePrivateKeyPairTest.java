package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;

public class AccountTransactionsPagePrivateKeyPairTest {

	// region construction

	@Test
	public void canCreateAccountTransactionsPagePrivateKeyPairFromParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountTransactionsPage page = new AccountTransactionsPage(Address.fromPublicKey(keyPair.getPublicKey()).getEncoded(), null, null);

		// Act:
		final AccountTransactionsPagePrivateKeyPair pair = new AccountTransactionsPagePrivateKeyPair(page, keyPair.getPrivateKey());

		// Assert:
		Assert.assertThat(pair.getPage(), IsSame.sameInstance(page));
		Assert.assertThat(pair.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
	}

	@Test
	public void cannotCreateAccountTransactionsPagePrivateKeyPairFromParametersIfAddressDoesNotMatchPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountTransactionsPage page = new AccountTransactionsPage(Utils.generateRandomAddress().getEncoded(), null, null);

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPagePrivateKeyPair(page, keyPair.getPrivateKey()), IllegalArgumentException.class);
	}

	@Test
	public void canCreateAccountTransactionsPagePrivateKeyPairFromDeserializer() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Address address = Address.fromPublicKey(keyPair.getPublicKey());
		final Deserializer deserializer = this.createDeserializer(address, keyPair.getPrivateKey());

		// Act:
		final AccountTransactionsPagePrivateKeyPair pair = new AccountTransactionsPagePrivateKeyPair(deserializer);

		// Assert:
		Assert.assertThat(pair.getPage().getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(pair.getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
	}

	@Test
	public void cannotCreateAccountTransactionsPagePrivateKeyPairFromDeserializerIfAddressDoesNotMatchPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Deserializer deserializer = this.createDeserializer(Utils.generateRandomAddress(), keyPair.getPrivateKey());

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPagePrivateKeyPair(deserializer), IllegalArgumentException.class);
	}

	// endregion

	// region createPageBuilder

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenAllOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountTransactionsPage originalPage = new AccountTransactionsPage(
				Address.fromPublicKey(keyPair.getPublicKey()).getEncoded(),
				Utils.generateRandomHash().toString(),
				"123");
		final AccountTransactionsPagePrivateKeyPair pair = new AccountTransactionsPagePrivateKeyPair(originalPage, keyPair.getPrivateKey());

		// Act:
		final AccountTransactionsPageBuilder builder = pair.createPageBuilder();
		final AccountTransactionsPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(originalPage.getAddress()));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(originalPage.getHash()));
		Assert.assertThat(page.getId(), IsEqual.equalTo(originalPage.getId()));
	}

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenNoOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountTransactionsPage originalPage = new AccountTransactionsPage(
				Address.fromPublicKey(keyPair.getPublicKey()).getEncoded(),
				null,
				null);
		final AccountTransactionsPagePrivateKeyPair pair = new AccountTransactionsPagePrivateKeyPair(originalPage, keyPair.getPrivateKey());

		// Act:
		final AccountTransactionsPageBuilder builder = pair.createPageBuilder();
		final AccountTransactionsPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(originalPage.getAddress()));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	// endregion

	private Deserializer createDeserializer(final Address address, final PrivateKey privateKey) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("address", address.getEncoded());
		jsonObject.put("value", privateKey.toString());
		return Utils.createDeserializer(jsonObject);
	}
}
