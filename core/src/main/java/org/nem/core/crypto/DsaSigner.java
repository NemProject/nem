package org.nem.core.crypto;

/**
 * Interface that supports signing and verification of arbitrarily sized message.
 */
public interface DsaSigner {

	/**
	 * Signs the SHA3 hash of an arbitrarily sized message.
	 *
	 * @param data The message to sign.
	 * @return The generated signature.
	 */
	Signature sign(final byte[] data);

	/**
	 * Verifies that the signature is valid.
	 *
	 * @param data The original message.
	 * @param signature The generated signature.
	 * @return true if the signature is valid.
	 */
	boolean verify(final byte[] data, final Signature signature);

	/**
	 * Determines if the signature is canonical.
	 *
	 * @param signature The signature.
	 * @return true if the signature is canonical.
	 */
	boolean isCanonicalSignature(final Signature signature);

	/**
	 * Makes this signature canonical.
	 *
	 * @param signature The signature.
	 * @return Signature in canonical form.
	 */
	Signature makeSignatureCanonical(final Signature signature);
}
