package org.nem.core.crypto;

public class KeyPair {

	//private final static SecureRandom RANDOM = new SecureRandom();

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
		/*
		final ECKeyPairGenerator generator = new ECKeyPairGenerator();
		final ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(Curves.secp256k1().getParams(), RANDOM);
		generator.init(keyGenParams);

		final AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
		final ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters)keyPair.getPrivate();
		final ECPublicKeyParameters publicKeyParams = (ECPublicKeyParameters)keyPair.getPublic();
		this.privateKey = new PrivateKey(privateKeyParams.getD());

		final ECPoint point = publicKeyParams.getQ();
		this.publicKey = new PublicKey(point.getEncoded(true));*/
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

	private KeyPair(final PrivateKey privateKey, final PublicKey publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;

		if (!publicKey.isCompressed()) {
			throw new IllegalArgumentException("publicKey must be in compressed form");
		}
	}

	/*private static PublicKey publicKeyFromPrivateKey(final PrivateKey privateKey) {
		final ECPoint point = Curves.secp256k1().getParams().getG().multiply(privateKey.getRaw());
		return new PublicKey(point.getEncoded(true));
	}*/

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
	 *
	 * @return true if the current key pair has a public key.
	 */
	public boolean hasPublicKey() {
		return null != this.publicKey;
	}

	/**
	 * Gets the EC private key parameters.
	 *
	 * @return The EC private key parameters.
	 */
	/*public ECPrivateKeyParameters getPrivateKeyParameters() {
		return new ECPrivateKeyParameters(this.getPrivateKey().getRaw(), Curves.secp256k1().getParams());
	}*/

	/**
	 * Gets the EC public key parameters.
	 *
	 * @return The EC public key parameters.
	 */
	/*public ECPublicKeyParameters getPublicKeyParameters() {
		final ECPoint point = Curves.secp256k1().getParams().getCurve().decodePoint(this.getPublicKey().getRaw());
		return new ECPublicKeyParameters(point, Curves.secp256k1().getParams());
	}*/
}