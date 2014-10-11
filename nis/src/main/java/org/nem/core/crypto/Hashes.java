package org.nem.core.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nem.core.utils.ExceptionUtils;

import java.security.*;
import java.util.logging.Logger;

/**
 * Static class that exposes hash functions.
 */
public class Hashes {
	private static final Logger LOGGER = Logger.getLogger(Hashes.class.getName());

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
	public static byte[] sha3(final byte[]... inputs) {
		return hash("SHA3-256", inputs);
	}

	// TODO 20141010 J-B: should we just replace sha3 with this?
	// > i would also prefer to hide the message digest in this class
	// > (like the other overloads)

	/**
	 * Gets an instance of a SHA3-512 message digest.
	 *
	 * @return The SHA3-512 instance.
	 */
	public static MessageDigest getSha3_512Instance() {
		return ExceptionUtils.propagate(
				() -> MessageDigest.getInstance("SHA3-512", "BC"),
				CryptoException::new);
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
