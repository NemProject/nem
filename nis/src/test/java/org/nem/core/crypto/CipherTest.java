package org.nem.core.crypto;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.test.Utils;

public class CipherTest {

	@Test
	public void canCreateCipherFromKeyPairs() {
		// Act:
		new Cipher(new KeyPair(), new KeyPair());

		// Assert: no exceptions
	}

	@Test
	public void canCreateCipherFromCipher() {
		// Arrange:
		final BlockCipher blockCipher = Mockito.mock(BlockCipher.class);

		// Act:
		new Cipher(blockCipher);

		// Assert: no exceptions
	}

	@Test
	public void ctorDelegatesToEngineCreateBlockCipher() {
		// Arrange:
		final KeyPair keyPair1 = new KeyPair();
		final KeyPair keyPair2 = new KeyPair();
		final CryptoEngine engine = Mockito.mock(CryptoEngine.class);

		// Act:
		new Cipher(keyPair1, keyPair2, engine);

		// Assert:
		Mockito.verify(engine, Mockito.only()).createBlockCipher(keyPair1, keyPair2);
	}

	@Test
	public void encryptDelegatesToBlockCipher() {
		// Arrange:
		final BlockCipher blockCipher = Mockito.mock(BlockCipher.class);
		final Cipher cipher = new Cipher(blockCipher);
		final byte[] data = Utils.generateRandomBytes();

		// Act:
		cipher.encrypt(data);

		// Assert:
		Mockito.verify(blockCipher, Mockito.only()).encrypt(data);
	}

	@Test
	public void decryptDelegatesToBlockCipher() {
		// Arrange:
		final BlockCipher blockCipher = Mockito.mock(BlockCipher.class);
		final Cipher cipher = new Cipher(blockCipher);
		final byte[] data = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedData = cipher.encrypt(data);
		cipher.decrypt(encryptedData);

		// Assert:
		Mockito.verify(blockCipher, Mockito.times(1)).decrypt(encryptedData);
	}
}
