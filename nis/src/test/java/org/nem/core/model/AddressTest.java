package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class AddressTest {

    @Test
    public void addressCanBeCreatedAroundEncodedAddress() {
        // Act:
        Address address = Address.fromEncoded("Sigma Gamma");

        // Assert:
        Assert.assertThat(address.getEncoded(), IsEqual.equalTo("Sigma Gamma"));
    }

    @Test
    public void addressCanBeCreatedAroundPublicKey() {
        // Act:
        Address address = Address.fromPublicKey(Utils.generateRandomBytes());

        // Assert:
        Assert.assertThat(address.getEncoded(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(address.getEncoded().length(), IsEqual.equalTo(40));
    }


    @Test
    public void sameAddressIsGeneratedForSameInputs() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address1 = Address.fromPublicKey(input);
        Address address2 = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address2, IsEqual.equalTo(address1));
    }

    @Test
    public void differentAddressesAreGeneratedForDifferentInputs() {

        // Arrange:
        byte[] input1 = Utils.generateRandomBytes();
        byte[] input2 = Utils.generateRandomBytes();

        // Act:
        Address address1 = Address.fromPublicKey(input1);
        Address address2 = Address.fromPublicKey(input2);

        // Assert:
        Assert.assertThat(address2, IsNot.not(IsEqual.equalTo(address1)));
    }

    @Test
    public void generatedAddressIsValid() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.isValid(), IsEqual.equalTo(true));
    }

    @Test
    public void generatedAddressHas40CharLength() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.getEncoded().length(), IsEqual.equalTo(40));
    }

    @Test
    public void generatedAddressBeginsWithN() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.getEncoded().charAt(0), IsEqual.equalTo('N'));
    }

    @Test
    public void addressWithIncorrectLengthIsNotValid() {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);
        String realAddress = address.getEncoded();
        String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

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
        byte[] input = Utils.generateRandomBytes();

        // Act:
        Address address = Address.fromPublicKey(input);
        String fakeAddress = Utils.incrementAtIndex(address.getEncoded(), index);

        // Assert:
        Assert.assertThat(Address.fromEncoded(fakeAddress).isValid(), IsEqual.equalTo(false));
    }

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        byte[] publicKey = Utils.generateRandomBytes();
        Address address = Address.fromPublicKey(publicKey);

        // Assert:
        Assert.assertThat(Address.fromPublicKey(publicKey), IsEqual.equalTo(address));
        Assert.assertThat(Address.fromEncoded(address.getEncoded()), IsEqual.equalTo(address));
        Assert.assertThat(Address.fromPublicKey(Utils.incrementAtIndex(publicKey, 12)), IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(Address.fromEncoded(Utils.incrementAtIndex(address.getEncoded(), 0)), IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)address)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        byte[] publicKey = Utils.generateRandomBytes();
        Address address = Address.fromPublicKey(publicKey);
        int hashCode = address.hashCode();

        // Assert:
        Assert.assertThat(Address.fromPublicKey(publicKey).hashCode(), IsEqual.equalTo(hashCode));
        Assert.assertThat(Address.fromEncoded(address.getEncoded()).hashCode(), IsEqual.equalTo(hashCode));
        Assert.assertThat(Address.fromPublicKey(Utils.incrementAtIndex(publicKey, 12)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(Address.fromEncoded(Utils.incrementAtIndex(address.getEncoded(), 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
    }

    //endregion
}
