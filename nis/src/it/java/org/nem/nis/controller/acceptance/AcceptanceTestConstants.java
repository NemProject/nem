package org.nem.nis.controller.acceptance;

import org.nem.core.crypto.*;
import org.nem.core.model.*;

/**
 * Constants used by the acceptance tests.
 */
public class AcceptanceTestConstants {

	/**
	 * The test account address.
	 */
	private static final Address ADDRESS = Address.fromEncoded("TBsanAAURPWQDBZQSSQ5ZP3NYINUBSQY4RKYI6I7");

	/**
	 * The test account public key.
	 */
	public static final PublicKey PUBLIC_KEY = PublicKey.fromHexString("4a25bf65646c8c3df87179bcb5ff7c4c3176ea02153a82d14615d53cf448ed83");

	/**
	 * The test account private key.
	 */
	public static final PrivateKey PRIVATE_KEY = PrivateKey.fromHexString("4d08c419bbc0191bc750f74a6912910b71d741eb621641d29705058df157804a");

	/**
	 * The second test account address.
	 */
	public static final Address ADDRESS2 = Address.fromEncoded("TAFMINM2IMWQ3WXJLHgogogoBB7C46EQ3UfireOA");

	static {
		final PublicKey expectedPublicKey = CryptoEngines.defaultEngine().createKeyGenerator().derivePublicKey(PRIVATE_KEY);
		if (!PUBLIC_KEY.equals(expectedPublicKey)) {
			throw new IllegalStateException("cannot initialize constants - public key does not match private key");
		}

		if (!ADDRESS.equals(Address.fromPublicKey(PUBLIC_KEY))) {
			throw new IllegalStateException("cannot initialize constants - public key does not match address");
		}

		if (!NetworkInfos.getTestNetworkInfo().isCompatible(ADDRESS)) {
			throw new IllegalStateException("cannot initialize constants - account information is only valid for test network");
		}
	}
}
