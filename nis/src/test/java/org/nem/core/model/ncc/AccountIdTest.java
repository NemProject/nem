package org.nem.core.model.ncc;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.math.BigInteger;

public class AccountIdTest {

	@Test
	public void accountIdCanBeCreatedAroundValidEncodedAddressString() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountId accountId = new AccountId(address.getEncoded());

		// Assert:
		Assert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCanBeCreatedAroundValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountId accountId = new AccountId(address);

		// Assert:
		Assert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountId((Address)null), IllegalArgumentException.class);
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

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final SerializableAccountId originalRequest = new SerializableAccountId(address);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalRequest, null);

		// Act:
		final AccountId request = new AccountId(deserializer);

		// Assert:
		Assert.assertThat(request.getAddress(), IsEqual.equalTo(address));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountId request = new AccountId(address);

		// Assert:
		Assert.assertThat(request, IsEqual.equalTo(new AccountId(address)));
		Assert.assertThat(request, IsEqual.equalTo(new AccountId(address.getEncoded())));
		Assert.assertThat(request, IsEqual.equalTo(new AccountId(Address.fromEncoded(address.getEncoded()))));
		Assert.assertThat(request, IsNot.not(IsEqual.equalTo(new AccountId(Utils.generateRandomAddress()))));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(request)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)request)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountId request = new AccountId(address);
		final int hashCode = request.hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(new AccountId(address).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(new AccountId(address.getEncoded()).hashCode()));
		Assert.assertThat(request, IsEqual.equalTo(new AccountId(Address.fromEncoded(address.getEncoded()))));
		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new AccountId(Utils.generateRandomAddress()).hashCode())));
	}

	//endregion

	private AccountId createAccountIdFromJson(final String address) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("account", address);
		return new AccountId(new JsonDeserializer(jsonObject, null));
	}
}