package org.nem.core.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nem.core.utils.ExceptionUtils;

import java.security.*;

/**
 * Static class that exposes hash functions.
 */
public class Hashes {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Performs a SHA3-256 hash of the concatenated inputs.
	 *
	 * @param inputs The byte arrays to concatenate and hash.
	 * @return The hash of the concatenated inputs.
	 * @throws CryptoException if the hash operation failed.
	 */
	public static byte[] sha3_256(final byte[]... inputs) {
		return hash("SHA3-256", inputs);
	}

	/**
	 * Performs a SHA3-512 hash of the concatenated inputs.
	 *
	 * @param inputs The byte arrays to concatenate and hash.
	 * @return The hash of the concatenated inputs.
	 * @throws CryptoException if the hash operation failed.
	 */
	public static byte[] sha3_512(final byte[]... inputs) {
		return hash("SHA3-512", inputs);
	}

	/**
	 * Performs a RIPEMD160 hash of the concatenated inputs.
	 *
	 * @param inputs The byte arrays to concatenate and hash.
	 * @return The hash of the concatenated inputs.
	 * @throws CryptoException if the hash operation failed.
	 */
	public static byte[] ripemd160(final byte[]... inputs) {
		return hash("RIPEMD160", inputs);
	}

	private static byte[] hash(final String algorithm, final byte[]... inputs) {
		return ExceptionUtils.propagate(
				() -> {
					final MessageDigest digest = MessageDigest.getInstance(algorithm, "BC");

					for (final byte[] input : inputs) {
						digest.update(input);
					}

					return digest.digest();
				},
				CryptoException::new);
	}
}
