//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import org.symbol.sdk.Bip32;
import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.KeyPair;

public final class CreateFromMnemonic {
	// Initialize the facade for the testnet network [>step-1]
	private final NemFacade facade = new NemFacade("testnet");
	// [<step-1]

	public static void main(final String[] args) {
		new CreateFromMnemonic().run();
	}

	private void run() {
		// Use an existing mnemonic if provided, [>step-2]
		// otherwise generate a random one.
		final Bip32 bip32 = new Bip32(NemFacade.BIP32_CURVE_NAME);
		String mnemonic = System.getenv("MNEMONIC");
		if (null != mnemonic) {
			System.out.println(
				"Loading mnemonic phrase from environment variable...");
		} else {
			System.out.println("Generating random mnemonic phrase...");
			mnemonic = bip32.random();
		}
		System.out.printf("Mnemonic phrase: %s%n", mnemonic); // [<step-2]

		// Load password from environment variable or use default [>step-3]
		final String password = System.getenv().getOrDefault(
			"PASSWORD", "correcthorsebatterystaple");
		System.out.printf("Password: %s%n", password);

		// Derive a root Bip32 node from the mnemonic and a password
		final Bip32.Bip32Node rootNode = bip32.fromMnemonic(
			mnemonic, password);
		// [<step-3]
		// Derive a child Bip32 node for the account at index 0 [>step-4]
		final int accountIndex = 0;
		final Bip32.Bip32Node childNode = rootNode.derivePath(
			facade.bip32Path(accountIndex));
		// [<step-4]
		// Convert the Bip32 node to a signing key pair [>step-5]
		final KeyPair keyPair = NemFacade.bip32NodeToKeyPair(childNode);

		// Derive the address from the public key
		final CryptoTypes.PublicKey publicKey = keyPair.getPublicKey();
		final Address address = facade.network.publicKeyToAddress(
			publicKey);

		// Output the account details
		System.out.printf("Address: %s%n", address);
		System.out.printf("Public key: %s%n", publicKey);
		System.out.printf("Private key: %s%n",
			keyPair.getPrivateKey()); // [<step-5]
	}
}
