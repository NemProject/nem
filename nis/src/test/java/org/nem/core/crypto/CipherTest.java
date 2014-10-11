package org.nem.core.crypto;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.ed25519.Ed25519Engine;
import org.nem.core.test.Utils;

public class CipherTest {

	@Test
	public void ctorDelegatesToDefaultEngineCreateBlockCipher() {
		// Arrange:
		final CipherContext context = new CipherContext();

		// Act:
		new Cipher(context.pair1, context.pair2);

		// Assert:
		Mockito.verify(context.engine, Mockito.times(1)).createBlockCipher(context.pair1, context.pair2);
	}

	@Test
	public void encryptDelegatesToBlockCipher() {
		// Arrange:
		final CipherContext context = new CipherContext();
		final Cipher cipher = new Cipher(context.pair1, context.pair2);
		final byte[] data = Utils.generateRandomBytes();

		// Act:
		cipher.encrypt(data);

		// Assert:
		Mockito.verify(context.blockCipher, Mockito.times(1)).encrypt(data);
	}

	@Test
	public void decryptDelegatesToBlockCipher() {
		// Arrange:
		final CipherContext context = new CipherContext();
		final Cipher cipher = new Cipher(context.pair1, context.pair2);
		final byte[] data = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedData = cipher.encrypt(data);
		cipher.decrypt(encryptedData);

		// Assert:
		Mockito.verify(context.blockCipher, Mockito.times(1)).decrypt(encryptedData);
	}

	private class CipherContext {
		private final Ed25519Engine engine = Mockito.mock(Ed25519Engine.class);
		private final BlockCipher blockCipher = Mockito.mock(BlockCipher.class);
		private final KeyPair pair1;
		private final KeyPair pair2;

		private CipherContext() {
			CryptoEngines.setDefaultEngine(this.engine);
			Mockito.when(this.engine.createBlockCipher(Mockito.any(), Mockito.any())).thenReturn(this.blockCipher);
			Mockito.when(this.engine.createKeyGenerator()).thenCallRealMethod();
			Mockito.when(this.engine.createKeyAnalyzer()).thenCallRealMethod();
			this.pair1 = new KeyPair();
			this.pair2 = new KeyPair();
		}
	}
}
