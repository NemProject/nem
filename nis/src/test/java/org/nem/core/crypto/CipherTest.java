package org.nem.core.crypto;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.ed25519.Ed25519CryptoEngine;
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
		private final Ed25519CryptoEngine engine = Mockito.mock(Ed25519CryptoEngine.class);
		private final BlockCipher blockCipher = Mockito.mock(BlockCipher.class);
		private final KeyPair pair1;
		private final KeyPair pair2;

		private CipherContext() {
			// TODO 20141010 J-B: i think setDefaultEngine will make the tests brittle (i.e. it will prevent some tests from running
			// > deterministically in parallel); not a problem for now, but could be a problem if we hook up with something like travis ci
			// > a workaround would be to have an overloaded constructor that is passed the engine
			// TODO 20141011 BR -> J: "overloaded constructor that is passed the engine" not sure I can follow, the classes still use getDefaultEngine().
			// TODO                   Or should getDefaultEngine() be replaced?
			CryptoEngines.setDefaultEngine(this.engine);
			Mockito.when(this.engine.createBlockCipher(Mockito.any(), Mockito.any())).thenReturn(this.blockCipher);
			Mockito.when(this.engine.createKeyGenerator()).thenCallRealMethod();
			Mockito.when(this.engine.createKeyAnalyzer()).thenCallRealMethod();
			this.pair1 = new KeyPair();
			this.pair2 = new KeyPair();
		}
	}
}
