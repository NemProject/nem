package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.utils.Base32Encoder;

import java.math.BigInteger;
import java.util.*;
import java.util.function.*;

public class AddressTest {

	//region construction

	@Test
	public void addressCanBeCreatedAroundEncodedAddress() {
		// Act:
		final Address address = Address.fromEncoded("Sigma Gamma");

		// Assert:
		Assert.assertThat(address.getEncoded(), IsEqual.equalTo("SIGMA GAMMA"));
		Assert.assertThat(address.getPublicKey(), IsNull.nullValue());
		Assert.assertThat(address.getVersion(), IsEqual.equalTo((byte)0x92));
	}

	@Test
	public void addressCannotBeCreatedAroundNullEncodedAddress() {
		// Act:
		ExceptionAssert.assertThrows(v -> Address.fromEncoded(null), IllegalArgumentException.class);
	}

	@Test
	public void addressCanBeCreatedAroundPublicKey() {
		// Act:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey(publicKey);

		// Assert:
		Assert.assertThat(address.getEncoded(), IsNull.notNullValue());
		Assert.assertThat(address.getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(address.getVersion(), IsEqual.equalTo(NetworkInfos.getDefault().getVersion()));
	}

	@Test
	public void addressCanBeCreatedAroundPublicKeyAndCustomVersion() {
		// Act:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final Address address = Address.fromPublicKey((byte)0x88, publicKey);

		// Assert:
		Assert.assertThat(Base32Encoder.getBytes(address.getEncoded())[0], IsEqual.equalTo((byte)0x88));
		Assert.assertThat(address.getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(address.getVersion(), IsEqual.equalTo((byte)0x88));
	}

	@Test
	public void addressCannotBeCreatedAroundNullPublicKey() {
		// Act:
		ExceptionAssert.assertThrows(v -> Address.fromPublicKey(null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> Address.fromPublicKey((byte)0x88, null), IllegalArgumentException.class);
	}

	//endregion

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
		Assert.assertThat(address.getEncoded().charAt(0), IsEqual.equalTo(NetworkInfos.getDefault().getAddressStartChar()));
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
		this.assertAddressIsNotValidIfChangedAtIndex(0);
	}

	@Test
	public void addressWithIncorrectHashIsNotValid() {
		// Assert:
		this.assertAddressIsNotValidIfChangedAtIndex(5);
	}

	@Test
	public void addressWithIncorrectChecksumIsNotValid() {
		// Assert:
		this.assertAddressIsNotValidIfChangedAtIndex(39);
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

	@Test
	public void addressWithLeadingWhitespaceIsInvalid() {
		// Assert:
		assertAddressWithPaddingInvalid((address, padding) -> padding + address.toString());
	}

	@Test
	public void addressWithTrailingWhitespaceIsInvalid() {
		// Assert:
		assertAddressWithPaddingInvalid((address, padding) -> address.toString() + padding);
	}

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

	//region compareTo

	@Test
	public void compareToReturnsExpectedResult() {
		// Arrange:
		final List<Address> addresses = new ArrayList<>();
		addresses.add(Address.fromEncoded("A"));
		addresses.add(Address.fromEncoded("B"));
		addresses.add(Address.fromEncoded("C"));
		addresses.add(Address.fromEncoded("D"));
		addresses.add(Address.fromEncoded("E"));
		addresses.add(Address.fromEncoded("F"));

		// Assert:
		for (int i = 0; i < addresses.size(); i++) {
			for (int j = 0; j < addresses.size(); j++) {
				Assert.assertThat(addresses.get(i).compareTo(addresses.get(j)) > 0, IsEqual.equalTo(i > j));
				Assert.assertThat(addresses.get(i).compareTo(addresses.get(j)) == 0, IsEqual.equalTo(i == j));
				Assert.assertThat(addresses.get(i).compareTo(addresses.get(j)) < 0, IsEqual.equalTo(i < j));
			}
		}
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
	public void canRoundtripAddressWithCompressedEncoding() {
		// Assert:
		this.assertAddressRoundTripInMode(AddressEncoding.COMPRESSED, false, true);
		this.assertAddressRoundTripInMode(AddressEncoding.COMPRESSED, false, false);
	}

	@Test
	public void canRoundtripAddressWithPublicKeyEncoding() {
		// Assert:
		this.assertAddressRoundTripInMode(AddressEncoding.PUBLIC_KEY, true, true);
		this.assertAddressRoundTripInMode(AddressEncoding.PUBLIC_KEY, true, false);
	}

	@Test(expected = MissingRequiredPropertyException.class)
	public void cannotReadNullAddressWithCompressedEncoding() {
		// Act:
		Address.readFrom(createEmptyDeserializer(), "address", AddressEncoding.COMPRESSED);
	}

	@Test(expected = MissingRequiredPropertyException.class)
	public void cannotReadNullAddressWithPublicKeyEncoding() {
		// Act:
		Address.readFrom(createEmptyDeserializer(), "address", AddressEncoding.PUBLIC_KEY);
	}

	@Test
	public void canReadNullAddressWithOptionalCompressedEncoding() {
		// Act:
		final Address address = Address.readFromOptional(createEmptyDeserializer(), "address", AddressEncoding.COMPRESSED);

		// Assert:
		Assert.assertThat(address, IsNull.nullValue());
	}

	@Test
	public void canReadNullAddressWithOptionalPublicKeyEncoding() {
		// Act:
		final Address address = Address.readFromOptional(createEmptyDeserializer(), "address", AddressEncoding.PUBLIC_KEY);

		// Assert:
		Assert.assertThat(address, IsNull.nullValue());
	}

	private static Deserializer createEmptyDeserializer() {
		return Utils.createDeserializer(new JsonSerializer().getObject());
	}

	private void assertAddressRoundTripInMode(
			final AddressEncoding encoding,
			final boolean isPublicKeyPreserved,
			final boolean useOptionalReadFrom) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address originalAddress = Utils.generateRandomAddressWithPublicKey();

		// Act:
		Address.writeTo(serializer, "address", originalAddress, encoding);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Address address = useOptionalReadFrom
				? Address.readFromOptional(deserializer, "address", encoding)
				: Address.readFrom(deserializer, "address", encoding);

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
