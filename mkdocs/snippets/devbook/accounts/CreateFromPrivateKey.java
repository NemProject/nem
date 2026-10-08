//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.KeyPair;

public final class CreateFromPrivateKey {
	// Initialize the facade for the testnet network [>step-1]
	private final NemFacade facade = new NemFacade("testnet");
	// [<step-1]

	public static void main(final String[] args) {
		new CreateFromPrivateKey().run();
	}

	private void run() {
		// Use an existing private key if provided, [>step-2]
		// Otherwise generate a random one.
		final String privateKeyString = System.getenv("PRIVATE_KEY");
		final CryptoTypes.PrivateKey privateKey;
		if (null != privateKeyString) {
			System.out.println(
				"Loading account from environment variable...");
			privateKey = new CryptoTypes.PrivateKey(privateKeyString);
		} else {
			System.out.println("Generating random account...");
			privateKey = CryptoTypes.PrivateKey.random();
		} // [<step-2]

		// Create a key pair from the private key [>step-3]
		final KeyPair keyPair = new KeyPair(privateKey);

		// Derive the public key from the private key
		final CryptoTypes.PublicKey publicKey = keyPair.getPublicKey();

		// Derive the address from the public key
		final Address address = facade.network.publicKeyToAddress(
			publicKey);

		// Output the account details
		System.out.printf("Address: %s%n", address);
		System.out.printf("Public key: %s%n", publicKey);
		System.out.printf("Private key: %s%n", privateKey); // [<step-3]
	}
}
