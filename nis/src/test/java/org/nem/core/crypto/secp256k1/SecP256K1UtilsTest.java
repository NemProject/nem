package org.nem.core.crypto.secp256k1;

import org.bouncycastle.crypto.params.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;

public class SecP256K1UtilsTest {

	@Test
	public void getPrivateKeyParametersReturnsPrivateKeyParameters() {
		// Arrange:
		final KeyGenerator generator = new SecP256K1KeyGenerator();
		final KeyPair keyPair = generator.generateKeyPair();

		// Act:
		final ECPrivateKeyParameters parameters = SecP256K1Utils.getPrivateKeyParameters(keyPair.getPrivateKey());

		// Assert:
		Assert.assertThat(parameters.getD(), IsEqual.equalTo(keyPair.getPrivateKey().getRaw()));
	}

	@Test
	public void getPublicKeyParametersReturnsPublicKeyParameters() {
		// Arrange:
		final KeyGenerator generator = new SecP256K1KeyGenerator();
		final KeyPair keyPair = generator.generateKeyPair();

		// Act:
		final ECPublicKeyParameters parameters = SecP256K1Utils.getPublicKeyParameters(keyPair.getPublicKey());

		// Assert:
		Assert.assertThat(parameters.getQ().getEncoded(), IsEqual.equalTo(keyPair.getPublicKey().getRaw()));
	}
}
