package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class AccountTransactionsPageTest {

	@Test
	public void accountTransactionsPageCanBeCreatedAroundOnlyValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), null, null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndValidHash() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), "ffeeddccbbaa99887766554433221100", null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndValidId() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), null, "12345");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundCompleteValidData() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), "ffeeddccbbaa99887766554433221100", "12345");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
	}

	@Test
	public void accountTransactionsPageCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPage(null, null, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPage("ABC", null, null), IllegalArgumentException.class);
	}

	@Test
	public void canCreateAccountTransactionsPageFromDeserializer() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final Hash hash = Utils.generateRandomHash();
		final Long id = 123L;
		final Deserializer deserializer = this.createDeserializer(address, hash, id);

		// Act:
		final AccountTransactionsPage page = new AccountTransactionsPage(deserializer);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(hash));
		Assert.assertThat(page.getId(), IsEqual.equalTo(123L));
	}

	@Test
	public void canCreateAccountTransactionsPageFromDeserializerWithEmptyHashAndId() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final Deserializer deserializer = this.createDeserializer(address, null, null);

		// Act:
		final AccountTransactionsPage page = new AccountTransactionsPage(deserializer);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(null));
		Assert.assertThat(page.getId(), IsEqual.equalTo(null));
	}

	@Test
	public void cannotCreateAccountTransactionsPageFromDeserializerWithInvalidAddress() {
		// Arrange:
		final Address address = Address.fromEncoded("blah");
		final Hash hash = Utils.generateRandomHash();
		final Long id = 123L;
		final Deserializer deserializer = this.createDeserializer(address, hash, id);

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPage(deserializer), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountTransactionsPageFromDeserializerWithNoAddress() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Long id = 123L;
		final Deserializer deserializer = this.createDeserializer(null, hash, id);

		// Act + Assert:
		ExceptionAssert.assertThrows(
				v -> new AccountTransactionsPage(deserializer),
				MissingRequiredPropertyException.class);
	}

	private Deserializer createDeserializer(final Address address, final Hash hash, final Long id) {
		final JSONObject jsonObject = new JSONObject();
		if (null != address) {
			jsonObject.put("address", address.getEncoded());
		}

		if (null != hash) {
			jsonObject.put("hash", hash.toString());
		}

		if (null != hash) {
			jsonObject.put("id", id.toString());
		}

		return Utils.createDeserializer(jsonObject);
	}
}