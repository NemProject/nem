package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

public class KeyPairTest {

	@Test
	public void ctorCanCreateNewKeyPair() {
		// Act:
		KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNot.not(IsEqual.equalTo(null)));
		Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPublicKey(), IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void ctorCanCreateNewKeyPairWithCompressedPublicKey() {
		// Act:
		KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.getPublicKey().getRaw().length, IsEqual.equalTo(33));
	}

	@Test
	public void ctorCreatesDifferentInstancesWithDifferentKeys() {
		// Act:
		KeyPair kp1 = new KeyPair();
		KeyPair kp2 = new KeyPair();

		// Assert:
		Assert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
		Assert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPrivateKey() {
		// Arrange:
		KeyPair kp1 = new KeyPair();

		// Act:
		KeyPair kp2 = new KeyPair(kp1.getPrivateKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(kp1.getPrivateKey()));
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPublicKey() {
		// Arrange:
		KeyPair kp1 = new KeyPair();

		// Act:
		KeyPair kp2 = new KeyPair(kp1.getPublicKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(false));
		Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(null));
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorFailsIfPublicKeyIsNotCompressed() {
		// Arrange:
		final PublicKey publicKey = createUncompressedPublicKey();

		// Act:
		new KeyPair(publicKey);
	}

	private static PublicKey createUncompressedPublicKey() {
		// Arrange:
		final byte[] rawPublicKey = (new KeyPair()).getPublicKey().getRaw();
		rawPublicKey[0] = 0;
		return new PublicKey(rawPublicKey);
	}
}
