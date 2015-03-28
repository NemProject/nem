package org.nem.core.model;

import org.nem.core.crypto.*;

import java.util.*;

/**
 * Class holding all addresses which are not allowed to harvest blocks.
 */
public class NonEligibleHarvesterPrivateKeys {

	private static final HashSet<PrivateKey> privateKeys = new HashSet<PrivateKey>() {
		{
			// Sustainability fund: NDSUSTAAB2GWHBUFJXP7QQGYHBVEFWZESBUUWM4P
			add(PrivateKey.fromHexString("d764f9c66fa558ef0292de82e3dad56eebecfda54a74518187ae748289369f69"));

			// Marketing pre V1 fund: NCMARKECQXP3SQZSJPCBKOQWIXRRI7LIS4FTU4VZ
			add(PrivateKey.fromHexString("75b9041a73845bb646ca7cebb631a5d927deb9f31e88fe09865c743cbb6f05b8"));

			// Operational fund: NCOPERAWEWCD4A34NP5UQCCKEX44MW4SL3QYJYS5
			add(PrivateKey.fromHexString("a07630d53dae153165d851aeb13e605f7514c8d87c0e1b39e522ab9bb68521cc"));

			// Developer pre V1 fund: NDEVPRE5ZWVG2PXSM54BGZBFF525G24UKVOGOZNT
			add(PrivateKey.fromHexString("89ff78fd5bd1ca43d9245b67b2ef2acb74c6328401bccbac269e621c019b8414"));

			// Developer post V1 fund: NDEVPOSK4OMR4PRTLYFHX4W5QTOND7TZDT2DTU4Q
			add(PrivateKey.fromHexString("02691be329fdec69b0e298ba3638a352d1855187f92b76f6be25ba93bc0201b3"));

			// Contributors without addresses fund: NCONTRLFKPO6YVXO5WUIJ4SWSCSKBJIPV456RSCL
			add(PrivateKey.fromHexString("5d059caa488fabccfa362df83245296386cda75a0115c258b9d4876d8c7b7163"));

			// Additional private keys with corresponding addresses starting with NAFUNDA - NAFUNDZ
			add(PrivateKey.fromHexString("1bd36500d5a7304a77e15fe990f7085bb10ee7d808113d6dbfc35c7ec56521ba"));
			add(PrivateKey.fromHexString("d68099c800015076165ac38d5f76b0be5b6847e14eaa970e118cf605e5d16944"));
			add(PrivateKey.fromHexString("2a0f6e25688a290d1038ff02c084eb32be5be87af121a2eed58d9ace951f898f"));
			add(PrivateKey.fromHexString("66d732f8bdd8cb05fe8db71abb032a8b33db9ea37d991e51f67f72a538469444"));
			add(PrivateKey.fromHexString("885198f9cd45ce4630aac22a076ab9c02112722446bb135f3192819125aad4a7"));
			add(PrivateKey.fromHexString("4e74675dae03559125ad0c690e46f3cb9dbd1ef08e6b711f1e6ef35cde4a6ff1"));
			add(PrivateKey.fromHexString("d2d1bdff8b707063d167c659758057e7ca10d730e3eca95d911bc28eb997401b"));
			add(PrivateKey.fromHexString("f6ccea10e34dccab183ab299e2b656cc432233b9f0763316a442cf443f6b9e79"));
			add(PrivateKey.fromHexString("cdd40d119015af19308782c170a8a3e0c7d3494f47d4c8e274d5c944e8f9ea2a"));
			add(PrivateKey.fromHexString("d13fcb16c851b652276f1707a091ee581480bf6a27ba956b1f89a401b5f8d947"));
			add(PrivateKey.fromHexString("2ffccf4cf3fe69c3ee46fcdf3cb4c63b0cbf8eee3a0b15c38d491ff60e89d6ee"));
			add(PrivateKey.fromHexString("0a5dc60f0732b305c2afaa6f9a415d9dcf95f5a767ba5845f3026218fc09575c"));
			add(PrivateKey.fromHexString("df01b57b21fcc998e96a69af14053b99f9d66522b76a439dfd28f8650de06ebc"));
			add(PrivateKey.fromHexString("e0cf8fc09319809f23b00edc5a7d0b67351ecf2283c40ea5ecd8aa4c86196a33"));
			add(PrivateKey.fromHexString("6ed23d9f1c0fc23e106c7610a103a66b6470ec20bb221e1865e300f8527b51f4"));
			add(PrivateKey.fromHexString("bba7bd5cf7fa50a1fc6c5bd9b19f1616707f2d931a6be7203329b9a4ab9b39f4"));
			add(PrivateKey.fromHexString("d3c72d264de99ac50e5a95841a49f8eb45ec26e0065c12431cf711a96a7f56fb"));
			add(PrivateKey.fromHexString("09766e9da6e805592a68933b2ee8f7bb837ef15762084e01a65b25cf192b1ed7"));
			add(PrivateKey.fromHexString("1883ab17e151187960115021571375014766744fc20bbb225ca24af9a012ff14"));
			add(PrivateKey.fromHexString("8a7dc2a8a6217bbf2a82222478c87fb0584ca76768f56dbc3642db7b9b1b601a"));
			add(PrivateKey.fromHexString("623b4f690f4784a3e28d0f09e2c2c219037be794e58eabc17926dd4d1abad3e6"));
			add(PrivateKey.fromHexString("e947e83a81861d33f383e3e2938de4dd7cfb4a0e79a55b186bb0f20f9f2bcc7d"));
			add(PrivateKey.fromHexString("3541bc1dabae66b56ed948d058c1b85608348a9d7c4e1149aa0fcacbc677f3e5"));
			add(PrivateKey.fromHexString("9a0e76f028e24120107d3aaa8dd78fea09f683241bdafe0dcf0172c696c4540c"));
			add(PrivateKey.fromHexString("53324dca00bdf81403f6c2a143622ecbba6caeda9923400f6d47712de766da40"));
			add(PrivateKey.fromHexString("7273fcad58ab4c0a75b5ebf57a9ff18246a49898c1648a08f197bbd9311a16a1"));

			// Additional private keys with corresponding addresses starting with NBFUNDA - NBFUNDZ
			add(PrivateKey.fromHexString("edaa799e99f380dd4fc316c2423f50a22a1c7078c6d6cb162602ae09147e0e96"));
			add(PrivateKey.fromHexString("cb3e4556e9c765e9cb5f7499af1d90a039cf61fc341e0ca08f6aaba9be0c67a8"));
			add(PrivateKey.fromHexString("02a395b818e8dc16a621c3ce7bc8259eddf854542a5762865fa418c303762034"));
			add(PrivateKey.fromHexString("e293604165a0e0cc33e04376f9d9effd4789054b489e1f18869b0ab8023b3d04"));
			add(PrivateKey.fromHexString("8ef9748690a95a5b38176e7e4d05cc6a2fffe4ce01dbae604d4a37e2d82fd335"));
			add(PrivateKey.fromHexString("593c2db04fcdcff1083bdfcc931d7d4ec7685982ebe55277e17cb1cf396e98a9"));
			add(PrivateKey.fromHexString("c070cd719f9063079de0917f830de6abd2ce7ca014d22ff15db75d352837d159"));
			add(PrivateKey.fromHexString("dfae5734c87a5adfd3fd890e9b33b01267c856c2f5b6164390be9b144a470117"));
			add(PrivateKey.fromHexString("2161d5e41fd6f5369a0352cfe649c21771c25b7c696f00298de45ae0529da397"));
			add(PrivateKey.fromHexString("21e6c5f2d1eb5276e4109408d96d1f40aeaf0f65c93b3a6b3a48edf710cd189f"));
			add(PrivateKey.fromHexString("76ddccbf42aa4802405258d72871ef70365f4a1bc69a912f14e0dc3397985f0a"));
			add(PrivateKey.fromHexString("4ec917c9aeb5213b8ab3893ecedc15d0fe052a3821d6783b1c7ec87dbbee5810"));
			add(PrivateKey.fromHexString("65d7e3e9686d1552a20f108da651bef6c4294ec561506d1924ab6640cf90538f"));
			add(PrivateKey.fromHexString("6c6cd6d269c83ad07de82a9e643b1d1a6e9da2757af8bfc116d997ed41f9042c"));
			add(PrivateKey.fromHexString("870af8a14fefb1964fdb04483f0ba4482fa3ef296710dcf0912843c65101742d"));
			add(PrivateKey.fromHexString("2e3b8d5e972cbb49c7df8c2638400223c311f436fe28126f592fdae3f504b1f5"));
			add(PrivateKey.fromHexString("9bb25189e2e7fa5eb42def6967d876f3c7fc2a2ae9cb10b9b02a110a7e72a5e3"));
			add(PrivateKey.fromHexString("75e0c744ed37e098e42b2aa361e5742505a1dc29eec4b5d116523475ce4c547b"));
			add(PrivateKey.fromHexString("becd2f736d5e2e75db618d658843999c0afa2ffd5b1f83501cd11e4aa740aeeb"));
			add(PrivateKey.fromHexString("2575a194d59043b7cfff624a28f44448323b12d76e3e6f8ff15e25da8aa6d70f"));
			add(PrivateKey.fromHexString("9519affd052542483d151ab5864a144a0481d1c638eae0e2ef3a415214ac4a11"));
			add(PrivateKey.fromHexString("81984f68d8e168a56cd0605c5a41658075ee5157e8a9604de404cb1696eb17d7"));
			add(PrivateKey.fromHexString("785eebc5c3a0bf9b12b02b92ceafe0437eca43823384186438693e3cd2cba761"));
			add(PrivateKey.fromHexString("8fd2829355b501245515cbafbc56e37686bdf82bdfa333f7ddcc2024b68926fd"));
			add(PrivateKey.fromHexString("14143f69cacfcea7e235750a4ea071cd2ad1f1e658812467427bd05bbea7c7cf"));
			add(PrivateKey.fromHexString("8277aad9f1f170e31f4e68f979a31a2f4f1597847655620e7e89e7b40601de7f"));

			// Additional private keys with corresponding addresses starting with NCFUNDA - NCFUNDZ (NCFUNDC missing)
			add(PrivateKey.fromHexString("9469e30195e79c4e86dc7d07ef3e871072d5e55e400d917569611256bfc210b4"));
			add(PrivateKey.fromHexString("edf19549a44848bdc5e314e987931e4e519e626ac5b321e34984f8253847c7ab"));
			add(PrivateKey.fromHexString("487288c36fc7ba7151cfb003f5add9c7c6b63643e9ea67bf7570b5a1dfa71be2"));
			add(PrivateKey.fromHexString("634e8cda2702230dec7cb8d0620b4d3902e50959ce64af68bd3ee5b3cd9eda9a"));
			add(PrivateKey.fromHexString("b76370a88c02ea7a2d4c3e00eddea0bce795266af5898f091aedc5e48dbe345c"));
			add(PrivateKey.fromHexString("bfcffb59bca20ac6f158887c0748b25911bc3d194111cb75f003b32c2c3ab079"));
			add(PrivateKey.fromHexString("7a075dc17a1c1f200db6cd054fc1479d1fcd8c0cf1f6e1b21980b99f1283f958"));
			add(PrivateKey.fromHexString("6fc25c8386a7a4c890c83fcaa85bc6350cab0e2a390f5944aa94ae87b5041f1e"));
			add(PrivateKey.fromHexString("a8ce384a96347787ad48f1a2d93b698fe587ec6d2487745570a4ed90ff8f85dc"));
			add(PrivateKey.fromHexString("2ff7b923c5bf3c10e3900ce18d3b84f04998a733aea2ae10e8d275d6a16302e8"));
			add(PrivateKey.fromHexString("cfed38264b447b6e956c556ef626180ea008ee3ef5a9304c16d03404eb12796d"));
			add(PrivateKey.fromHexString("71daeffcd3b1bc264272b739bcb210e8b7ff42521af37b4365aacfb056febca1"));
			add(PrivateKey.fromHexString("eb884e2331ae572b4187f612ba123cb0a596b3d899aca7816cc00d21e438062e"));
			add(PrivateKey.fromHexString("c0b774a3a2040b912bdc89e0626ed6691af5a4e431efb1165703f90b4d0258ea"));
			add(PrivateKey.fromHexString("9469115234e22ae0f99295d3cbcc1d36681f01ed7e0f893b8d886f29a1f1963e"));
			add(PrivateKey.fromHexString("cc20d0ae021cb1bfc063c264baffee1ddaff1068a2fce9a88858f3aaf9a78174"));
			add(PrivateKey.fromHexString("5558ea9026af88e7ca6d161664e4f35efee81af77c851345297bd9051839f75c"));
			add(PrivateKey.fromHexString("f8d699c884e320a8b98d8c7be07cadeecf9048c47211b15f48d2f424e43b6b88"));
			add(PrivateKey.fromHexString("8fb54ead32cf66642bc203ad811d90457039f2924355314a98e3e4f04a9bd2ac"));
			add(PrivateKey.fromHexString("2c5c00cada3367d154ca0af7a339a86b2842c494ebce2b82654bd145550c780a"));
			add(PrivateKey.fromHexString("249a11e7c584ff3280c911430c96278922456ac2435e6d97b0f8c501e02d106d"));
			add(PrivateKey.fromHexString("f805c00e121e02261bf3aeb073dc58b6ae5fccfc2c575be127cd8e6c95f79647"));
			add(PrivateKey.fromHexString("45ad520869c4621f74ddb7e8104443b5687514ec7864bd5f69018e78edea07eb"));
			add(PrivateKey.fromHexString("45d524b4bc02ccc77818d6fa2d5983233703cfa87e15e7c72b9f9066d3880770"));
			add(PrivateKey.fromHexString("d725c01a9831712e8d3c8ce124b21ef028f2b2288e9f2725e675870de6543e93"));
			add(PrivateKey.fromHexString("1684f15929e28d64422a8d080ba3d270646049f0e077c1347a7d00a51d7fcd9c"));

			// Additional private keys with corresponding addresses starting with NDFUNDA - NDFUNDZ
			add(PrivateKey.fromHexString("60d74344bdc9b6d7eeb4aeaf7b3b6186589cf0d60897c21b54c7e1eb6e468382"));
			add(PrivateKey.fromHexString("60ef688edd1d0a99683a7e56e2fd9628d0047caf312fa9456fd73945d93b8e19"));
			add(PrivateKey.fromHexString("067de2225aecdc77b64142c138097075778dafdcaa6234f062db18973fa66cf0"));
			add(PrivateKey.fromHexString("544536e4605559103a0334830f804d012770f267a9aa3a45588362eb75666dfb"));
			add(PrivateKey.fromHexString("f351d593f44fc6e6d5653ec78d6752695e54c0d572e0a36b5e164152f1fa2f0b"));
			add(PrivateKey.fromHexString("2e4d6e28d74fbaf57e22c7da65f6919389d15aa6af859901b3fab4fbb731b2e6"));
			add(PrivateKey.fromHexString("e5d89784b9c8102e5b8f8599491b926d4f23f34a09cb25d0998c0c3ef423961a"));
			add(PrivateKey.fromHexString("86d648d4ae7065b5a3835a9d7e3d64875c1ba3571224f9c6ec7385d9cf4420ec"));
			add(PrivateKey.fromHexString("72fff06bba82996ba588ca90082652a2c03348e1d6880d9e9da3af25db2d9d26"));
			add(PrivateKey.fromHexString("b8fe06a6449fa3ff3d38cdc106103c3d305b25273d710e90f7cb288d6cbbdf37"));
			add(PrivateKey.fromHexString("0fb094d4a2fa6e531bb249a70f7f42e976b55455a801c290f29b6c85a774d134"));
			add(PrivateKey.fromHexString("772c4523c31a27743952f183d87e69f60e37bef013c657dc08ca0b0a1f7d15c9"));
			add(PrivateKey.fromHexString("d9effe76bb8adaaf4f786c8f02357f82ae603970600fb2131b07f74d24ad6328"));
			add(PrivateKey.fromHexString("f308d0080c8d670947dd18cdaec280784b6a972805eb946629efb76197644608"));
			add(PrivateKey.fromHexString("3e30ebe3fd1b7d23a52a6e9d28be8629a7e65afb6084d87a19bd74fefd906a2f"));
			add(PrivateKey.fromHexString("b4eabf5bc398e9d8b2c15ac7c6a16e736cfe2d41ed03e59a23b01f56d5b6e771"));
			add(PrivateKey.fromHexString("d55918fe14ed81d44038f6a23ef870e9220ef806333a11c168b3bfa885a42581"));
			add(PrivateKey.fromHexString("c652280fe8840e0f66da91bb31da174c254849b4f44670ae50f52f70289da130"));
			add(PrivateKey.fromHexString("188b614ba8f6e88146b4fc8cfc479c2cf5c88e43af0d58a45cc61db373584752"));
			add(PrivateKey.fromHexString("27e6e96702ff28bc2e6ff7376ee862b2f00487f8c4d95aff7bc2c42ea81b8ef7"));
			add(PrivateKey.fromHexString("ff253f628e56b04ba9249fb5679bad1ff0b3b767d15e2e40bec1ee11400c0c9e"));
			add(PrivateKey.fromHexString("bb0ddbe68fdb1ed0e04f95d7fc385f03b274204fba54054856f8500b0ba9b4fc"));
			add(PrivateKey.fromHexString("52162ce863dbd555d83c64d79daf8b2d20e330bb93ab7584cab55de839b402bc"));
			add(PrivateKey.fromHexString("cdb7a46535a41fb60d2bb089bce4fcebd67e240156729bb47bb9b13216de3dc4"));
			add(PrivateKey.fromHexString("89d58d494c274ddb05d78675229d47f9e1b6eff77144fea04c6ccdeef4110654"));
			add(PrivateKey.fromHexString("b6c60eb3d8780e34e3c6f8b99fd0b05de63709d0e662658370b7426d81397409"));
		}
	};

	private static final HashSet<PublicKey> publicKeys = new HashSet<PublicKey>() {
		{
			privateKeys.stream().forEach(priv -> add(new KeyPair(priv).getPublicKey()));
		}
	};

	private static final HashSet<Address> addresses = new HashSet<Address>() {
		{
			publicKeys.stream().forEach(pub -> add(Address.fromPublicKey(pub)));
		}
	};

	/**
	 * Gets a value indicating whether or not the given private key is eligible for harvesting.
	 *
	 * @param privateKey The private key.
	 * @return true if the private key is eligible for harvesting, false otherwise.
	 */
	public static boolean isEligiblePrivateKey(final PrivateKey privateKey) {
		return !privateKeys.contains(privateKey);
	}

	/**
	 * Gets a value indicating whether or not the given public key corresponds to a private key which is eligible for harvesting.
	 *
	 * @param publicKey The public key.
	 * @return true if the public key is eligible for harvesting, false otherwise.
	 */
	public static boolean isEligiblePublicKey(final PublicKey publicKey) {
		return !publicKeys.contains(publicKey);
	}

	/**
	 * Gets a value indicating whether or not the given address corresponds to a private key which is eligible for harvesting.
	 *
	 * @param address The address.
	 * @return true if the address is eligible for harvesting, false otherwise.
	 */
	public static boolean isEligibleAddress(final Address address) {
		return !addresses.contains(address);
	}

	/**
	 * Gets the collection of private keys.
	 *
	 * @return The collection of private keys.
	 */
	public static Set<PrivateKey> getPrivateKeys() {
		return Collections.unmodifiableSet(privateKeys);
	}

	/**
	 * Gets the collection of public keys.
	 *
	 * @return The collection of public keys.
	 */
	public static Set<PublicKey> getPublicKeys() {
		return Collections.unmodifiableSet(publicKeys);
	}

	/**
	 * Gets the collection of addresses.
	 *
	 * @return The collection of addresses.
	 */
	public static Set<Address> getAddresses() {
		return Collections.unmodifiableSet(addresses);
	}
}
