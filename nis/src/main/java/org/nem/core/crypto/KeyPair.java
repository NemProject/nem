package org.nem.core.crypto;

public class KeyPair {

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	/**
	 * Creates a random key pair.
	 */
	public KeyPair() {
		final KeyGenerator generator = CryptoEngines.getDefaultEngine().createKeyGenerator();
		final KeyPair pair = generator.generateKeyPair();
		this.privateKey = pair.getPrivateKey();
		this.publicKey = pair.getPublicKey();
	}

	/**
	 * Creates a key pair around a private key.
	 * The public key is calculated from the private key.
	 *
	 * @param privateKey The private key.
	 */
	public KeyPair(final PrivateKey privateKey) {
		this(privateKey, CryptoEngines.getDefaultEngine().createKeyGenerator().derivePublicKey(privateKey));
	}

	/**
	 * Creates a key pair around a public key.
	 * The private key is empty.
	 *
	 * @param publicKey The public key.
	 */
	public KeyPair(final PublicKey publicKey) {
		this(null, publicKey);
	}

	public KeyPair(final PrivateKey privateKey, final PublicKey publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;

		if (!publicKey.isCompressed()) {
			throw new IllegalArgumentException("publicKey must be in compressed form");
		}
	}

	/**
	 * Gets the private key.
	 *
	 * @return The private key.
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * Gets the public key.
	 *
	 * @return the public key.
	 */
	public PublicKey getPublicKey() {
		return this.publicKey;
	}

	/**
	 * Determines if the current key pair has a private key.
	 *
	 * @return true if the current key pair has a private key.
	 */
	public boolean hasPrivateKey() {
		return null != this.privateKey;
	}

	/**
	 * Determines if the current key pair has a public key.
	 * TODO 20141010 J-B: we should probably remove this, since public key can't really ever be null.
	 *
	 * @return true if the current key pair has a public key.
	 */
	public boolean hasPublicKey() {
		return null != this.publicKey;
	}
}