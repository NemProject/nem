package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;

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
	public void addressPaddedWithSpacesIsInvalid() {
		// Arrange:
		final Address address = Address.fromEncoded("TD5AXB37QG5DXD25YHLMS4VDMI3A7HBGBYHDNB63 ");

		// Assert:
		Assert.assertThat(address.isValid(), IsEqual.equalTo(false));
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
		// Act:
		final Address address = Address.fromEncoded("A*B");

		// Assert:
		Assert.assertThat(address.isValid(), IsEqual.equalTo(false));
	}

	@Test
	public void addressWithIncorrectLengthIsNotValid() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final Address address = Address.fromPublicKey(publicKey);
		final String realAddress = address.getEncoded();
		final String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

		// Assert:
		Assert.assertThat(Address.fromEncoded(realAddress).isValid(), IsEqual.equalTo(true));
		Assert.assertThat(Address.fromEncoded(fakeAddress).isValid(), IsEqual.equalTo(false));
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
	public void canWriteAddress() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		Address.writeTo(serializer, "Address", address);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Address"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canRoundtripAddress() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address originalAddress = Address.fromEncoded("MockAcc");

		// Act:
		Address.writeTo(serializer, "Address", originalAddress);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Address address = Address.readFrom(deserializer, "Address");

		// Assert:
		Assert.assertThat(address, IsEqual.equalTo(originalAddress));
	}

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
