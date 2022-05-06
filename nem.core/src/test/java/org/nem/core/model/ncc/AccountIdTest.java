package org.nem.core.model.ncc;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
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
		MatcherAssert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCanBeCreatedAroundValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountId accountId = new AccountId(address);

		// Assert:
		MatcherAssert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountId((Address) null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountId((String) null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountId("ABC"), IllegalArgumentException.class);
	}

	@Test
	public void accountIdCanBeDeserializedFromJsonWithValidAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Act:
		final AccountId accountId = this.createAccountIdFromJson(address.getEncoded());

		// Assert:
		MatcherAssert.assertThat(accountId.getAddress(), IsEqual.equalTo(address));
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
		MatcherAssert.assertThat(request.getAddress(), IsEqual.equalTo(address));
	}

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountId request = new AccountId(address);

		// Assert:
		MatcherAssert.assertThat(request, IsEqual.equalTo(new AccountId(address)));
		MatcherAssert.assertThat(request, IsEqual.equalTo(new AccountId(address.getEncoded())));
		MatcherAssert.assertThat(request, IsEqual.equalTo(new AccountId(Address.fromEncoded(address.getEncoded()))));
		MatcherAssert.assertThat(request, IsNot.not(IsEqual.equalTo(new AccountId(Utils.generateRandomAddress()))));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(request)));
		MatcherAssert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object) request)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountId request = new AccountId(address);
		final int hashCode = request.hashCode();

		// Assert:
		MatcherAssert.assertThat(hashCode, IsEqual.equalTo(new AccountId(address).hashCode()));
		MatcherAssert.assertThat(hashCode, IsEqual.equalTo(new AccountId(address.getEncoded()).hashCode()));
		MatcherAssert.assertThat(request, IsEqual.equalTo(new AccountId(Address.fromEncoded(address.getEncoded()))));
		MatcherAssert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(new AccountId(Utils.generateRandomAddress()).hashCode())));
	}

	// endregion

	private AccountId createAccountIdFromJson(final String address) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("account", address);
		return new AccountId(new JsonDeserializer(jsonObject, null));
	}
}
