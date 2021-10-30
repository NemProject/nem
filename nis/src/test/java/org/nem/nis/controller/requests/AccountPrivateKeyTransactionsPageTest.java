package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
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
		MatcherAssert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(page.getHash(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getPageSize(), IsNull.nullValue());
	}

	@Test
	public void canCreatePageWithAllOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey(), hash.toString(),
				"1234", "56");

		// Assert:
		MatcherAssert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(page.getHash(), IsEqual.equalTo(hash));
		MatcherAssert.assertThat(page.getId(), IsEqual.equalTo(1234L));
		MatcherAssert.assertThat(page.getPageSize(), IsEqual.equalTo(56));
	}

	@Test
	public void cannotPageIfPrivateKeyIsNull() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountPrivateKeyTransactionsPage(null, hash.toString(), "1234", "56"),
				IllegalArgumentException.class);
	}

	@Test
	public void canCreatePageWithoutOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey(), null, null, null);

		// Assert:
		MatcherAssert.assertThat(page.getPrivateKey(), IsSame.sameInstance(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(page.getHash(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getPageSize(), IsNull.nullValue());
	}

	// endregion

	// region deserialization

	@Test
	public void canDeserializePageWithAllOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();
		final Deserializer deserializer = this.createDeserializer(keyPair.getPrivateKey(), hash, 1234L, 56);

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(deserializer);

		// Assert:
		MatcherAssert.assertThat(page.getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(page.getHash(), IsEqual.equalTo(hash));
		MatcherAssert.assertThat(page.getId(), IsEqual.equalTo(1234L));
		MatcherAssert.assertThat(page.getPageSize(), IsEqual.equalTo(56));
	}

	@Test
	public void cannotDeserializePageIfPrivateKeyIsMissing() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Deserializer deserializer = this.createDeserializer(null, hash, 1234L, 56);

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountPrivateKeyTransactionsPage(deserializer), MissingRequiredPropertyException.class);
	}

	@Test
	public void canDeserializePageWithoutOptionalParameters() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Deserializer deserializer = this.createDeserializer(keyPair.getPrivateKey(), null, null, null);

		// Act:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(deserializer);

		// Assert:
		MatcherAssert.assertThat(page.getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		MatcherAssert.assertThat(page.getHash(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getPageSize(), IsNull.nullValue());
	}

	// endregion

	// region createIdBuilder / createPageBuilder

	@Test
	public void createIdBuilderReturnsExpectedBuilderWhenAllOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Hash hash = Utils.generateRandomHash();
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey(), hash.toString(),
				"1234", "56");

		// Act:
		final AccountTransactionsIdBuilder builder = page.createIdBuilder();
		final AccountTransactionsId id = builder.build();

		// Assert:
		MatcherAssert.assertThat(id.getAddress(), IsEqual.equalTo(Address.fromPublicKey(keyPair.getPublicKey())));
		MatcherAssert.assertThat(id.getHash(), IsEqual.equalTo(hash));
	}

	@Test
	public void createIdBuilderReturnsExpectedBuilderWhenNoOptionalParametersAreSpecified() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(keyPair.getPrivateKey(), null, null, null);

		// Act:
		final AccountTransactionsIdBuilder builder = page.createIdBuilder();
		final AccountTransactionsId id = builder.build();

		// Assert:
		MatcherAssert.assertThat(id.getAddress(), IsEqual.equalTo(Address.fromPublicKey(keyPair.getPublicKey())));
		MatcherAssert.assertThat(id.getHash(), IsNull.nullValue());
	}

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenAllOptionalParametersAreSpecified() {
		// Arrange:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(new KeyPair().getPrivateKey(),
				Utils.generateRandomHash().toString(), "1234", "56");

		// Act:
		final DefaultPageBuilder builder = page.createPageBuilder();
		final DefaultPage defaultPage = builder.build();

		// Assert:
		MatcherAssert.assertThat(defaultPage.getId(), IsEqual.equalTo(1234L));
		MatcherAssert.assertThat(defaultPage.getPageSize(), IsEqual.equalTo(56));
	}

	@Test
	public void createPageBuilderReturnsExpectedBuilderWhenNoOptionalParametersAreSpecified() {
		// Arrange:
		final AccountPrivateKeyTransactionsPage page = new AccountPrivateKeyTransactionsPage(new KeyPair().getPrivateKey(), null, null,
				null);

		// Act:
		final DefaultPageBuilder builder = page.createPageBuilder();
		final DefaultPage defaultPage = builder.build();

		// Assert:
		MatcherAssert.assertThat(defaultPage.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(defaultPage.getPageSize(), IsEqual.equalTo(25));
	}

	// endregion

	private Deserializer createDeserializer(final PrivateKey privateKey, final Hash hash, final Long id, final Integer pageSize) {
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
		if (null != pageSize) {
			jsonObject.put("pageSize", pageSize.toString());
		}
		return Utils.createDeserializer(jsonObject);
	}
}
