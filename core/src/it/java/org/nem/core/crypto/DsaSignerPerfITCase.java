package org.nem.core.crypto;

import org.junit.*;

public class DsaSignerPerfITCase {

	@Test
	public void ed25519EngineVerifyHasExpectedSpeed() {
		// Assert (should be less than 550 micro seconds per verification on a decent computer):
		assertVerifySpeed(CryptoEngines.ed25519Engine(), 3000, 10000, 550);
	}

	@Test
	public void secp256k1EngineVerifyHasExpectedSpeed() {
		// Assert (should be less than 5000 micro seconds per verification on a decent computer):
		assertVerifySpeed(CryptoEngines.secp256k1Engine(), 500, 500, 5000);
	}

	private static void assertVerifySpeed(
			final CryptoEngine engine,
			final int numWarmUpIterations,
			final int numTimedIterations,
			final int threshold) {
		// Arrange:
		final KeyPair keyPair = KeyPair.random(engine);
		final DsaSigner dsaSigner = engine.createDsaSigner(keyPair);
		final byte[] input = org.nem.core.test.Utils.generateRandomBytes();
		final Signature signature = dsaSigner.sign(input);

		// Warm up
		for (int i = 0; i < numWarmUpIterations; i++) {
			dsaSigner.verify(input, signature);
		}

		// Act:
		final long start = System.currentTimeMillis();
		for (int i = 0; i < numTimedIterations; i++) {
			dsaSigner.verify(input, signature);
		}
		final long stop = System.currentTimeMillis();

		// Assert:
		final long timeInMicroSeconds = (stop - start) * 1000 / numTimedIterations;
		System.out.println(String.format("verify needs %d micro seconds.", timeInMicroSeconds));
		Assert.assertTrue(
				String.format("verify needs %d micro seconds (expected less than %d micro seconds)", timeInMicroSeconds, threshold),
				timeInMicroSeconds < threshold);
	}
}
