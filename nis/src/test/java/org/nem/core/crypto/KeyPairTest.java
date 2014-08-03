package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;

public class KeyPairTest {

	@Test
	public void ctorCanCreateNewKeyPair() {
		// Act:
		KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
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
		Assert.assertThat(kp2.getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void keyPairProducesCorrectPublicKeysSuite() {
		final String[] privateKeys = {
				"9201D5322CDB870181830D7529EDB9A668A09324277263865B5D136500234CB2",
				"FED8F9D7E0428821D24E5429FFA5F8232FC08313D61C1BF6DF9B1DDF81973ADE"
		};
		final String expectedPublicKeys[] = {
				"024095F37906AB8FFB9EA44085BED4748F3F5E3FFD66C5A70818399BFCD24308B4",
				"02213e18b3c33f06518b6d4d3324b6f0961db98253232666bdd126552e05a0d0f3"
		};

		// Arrange:
		for (int i = 0; i < privateKeys.length; ++i) {
			final String privateKey = privateKeys[i];
			// Act:
			final KeyPair keyPair = new KeyPair(PrivateKey.fromHexString(privateKey));

			// Assert:
			Assert.assertThat(keyPair.getPublicKey(), IsEqual.equalTo(PublicKey.fromHexString(expectedPublicKeys[i])));
		}

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
