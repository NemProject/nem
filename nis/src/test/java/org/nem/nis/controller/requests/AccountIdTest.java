package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class AccountIdTest {

	@Test
	public void accountIdCanBeCreatedAroundValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountId accountId = new AccountId(address.getEncoded());

		// Assert:
		Assert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountId((String)null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountId("ABC"), IllegalArgumentException.class);
	}

	@Test
	public void accountIdCanBeDeserializedFromJsonWithValidAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Act:
		final AccountId accountId = this.createAccountIdFromJson(address.getEncoded());

		// Assert:
		Assert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCannotBeDeserializedFromJsonWithInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> this.createAccountIdFromJson(null), MissingRequiredPropertyException.class);
		ExceptionAssert.assertThrows(v -> this.createAccountIdFromJson("ABC"), IllegalArgumentException.class);
	}

	private AccountId createAccountIdFromJson(final String address) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("account", address);
		return new AccountId(new JsonDeserializer(jsonObject, null));
	}
}