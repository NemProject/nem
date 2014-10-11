package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.util.Arrays;

public class HashesTest {

	@Test
	public void sha3HashHas32ByteLength() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash = Hashes.sha3(input);

		// Assert:
		Assert.assertThat(hash.length, IsEqual.equalTo(32));
	}

	@Test
	public void sha3GeneratesSameHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3(input);
		final byte[] hash2 = Hashes.sha3(input);

		// Assert:
		Assert.assertThat(hash1.length, IsEqual.equalTo(32));
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3GeneratesSameHashForSameMergedInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3(input);
		final byte[] hash2 = Hashes.sha3(this.split(input));

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3_512InstanceCanBeCreated() {
		// Assert:
		Hashes.getSha3_512Instance();
	}

	@Test
	public void sha3GeneratesDifferentHashForDifferentInputs() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3(input1);
		final byte[] hash2 = Hashes.sha3(input2);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	@Test
	public void ripemd160HashHas20ByteLength() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash = Hashes.ripemd160(input);

		// Assert:
		Assert.assertThat(hash.length, IsEqual.equalTo(20));
	}

	@Test
	public void ripemd160GeneratesSameHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.ripemd160(input);
		final byte[] hash2 = Hashes.ripemd160(input);

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void ripemd160GeneratesSameHashForSameMergedInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.ripemd160(input);
		final byte[] hash2 = Hashes.ripemd160(this.split(input));

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void ripemd160GeneratesDifferentHashForDifferentInputs() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.ripemd160(input1);
		final byte[] hash2 = Hashes.ripemd160(input2);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	@Test
	public void sha3AndRipemd160GenerateDifferentHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3(input);
		final byte[] hash2 = Hashes.ripemd160(input);

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
