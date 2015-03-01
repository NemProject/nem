package org.nem.core.crypto;

public class KeyPair {

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	/**
	 * Creates a random key pair.
	 */
	public KeyPair() {
		final KeyGenerator generator = CryptoEngines.defaultEngine().createKeyGenerator();
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
		this(privateKey, CryptoEngines.defaultEngine());
	}

	/**
	 * Creates a key pair around a private key.
	 * The public key is calculated from the private key.
	 *
	 * @param privateKey The private key.
	 * @param engine The crypto engine.
	 */
	public KeyPair(final PrivateKey privateKey, final CryptoEngine engine) {
		this(privateKey, engine.createKeyGenerator().derivePublicKey(privateKey), engine);
	}

	/**
	 * Creates a key pair around a public key.
	 * The private key is empty.
	 *
	 * @param publicKey The public key.
	 */
	public KeyPair(final PublicKey publicKey) {
		this(publicKey, CryptoEngines.defaultEngine());
	}

	/**
	 * Creates a key pair around a public key.
	 * The private key is empty.
	 *
	 * @param publicKey The public key.
	 * @param engine The crypto engine.
	 */
	public KeyPair(final PublicKey publicKey, final CryptoEngine engine) {
		this(null, publicKey, engine);
	}

	private KeyPair(final PrivateKey privateKey, final PublicKey publicKey, final CryptoEngine engine) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;

		if (!engine.createKeyAnalyzer().isKeyCompressed(this.publicKey)) {
			throw new IllegalArgumentException("publicKey must be in compressed form");
		}
	}

	/**
	 * Creates a random key pair that is compatible with the specified engine.
	 *
	 * @param engine The crypto engine.
	 * @return The key pair.
	 */
	public static KeyPair random(final CryptoEngine engine) {
		final KeyPair pair = engine.createKeyGenerator().generateKeyPair();
		return new KeyPair(pair.getPrivateKey(), pair.getPublicKey(), engine);
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
}