package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public class AddressTest {

    @Test
    public void sameAddressIsGeneratedForSameInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address1 = Address.fromPublicKey(input);
        String address2 = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address2, IsEqual.equalTo(address1));
    }

    @Test
    public void differentAddressesAreGeneratedForDifferentInputs() throws Exception {

        // Arrange:
        byte[] input1 = Utils.generateRandomBytes();
        byte[] input2 = Utils.generateRandomBytes();

        // Act:
        String address1 = Address.fromPublicKey(input1);
        String address2 = Address.fromPublicKey(input2);

        // Assert:
        Assert.assertThat(address2, IsNot.not(IsEqual.equalTo(address1)));
    }

    @Test
    public void generatedAddressIsValid() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(Address.isValid(address), IsEqual.equalTo(true));
    }

    @Test
    public void generatedAddressHas40CharLength() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.length(), IsEqual.equalTo(40));
    }

    @Test
    public void generatedAddressBeginsWithN() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address = Address.fromPublicKey(input);

        // Assert:
        Assert.assertThat(address.charAt(0), IsEqual.equalTo('N'));
    }

    @Test
    public void addressWithIncorrectLengthIsNotValid() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address = Address.fromPublicKey(input);
        address = address.substring(0, address.length() - 1);

        // Assert:
        Assert.assertThat(Address.isValid(address), IsEqual.equalTo(false));
    }

    @Test
    public void addressWithIncorrectVersionIsNotValid() throws Exception {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(0);
    }

    @Test
    public void addressWithIncorrectHashIsNotValid() throws Exception {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(5);
    }

    @Test
    public void addressWithIncorrectChecksumIsNotValid() throws Exception {
        // Assert:
        assertAddressIsNotValidIfChangedAtIndex(39);
    }

    private void assertAddressIsNotValidIfChangedAtIndex(final int index) throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        String address = Address.fromPublicKey(input);
        address = incrementCharAtIndex(address, index);

        // Assert:
        Assert.assertThat(Address.isValid(address), IsEqual.equalTo(false));
    }

    private static String incrementCharAtIndex(final String s, final int index) {
        char[] chars = s.toCharArray();
        chars[index] = (char)(chars[index] + 1);
        return new String(chars);
    }
}
