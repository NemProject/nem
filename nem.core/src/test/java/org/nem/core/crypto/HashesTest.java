package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.util.Arrays;
import java.util.function.Function;

public class HashesTest {
	private static final HashTester SHA3_256_TESTER = new HashTester(Hashes::sha3_256, 32);
	private static final HashTester SHA3_512_TESTER = new HashTester(Hashes::sha3_512, 64);
	private static final HashTester RIPEMD160_TESTER = new HashTester(Hashes::ripemd160, 20);

	//region sha3_256

	@Test
	public void sha3_256HashHasExpectedByteLength() {
		// Assert:
		SHA3_256_TESTER.assertHashHasExpectedLength();
	}

	@Test
	public void sha3_256GeneratesSameHashForSameInputs() {
		// Assert:
		SHA3_256_TESTER.assertHashIsSameForSameInputs();
	}

	@Test
	public void sha3_256GeneratesSameHashForSameMergedInputs() {
		// Assert:
		SHA3_256_TESTER.assertHashIsSameForSplitInputs();
	}

	@Test
	public void sha3_256GeneratesDifferentHashForDifferentInputs() {
		// Assert:
		SHA3_256_TESTER.assertHashIsDifferentForDifferentInputs();
	}

	//endregion

	//region sha3_512

	@Test
	public void sha3_512HashHasExpectedByteLength() {
		// Assert:
		SHA3_512_TESTER.assertHashHasExpectedLength();
	}

	@Test
	public void sha3_512GeneratesSameHashForSameInputs() {
		// Assert:
		SHA3_512_TESTER.assertHashIsSameForSameInputs();
	}

	@Test
	public void sha3_512GeneratesSameHashForSameMergedInputs() {
		// Assert:
		SHA3_512_TESTER.assertHashIsSameForSplitInputs();
	}

	@Test
	public void sha3_512GeneratesDifferentHashForDifferentInputs() {
		// Assert:
		SHA3_512_TESTER.assertHashIsDifferentForDifferentInputs();
	}

	//endregion

	//region ripemd160

	@Test
	public void ripemd160HashHasExpectedByteLength() {
		// Assert:
		RIPEMD160_TESTER.assertHashHasExpectedLength();
	}

	@Test
	public void ripemd160GeneratesSameHashForSameInputs() {
		// Assert:
		RIPEMD160_TESTER.assertHashIsSameForSameInputs();
	}

	@Test
	public void ripemd160GeneratesSameHashForSameMergedInputs() {
		// Assert:
		RIPEMD160_TESTER.assertHashIsSameForSplitInputs();
	}

	@Test
	public void ripemd160GeneratesDifferentHashForDifferentInputs() {
		// Assert:
		RIPEMD160_TESTER.assertHashIsDifferentForDifferentInputs();
	}

	//endregion

	//region different hash algorithm

	@Test
	public void sha3_256AndRipemd160GenerateDifferentHashForSameInputs() {
		// Assert:
		assertHashesAreDifferent(Hashes::sha3_256, Hashes::ripemd160);
	}

	@Test
	public void sha3_256AndSha3_512GenerateDifferentHashForSameInputs() {
		// Assert:
		assertHashesAreDifferent(Hashes::sha3_256, Hashes::sha3_512);
	}

	@Test
	public void sha3_512AndRipemd160GenerateDifferentHashForSameInputs() {
		// Assert:
		assertHashesAreDifferent(Hashes::sha3_512, Hashes::ripemd160);
	}

	private static void assertHashesAreDifferent(
			final Function<byte[], byte[]> hashFunction1,
			final Function<byte[], byte[]> hashFunction2) {

		// Arrange:
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] hash1 = hashFunction1.apply(input);
		final byte[] hash2 = hashFunction2.apply(input);

		// Assert:
		Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
	}

	//endregion

	private static class HashTester {
		private final Function<byte[], byte[]> hashFunction;
		private final Function<byte[][], byte[]> hashMultipleFunction;
		private final int expectedHashLength;

		public HashTester(final Function<byte[][], byte[]> hashMultipleFunction, final int expectedHashLength) {
			this.hashMultipleFunction = hashMultipleFunction;
			this.hashFunction = input -> hashMultipleFunction.apply(new byte[][] { input });
			this.expectedHashLength = expectedHashLength;
		}

		public void assertHashHasExpectedLength() {
			// Arrange:
			final byte[] input = Utils.generateRandomBytes();

			// Act:
			final byte[] hash = this.hashFunction.apply(input);

			// Assert:
			Assert.assertThat(hash.length, IsEqual.equalTo(this.expectedHashLength));
		}

		public void assertHashIsSameForSameInputs() {
			// Arrange:
			final byte[] input = Utils.generateRandomBytes();

			// Act:
			final byte[] hash1 = this.hashFunction.apply(input);
			final byte[] hash2 = this.hashFunction.apply(input);

			// Assert:
			Assert.assertThat(hash2, IsEqual.equalTo(hash1));
		}

		public void assertHashIsSameForSplitInputs() {
			// Arrange:
			final byte[] input = Utils.generateRandomBytes();

			// Act:
			final byte[] hash1 = this.hashFunction.apply(input);
			final byte[] hash2 = this.hashMultipleFunction.apply(split(input));

			// Assert:
			Assert.assertThat(hash2, IsEqual.equalTo(hash1));
		}

		public void assertHashIsDifferentForDifferentInputs() {
			// Arrange:
			final byte[] input1 = Utils.generateRandomBytes();
			final byte[] input2 = Utils.generateRandomBytes();

			// Act:
			final byte[] hash1 = this.hashFunction.apply(input1);
			final byte[] hash2 = this.hashFunction.apply(input2);

			// Assert:
			Assert.assertThat(hash2, IsNot.not(IsEqual.equalTo(hash1)));
		}

		private static byte[][] split(final byte[] input) {
			return new byte[][] {
					Arrays.copyOfRange(input, 0, 17),
					Arrays.copyOfRange(input, 17, 100),
					Arrays.copyOfRange(input, 100, input.length)
			};
		}
	}
}