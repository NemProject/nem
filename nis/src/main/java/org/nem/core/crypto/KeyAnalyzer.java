package org.nem.core.crypto;

/**
 * Interface to analyze keys.
 */
public interface KeyAnalyzer {

	/**
	 * Gets a value indication whether or not hte public key is compressed.
	 *
	 * @param publicKey The public key.
	 * @return true if the public key is compressed, false otherwise.
	 */
	public boolean isKeyCompressed(final PublicKey publicKey);
}
