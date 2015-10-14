package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class AccountPrivateKeyTransactionsPageTest {

	// region construction

	@Test
	public void canCreatePageWithOnlyPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey());

		// Assert:
		Assert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	@Test
	public void canCreatePageWithAllOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();
		final Long id = 1234L;

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(
				keyPair.getPrivateKey(),
				hash.toString(),
				id.toString());

		// Assert:
		Assert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(page.getId(), IsEqual.equalTo(1234L));
	}

	@Test
	public void cannotPageIfPrivateKeyIsNull() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Long id = 1234L;

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new AccountPrivateKeyTransactionsPage(null, hash.toString(), id.toString()),
				IllegalArgumentException.class);
	}

	@Test
	public void canCreatePageWithoutOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey(), null, null);

		// Assert:
		Assert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		Assert.assertThat(null, IsEqual.equalTo(page.getHash()));
		Assert.assertThat(null, IsEqual.equalTo(page.getId()));
	}

	// endregion

	// region deserialization

	@Test
	public void canDeserializePageWithAllOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();
		final Long id = 1234L;
		final Deserializer deserializer = this.createDeserializer(keyPair.getPrivateKey(), hash, id);

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(deserializer);

		// Assert:
		Assert.assertThat(page.getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(page.getId(), IsEqual.equalTo(id));
	}

	@Test
	public void cannotDeserializePageIfPrivateKeyIsMissing() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Long id = 1234L;
		final Deserializer deserializer = this.createDeserializer(null, hash, id);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new AccountPrivateKeyTransactionsPage(deserializer),
				MissingRequiredPropertyException.class);
	}

	@Test
	public void canDeserializePageWithoutOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Deserializer deserializer = this.createDeserializer(keyPair.getPrivateKey(), null, null);

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(deserializer);

		// Assert:
		Assert.assertThat(page.getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		Assert.assertThat(null, IsEqual.equalTo(page.getHash()));
		Assert.assertThat(null, IsEqual.equalTo(page.getId()));
	}

	// endregion

	// region createIdBuilder / createPageBuilder

	@Test
	public void createIdBuilderReturnsExpectedBuilderWhenAllOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(
				keyPair.getPrivateKey(),
				hash.toString(),
				"1234");

		// Act:
		final AccountTransactionsIdBuilder builder = page.createIdBuilder();
		final AccountTransactionsId id = builder.build();

		// Assert:
		Assert.assertThat(id.getAddress(), IsEqual.equalTo(Address.fromPublicKey(keyPair.getPublicKey())));
		Assert.assertThat(id.getHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void createIdBuilderReturnsExpectedBuilderWhenNoOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(
				keyPair.getPrivateKey(),
				null,
				null);

		// Act:
		final AccountTransactionsIdBuilder builder = page.createIdBuilder();
		final AccountTransactionsId id = builder.build();

		// Assert:
		Assert.assertThat(id.getAddress(), IsEqual.equalTo(Address.fromPublicKey(keyPair.getPublicKey())));
		Assert.assertThat(id.getHash(), IsNull.nullValue());
	}

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenAllOptionalParametersAreSpecified() {
		// Arrange:
		final Long id = 1234L;
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(
				new KeyPair().getPrivateKey(),
				Utils.generateRandomHash().toString(),
				id.toString());

		// Act:
		final DefaultPageBuilder builder = page.createPageBuilder();
		final DefaultPage defaultPage = builder.build();

		// Assert:
		Assert.assertThat(defaultPage.getId(), IsEqual.equalTo(id));
	}

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenNoOptionalParametersAreSpecified() {
		// Arrange:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(
				new KeyPair().getPrivateKey(),
				null,
				null);

		// Act:
		final DefaultPageBuilder builder = page.createPageBuilder();
		final DefaultPage defaultPage = builder.build();

		// Assert:
		Assert.assertThat(defaultPage.getId(), IsNull.nullValue());
	}

	// endregion

	private Deserializer createDeserializer(final PrivateKey privateKey, final Hash hash, final Long id) {
		final JSONObject jsonObject = new JSONObject();
		if (null != privateKey) {
			jsonObject.put("value", privateKey.toString());
		}
		if (null != hash) {
			jsonObject.put("hash", hash.toString());
		}
		if (null != id) {
			jsonObject.put("id", id.toString());
		}
		return Utils.createDeserializer(jsonObject);
	}
}
