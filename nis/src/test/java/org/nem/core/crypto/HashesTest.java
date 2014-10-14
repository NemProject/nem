package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.util.Arrays;

public class HashesTest {

	// TODO 20141010 J-J refactor some of these tests

	//region sha3_256

	@Test
	public void sha3_256HashHas32ByteLength() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash = Hashes.sha3_256(input);

		// Assert:
		Assert.assertThat(hash.length, IsEqual.equalTo(32));
	}

	@Test
	public void sha3_256GeneratesSameHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_256(input);
		final byte[] hash2 = Hashes.sha3_256(input);

		// Assert:
		Assert.assertThat(hash1.length, IsEqual.equalTo(32));
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3_256GeneratesSameHashForSameMergedInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_256(input);
		final byte[] hash2 = Hashes.sha3_256(this.split(input));

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3_256GeneratesDifferentHashForDifferentInputs() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_256(input1);
		final byte[] hash2 = Hashes.sha3_256(input2);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	//endregion

	//region sha3_512

	@Test
	public void sha3_512HashHas64ByteLength() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash = Hashes.sha3_512(input);

		// Assert:
		Assert.assertThat(hash.length, IsEqual.equalTo(64));
	}

	@Test
	public void sha3_512GeneratesSameHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_512(input);
		final byte[] hash2 = Hashes.sha3_512(input);

		// Assert:
		Assert.assertThat(hash1.length, IsEqual.equalTo(64));
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3_512GeneratesSameHashForSameMergedInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_512(input);
		final byte[] hash2 = Hashes.sha3_512(this.split(input));

		// Assert:
		Assert.assertThat(hash2, IsEqual.equalTo(hash1));
	}

	@Test
	public void sha3_512GeneratesDifferentHashForDifferentInputs() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_512(input1);
		final byte[] hash2 = Hashes.sha3_512(input2);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	//endregion

	//region ripemd160

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

	//endregion

	//region different hash algorithm

	@Test
	public void sha3_256AndRipemd160GenerateDifferentHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_256(input);
		final byte[] hash2 = Hashes.ripemd160(input);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	@Test
	public void sha3_256AndSha3_512GenerateDifferentHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_256(input);
		final byte[] hash2 = Hashes.sha3_512(input);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	@Test
	public void sha3_512AndRipemd160GenerateDifferentHashForSameInputs() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = Hashes.sha3_512(input);
		final byte[] hash2 = Hashes.ripemd160(input);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	//endregion

	private byte[][] split(final byte[] input) {
		return new byte[][] {
				Arrays.copyOfRange(input, 0, 17),
				Arrays.copyOfRange(input, 17, 100),
				Arrays.copyOfRange(input, 100, input.length)
		};
	}
}
