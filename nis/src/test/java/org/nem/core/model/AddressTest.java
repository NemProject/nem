package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.util.function.*;

public class AddressTest {

	@Test
	public void addressCanBeCreatedAroundEncodedAddress() {
		// Act:
		final Address address = Address.fromEncoded("Sigma Gamma");

		// Assert:
		Assert.assertThat(address.getEncoded(), IsEqual.equalTo("SIGMA GAMMA"));
		Assert.assertThat(address.getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void addressCanBeCreatedAroundPublicKey() {
		// Act:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address.getEncoded(), IsNull.notNullValue());
		Assert.assertThat(address.getPublicKey(), IsEqual.equalTo(publicKey));
	}

	@Test
	public void sameAddressIsGeneratedForSameInputs() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address1 = Address.fromPublicKey(publicKey);
		final Address address2 = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address2, IsEqual.equalTo(address1));
	}

	@Test
	public void differentAddressesAreGeneratedForDifferentInputs() {
		// Arrange:
		final PublicKey publicKey1 = Utils.generateRandomPublicKey();
		final PublicKey publicKey2 = Utils.generateRandomPublicKey();

		// Act:
		final Address address1 = Address.fromPublicKey(publicKey1);
		final Address address2 = Address.fromPublicKey(publicKey2);

		// Assert:
		Assert.assertThat(address2, IsNot.not(IsEqual.equalTo(address1)));
	}

	@Test
	public void generatedAddressIsValid() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void addressesWithDifferentCasingsAreValid() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(addressAsLower(address).isValid(), IsEqual.equalTo(true));
		Assert.assertThat(addressAsUpper(address).isValid(), IsEqual.equalTo(true));
		Assert.assertThat(addressAsMixed(address).isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void generatedAddressHas40CharLength() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address.getEncoded().length(), IsEqual.equalTo(40));
	}

	@Test
	public void generatedAddressBeginsWithDefaultNetworkAddressStartChar() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address.getEncoded().charAt(0), IsEqual.equalTo(NetworkInfo.getDefault().getAddressStartChar()));
	}

	@Test
	public void addressWithNonBase32CharactersIsNotValid() {
		// Assert:
		assertInvalidAddressAfterTransform(address -> address.substring(0, 20) + "*" + address.substring(21));
	}

	@Test
	public void addressWithIncorrectLengthIsNotValid() {
		// Assert:
		assertInvalidAddressAfterTransform(address -> address.substring(0, address.length() - 1));
	}

	@Test
	public void addressWithIncorrectNumberOfDecodedBytesIsNotValid() {
		// Assert:
		assertInvalidAddressAfterTransform(address -> address.substring(0, address.length() - 8) + "========");
	}

	private static void assertInvalidAddressAfterTransform(final Function<String, String> transform) {
		// Act:
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());
		final String invalidAddress = transform.apply(address.toString());

		// Assert:
		Assert.assertThat(Address.fromEncoded(invalidAddress).isValid(), IsEqual.equalTo(false));
	}

	@Test
	public void addressWithIncorrectVersionIsNotValid() {
		// Assert:
		assertAddressIsNotValidIfChangedAtIndex(0);
	}

	@Test
	public void addressWithIncorrectHashIsNotValid() {
		// Assert:
		assertAddressIsNotValidIfChangedAtIndex(5);
	}

	@Test
	public void addressWithIncorrectChecksumIsNotValid() {
		// Assert:
		assertAddressIsNotValidIfChangedAtIndex(39);
	}

	private void assertAddressIsNotValidIfChangedAtIndex(final int index) {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address = Address.fromPublicKey(publicKey);
		final String fakeAddress = Utils.modifyBase32AtIndex(address.getEncoded(), index);

		// Assert:
		Assert.assertThat(Address.fromEncoded(fakeAddress).isValid(), IsEqual.equalTo(false));
	}

	@Ignore
	@Test
	public void addressWithLeadingWhitespaceIsInvalid() {
		// Assert:
		assertAddressWithPaddingInvalid((address, padding) -> padding + address.toString());
	}

	@Ignore
	@Test
	public void addressWithTrailingWhitespaceIsInvalid() {
		// Assert:
		assertAddressWithPaddingInvalid((address, padding) -> address.toString() + padding);
	}

	@Ignore
	@Test
	public void addressWithLeadingAndTrailingWhitespaceIsInvalid() {
		// Assert:
		assertAddressWithPaddingInvalid((address, padding) -> padding + address.toString() + padding);
	}

	private static void assertAddressWithPaddingInvalid(final BiFunction<Address, String, String> paddingFunction) {
		// Arrange:
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());

		// Assert:
		for (final String padding : new String[] { " ", "\t", "  \t \t " }) {
			final String paddedAddress = paddingFunction.apply(address, padding);
			Assert.assertThat(Address.fromEncoded(paddedAddress).isValid(), IsEqual.equalTo(false));
		}
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(Address.fromPublicKey(publicKey), IsEqual.equalTo(address));
		Assert.assertThat(Address.fromEncoded(address.getEncoded()), IsEqual.equalTo(address));
		Assert.assertThat(addressAsLower(address), IsEqual.equalTo(address));
		Assert.assertThat(addressAsUpper(address), IsEqual.equalTo(address));
		Assert.assertThat(addressAsMixed(address), IsEqual.equalTo(address));
		Assert.assertThat(Address.fromPublicKey(Utils.mutate(publicKey)), IsNot.not(IsEqual.equalTo(address)));
		Assert.assertThat(Address.fromEncoded(Utils.incrementAtIndex(address.getEncoded(), 0)), IsNot.not(IsEqual.equalTo(address)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(address)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)address)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey(publicKey);
		final int hashCode = address.hashCode();

		// Assert:
		Assert.assertThat(Address.fromPublicKey(publicKey).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(Address.fromEncoded(address.getEncoded()).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(addressAsLower(address).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(addressAsUpper(address).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(addressAsMixed(address).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(Address.fromPublicKey(Utils.mutate(publicKey)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(Address.fromEncoded(Utils.incrementAtIndex(address.getEncoded(), 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAddressWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("BlahAddress");

		// Act:
		Address.writeTo(serializer, "address", address);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("address"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canWriteAddressWithAddressEncoding() {
		// Arrange:
		final Address address = Address.fromEncoded("BlahAddress");

		// Assert:
		assertCanWriteAddressWithEncoding(
				address,
				AddressEncoding.COMPRESSED,
				address.getEncoded());
	}

	@Test
	public void canWriteAddressWithPublicKeyEncoding() {
		// Arrange:
		final Address address = Address.fromPublicKey((new KeyPair()).getPublicKey());

		// Assert:
		assertCanWriteAddressWithEncoding(
				address,
				AddressEncoding.PUBLIC_KEY,
				address.getPublicKey().toString());
	}

	@Test
	public void canWriteAddressThatDoesNotHavePublicKeyWithPublicKeyEncoding() {
		// Arrange:
		final Address address = Address.fromEncoded("BlahAddress");

		// Assert:
		assertCanWriteAddressWithEncoding(
				address,
				AddressEncoding.PUBLIC_KEY,
				null);
	}

	private static void assertCanWriteAddressWithEncoding(
			final Address address,
			final AddressEncoding encoding,
			final String expectedSerializedString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		Address.writeTo(serializer, "address", address, encoding);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("address"), IsEqual.equalTo(expectedSerializedString));
	}

	@Test
	public void canRoundtripAddressWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address originalAddress = Address.fromEncoded("BlahAddress");

		// Act:
		Address.writeTo(serializer, "address", originalAddress);
		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Address address = Address.readFrom(deserializer, "address");

		// Assert:
		Assert.assertThat(originalAddress, IsEqual.equalTo(address));
	}

	@Test
	public void canRoundtripAddressWithAddressEncoding() {
		// Assert:
		assertAddressRoundTripInMode(AddressEncoding.COMPRESSED, false);
	}

	@Test
	public void canRoundtripAddressWithPublicKeyEncoding() {
		// Assert:
		assertAddressRoundTripInMode(AddressEncoding.PUBLIC_KEY, true);
	}

	private void assertAddressRoundTripInMode(final AddressEncoding encoding, final boolean isPublicKeyPreserved) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address originalAddress = Utils.generateRandomAddressWithPublicKey();

		// Act:
		Address.writeTo(serializer, "address", originalAddress, encoding);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Address address = Address.readFrom(deserializer, "address", encoding);

		// Assert:
		Assert.assertThat(address, IsEqual.equalTo(originalAddress));
		if (isPublicKeyPreserved) {
			Assert.assertThat(address.getPublicKey(), IsEqual.equalTo(originalAddress.getPublicKey()));
		} else {
			Assert.assertThat(address.getPublicKey(), IsNull.nullValue());
		}
	}

	//endregion

	//endregion

	//region toString

	@Test
	public void toStringReturnsEncodedAddress() {
		// Arrange:
		final Address address = Address.fromEncoded("Sigma Gamma");

		// Assert:
		Assert.assertThat(address.toString(), IsEqual.equalTo("SIGMA GAMMA"));
	}

	//endregion

	private static Address addressAsLower(final Address address) {
		return Address.fromEncoded(address.getEncoded().toLowerCase());
	}

	private static Address addressAsUpper(final Address address) {
		return Address.fromEncoded(address.getEncoded().toUpperCase());
	}

	private static Address addressAsMixed(final Address address) {
		return Address.fromEncoded(
				addressAsLower(address).getEncoded().substring(0, 20) +
				addressAsUpper(address).getEncoded().substring(20));
	}
}
