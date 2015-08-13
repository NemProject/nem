package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public class BlockCipherStressITCase {

	@Test
	public void ed25519EngineCipherCannotBeCracked() {
		// Assert: no failures
		stressCipher(CryptoEngines.ed25519Engine());
	}

	@Test
	public void secp256k1EngineCipherCannotBeCracked() {
		// Assert: no failures
		stressCipher(CryptoEngines.secp256k1Engine());
	}

	private static void stressCipher(final CryptoEngine engine) {
		final int numIterations = 10000;
		int i = 0;
		long start = System.currentTimeMillis();
		while (i < numIterations) {
			stressCipherSingleIteration(engine);

			++i;
			if (0 == i % (numIterations / 100)) {
				final long stop = System.currentTimeMillis();
				final long timeInMilliSeconds = (stop - start);
				System.out.println(String.format("iteration %d (%d ms)", i,	timeInMilliSeconds));
				start = stop;
			}
		}
	}

	private static void stressCipherSingleIteration(final CryptoEngine engine) {
		final BlockCipher blockCipher1 = engine.createBlockCipher(KeyPair.random(engine), KeyPair.random(engine));
		final BlockCipher blockCipher2 = engine.createBlockCipher(KeyPair.random(engine), KeyPair.random(engine));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes1 = blockCipher1.encrypt(input);
		final byte[] encryptedBytes2 = blockCipher2.encrypt(input);

		// Assert:
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes2), IsNot.not(IsEqual.equalTo(input)));
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes1), IsNot.not(IsEqual.equalTo(input)));
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
	}
}
