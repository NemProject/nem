package org.nem.core.crypto;

/**
 * Wraps DSA signing and verification logic.
 */
public class Signer implements DsaSigner {
	private final DsaSigner signer;

	/**
	 * Creates a signer around a KeyPair.
	 *
	 * @param keyPair The KeyPair that should be used for signing and verification.
	 */
	public Signer(final KeyPair keyPair) {
		this(keyPair, CryptoEngines.defaultEngine());
	}

	/**
	 * Creates a signer around a KeyPair.
	 *
	 * @param keyPair The KeyPair that should be used for signing and verification.
	 * @param engine The crypto engine.
	 */
	public Signer(final KeyPair keyPair, final CryptoEngine engine) {
		this(engine.createDsaSigner(keyPair));
	}

	/**
	 * Creates a signer around a DsaSigner.
	 *
	 * @param signer The signer.
	 */
	public Signer(final DsaSigner signer) {
		this.signer = signer;
	}

	@Override
	public Signature sign(final byte[] data) {
		return this.signer.sign(data);
	}

	@Override
	public boolean verify(final byte[] data, final Signature signature) {
		return this.signer.verify(data, signature);
	}

	@Override
	public boolean isCanonicalSignature(final Signature signature) {
		return this.signer.isCanonicalSignature(signature);
	}

	@Override
	public Signature makeSignatureCanonical(final Signature signature) {
		return this.signer.makeSignatureCanonical(signature);
	}
}
