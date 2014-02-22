package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.util.Arrays;

public class HashesTest {

    @Test
    public void sha3HashHas32ByteLength() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash = Hashes.sha3(input);

        // Assert:
        Assert.assertThat(hash.length, IsEqual.equalTo(32));
    }

    @Test
    public void sha3GeneratesSameHashForSameInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.sha3(input);
        byte[] hash2 = Hashes.sha3(input);

        // Assert:
        Assert.assertThat(hash1.length, IsEqual.equalTo(32));
        Assert.assertThat(hash2, IsEqual.equalTo(hash1));
    }

    @Test
    public void sha3GeneratesSameHashForSameMergedInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.sha3(input);
        byte[] hash2 = Hashes.sha3(split(input));

        // Assert:
        Assert.assertThat(hash2, IsEqual.equalTo(hash1));
    }

    @Test
    public void sha3GeneratesDifferentHashForDifferentInputs() throws Exception {
        // Arrange:
        byte[] input1 = Utils.generateRandomBytes();
        byte[] input2 = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.sha3(input1);
        byte[] hash2 = Hashes.sha3(input2);

        // Assert:
        Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
    }

    @Test
    public void ripemd160HashHas20ByteLength() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash = Hashes.ripemd160(input);

        // Assert:
        Assert.assertThat(hash.length, IsEqual.equalTo(20));
    }

    @Test
    public void ripemd160GeneratesSameHashForSameInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.ripemd160(input);
        byte[] hash2 = Hashes.ripemd160(input);

        // Assert:
        Assert.assertThat(hash2, IsEqual.equalTo(hash1));
    }

    @Test
    public void ripemd160GeneratesSameHashForSameMergedInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.ripemd160(input);
        byte[] hash2 = Hashes.ripemd160(split(input));

        // Assert:
        Assert.assertThat(hash2, IsEqual.equalTo(hash1));
    }

    @Test
    public void ripemd160GeneratesDifferentHashForDifferentInputs() throws Exception {
        // Arrange:
        byte[] input1 = Utils.generateRandomBytes();
        byte[] input2 = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.ripemd160(input1);
        byte[] hash2 = Hashes.ripemd160(input2);

        // Assert:
        Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
    }

    @Test
    public void sha3AndRipemd160GenerateDifferentHashForSameInputs() throws Exception {
        // Arrange:
        byte[] input = Utils.generateRandomBytes();

        // Act:
        byte[] hash1 = Hashes.sha3(input);
        byte[] hash2 = Hashes.ripemd160(input);

        // Assert:
        Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
    }

    private byte[][] split(final byte[] input) {
        return new byte[][] {
            Arrays.copyOfRange(input, 0, 17),
            Arrays.copyOfRange(input, 17, 100),
            Arrays.copyOfRange(input, 100, input.length)
        };
    }
}
