package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

/**
 * Implementation of the IES cipher for Ed25519.
 */
public class Ed25519IesCipher implements IesCipher {

	public Ed25519IesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		throw new RuntimeException("Not implemented yet.");
	}

	@Override
	public byte[] encrypt(final byte[] input) {
		throw new RuntimeException("Not implemented yet.");
	}

	@Override
	public byte[] decrypt(final byte[] input) {
		throw new RuntimeException("Not implemented yet.");
	}
}
