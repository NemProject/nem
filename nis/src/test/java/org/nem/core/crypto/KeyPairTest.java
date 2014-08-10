package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class KeyPairTest {

	@Test
	public void ctorCanCreateNewKeyPair() {
		// Act:
		final KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void ctorCanCreateNewKeyPairWithCompressedPublicKey() {
		// Act:
		final KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.getPublicKey().getRaw().length, IsEqual.equalTo(33));
	}

	@Test
	public void ctorCreatesDifferentInstancesWithDifferentKeys() {
		// Act:
		final KeyPair kp1 = new KeyPair();
		final KeyPair kp2 = new KeyPair();

		// Assert:
		Assert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
		Assert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPrivateKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPrivateKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(kp1.getPrivateKey()));
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPublicKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPublicKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(false));
		Assert.assertThat(kp2.getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void keyPairProducesCorrectPublicKeysSuite() {
		final Map<String, String> priv2Pub = new HashMap<String, String>() {{
			put("9201D5322CDB870181830D7529EDB9A668A09324277263865B5D136500234CB2", "024095F37906AB8FFB9EA44085BED4748F3F5E3FFD66C5A70818399BFCD24308B4");
			put("FED8F9D7E0428821D24E5429FFA5F8232FC08313D61C1BF6DF9B1DDF81973ADE", "02213e18b3c33f06518b6d4d3324b6f0961db98253232666bdd126552e05a0d0f3");
			/* alice  */ put("008c1f538e0f15a1253692b076dfad46578dc874912b148019dd3c7076d4025dba", "02d2de7addd7fce7b3c0144ca40269fdafc2cae7bb08f2c4f8c5f6c665a8a59698");
			/* alice2 */ put("16dcadddaf26707f475d821b3a63c215c93eebe5f26a67f0437b21c5924b4e06"	 , "02c621aba26b261b629edddd912aac1acd8be7b9f0c798823fe4ed0fa65d69d80e");
			/*        */ put("39275331bcae98fc128f807eeb891679ff37cd2c2cbbb0f6471d6c3eefac28fa"	 , "0336cabee030463a5d7df818d1e76a843d065a5b6841ccefe16e5805a754038b7a");
			/*        */ put("76b0ae30ad0daa8827d4212ff6fdc39a4f8721e1f71bfab1d13673b1e6c9cf76"	 , "03327f6bc7311e96f257e4fbde0351cf40ae7cea6a0662019d4140d43b24519b17");
			/* thies  */ put("00c9658e78146a62ee0bc222a25ca79667b7a7e5225b839fce1d4aa20e64aecf1a", "03c55bd250e56c292ed4c898b0883676313283251d21b6a9099bb989db99d736d2");
			/* makoto */ put("37c5c01c109d58a0d5f7fd48ec11507866808dddf72d4b5baeae6d3c7f506150"	 , "036ccaeb7c39125a5d498e48a34a0811a4e8321c0eb37b9e85128823b37142ce48");
			/* patem  */ put("009b815cc4f967bae9fc08cff7b5087967af5b2c6d8a07a45eb39488fe8a38d079", "02bb032b4eaf976090d00776259602b09baebf06908d7855372d2a4eb1db21042a");
			/* gimre  */ put("0091c38fe1de082eef0d728e19bdf7df9de8804f093cbef04e3f0e5d93bc326b6e", "0350f94f8c3a04a4f47356ba749b74418a55511d88a56d180998130d8c26b28bfd");
			/* jagua  */ put("38caf638f4f193fcc689b3eeb3c73849ffe68c12302f24138582d7d8d0cdf918"	 , "031f7b6c1c446a0e9c6f5b76f64043e4b05c6a91e3820624d930e423d0cc644567");

			/* insta    */ put("1fd8f2ff588c24cfddd0a0eb6aa2702d78e4919a595eb891415d8c976c65a159"  , "03f7dc4a82986c89283e2f3e422945f8e9976a2ceb51bdec28c67b8116bcfa276f");
			/* instb    */ put("3e071396f40efd9acf7d33346753593da255a388975d1e9c65fb63460060f172"  , "03e8111baaa5f397271b7440cbdc31f15aee38e425c8050b06533f0dd032cfb2b6");
			/* instc    */ put("5468c0734595a645e0f6ed044038f02b02c3021f9ca836094301a7e9df334d27"  , "0255d91d6a5579e5ab2c22530f6efdd4c8b0e9d308be5f0a4559d755f9f20da755");
			/* instd    */ put("725093849280651bc6df99ce72acf64d8cd84ed129d5ca0f1041ca5a12b7f25b"  , "02f883ed845b1a3311497bb63ab08b2afc0c45f890ebb889738aff5720269ae96d");
			/* seish    */ put("29a90aae90f5ef0c9031783aa1e6378a7d74518468b0e7e8e4af11df4e66526f"  , "03f78fb11f0808654754f9d50683b17b1a31c5b120a5348050f6acb113c1e517f7");
			/* jaded    */ put("7ea895a3e1db6fc7c7c90ccc8f965d4a9721701bc978ec5beead4e9228280de4"  , "03c8f8b2efbc0e068457e2e3bd18bf370f79bf713c5eaa441cc325867090ddc1b4");
			/* bob-gimre*/ put("0091c38fe1de082eef0d728e19bdf7df9de8804f093cbef04e3f0e5d93bc326b6e", "0350f94f8c3a04a4f47356ba749b74418a55511d88a56d180998130d8c26b28bfd");
			/* san      */ put("6ca2381bf63af355e2ec0b7e18f4cff7b44c450abd144d9f451035d45f721d59"  , "03249c6b972d2f79b6dd8f7381643394aad6d203e785fbea87861fd877f1c5b3d6");
			/* go       */ put("5b41d05d88a07b70d37c416adee4181f84f6b80123fedfe97b6f123650117a4c"  , "02f1b676a94c013763ad76d15f5ae8bb4a72acab115375ad5d8195f42b32305cd6");
			/* hachi    */ put("00f932f79698c67f130c3510b37172239dcf5373c1cabe9aeef2f6247a9719bd79", "0331df4cf39e26286522d06ef69e2e9b7505876e8e7498fcd576b92853e73a591a");
			/* jusan    */ put("1c577ef2e971d07b91073e3fb643d8a84bc4c7049c52bbd123037098cd1ff028"  , "021c6fe195b65ee6975b782227ade755339ae98235c5b763590d4bb57d6f08c7c2");
			/* nijuichi */ put("009e728371176014fb287ba3afef07b6d92986e888b3b02cdeba7d211d4504c94d", "023c904975a5d3ce7bf6bd91ad9e9cda6482ae09f20f4db9a5521dd078cd4918fe");
		}};

		// Arrange:
		for (final Map.Entry<String, String> entry : priv2Pub.entrySet()) {
			final String privateKey = entry.getKey();

			// Act:
			final KeyPair keyPair = new KeyPair(PrivateKey.fromHexString(privateKey));

			// Assert:
			Assert.assertThat(keyPair.getPublicKey(), IsEqual.equalTo(PublicKey.fromHexString(entry.getValue())));
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
