package org.nem.core.model;

import org.nem.core.crypto.PublicKey;

import java.util.*;

/**
 * Class holding all public keys of accounts that are blocked from harvesting.
 */
public class BlockedHarvesterPublicKeys {

	private static final Set<PublicKey> PUBLIC_KEYS = new HashSet<PublicKey>() {
		{
			// Sustainability fund: NDSUSTAAB2GWHBUFJXP7QQGYHBVEFWZESBUUWM4P
			this.add(PublicKey.fromHexString("b74e3914b13cb742dfbceef110d85bad14bd3bb77051a08be93c0f8a0651fde2"));

			// Marketing pre V1 fund: NCMARKECQXP3SQZSJPCBKOQWIXRRI7LIS4FTU4VZ
			this.add(PublicKey.fromHexString("f41b99320549741c5cce42d9e4bb836d98c50ed5415d0c3c2912d1bb50e6a0e5"));

			// Operational fund: NCOPERAWEWCD4A34NP5UQCCKEX44MW4SL3QYJYS5
			this.add(PublicKey.fromHexString("6f4dc5fa7b531f56543a4b567fefea2fcf080be71af73aca9925968eeee2ad0f"));

			// Developer pre V1 fund: NDEVPRE5ZWVG2PXSM54BGZBFF525G24UKVOGOZNT
			this.add(PublicKey.fromHexString("6c8267a0550072a7b89e79bd91cf621862a36bc41d4463f9fcf416759b5dd801"));

			// Developer post V1 fund: NDEVPOSK4OMR4PRTLYFHX4W5QTOND7TZDT2DTU4Q
			this.add(PublicKey.fromHexString("56a7ae8caca7356fffe98e1dfdf3f4218bb837b5ec6aae927a964e2ff1861e20"));

			// Contributors without addresses fund: NCONTRLFKPO6YVXO5WUIJ4SWSCSKBJIPV456RSCL
			this.add(PublicKey.fromHexString("1ef1ba8f753b4931bfceab0ad6a08892f1735f304f6c33ce04b41d66fa10cb4b"));

			// Additional public keys with corresponding addresses starting with NAFUNDA - NAFUNDZ
			this.add(PublicKey.fromHexString("6ab8401388e67b995139c7d1ddd3ea704245ca887b81f956edc5453c7869aac8"));
			this.add(PublicKey.fromHexString("ac4b53284650c8d3a2c5b56dbf8fce9f65cda411a07d7e24cea1cdc867cbbc9d"));
			this.add(PublicKey.fromHexString("f07caa65d1ee756ca41f019a43e4861cc0abc0cf26f9bacb792aa1e269d6f2ce"));
			this.add(PublicKey.fromHexString("662eca63f3ec0235917a8f257e844403983d8e7bc84c05806e889bc57b38dfe4"));
			this.add(PublicKey.fromHexString("6360d1c79365cffb9811aa308e5e545976c1124577597eecb7848b79ec6130a3"));
			this.add(PublicKey.fromHexString("11306ac95b36d409aecf87d8f66b01347d313a06ce1154a5610386e0a57de426"));
			this.add(PublicKey.fromHexString("2333576327e547c6bb23d1f274172bf9b6d07ccefeefe3f9c1ba45991f087d0a"));
			this.add(PublicKey.fromHexString("ec3bb27e0d9aab1757d20bd80986f4fc19a2275b975117588220370aecc44a53"));
			this.add(PublicKey.fromHexString("09dd2c20f3edcc3fd6b7554498c679362261a4a8d7eb6afe01d96c7186ed2e39"));
			this.add(PublicKey.fromHexString("07db1d2fa7b01a22d9e452664a77d29d4a11a8dcdbf96bb3552b2dbd43795f21"));
			this.add(PublicKey.fromHexString("ffeffd454ceee25ba695529a6a92dfc074d326fa49c5f394453ccf8bafa42a4f"));
			this.add(PublicKey.fromHexString("4dcb21e77c46caf810e251f3819cc7f90a32fa5825ff1bb5aae0d8f604a65e08"));
			this.add(PublicKey.fromHexString("43fcf4daf69425cdc03d414a00a780bf9b93dc20a7e4555986b326da5255aa4a"));
			this.add(PublicKey.fromHexString("e369f57a2eb108b8b951bc06715c39e333cd81b75c641cdf9c2b35587f33cf3a"));
			this.add(PublicKey.fromHexString("b7b29594762409fe5da1b0b9ff2c8b32943f701dfc31ab67746c53187b485995"));
			this.add(PublicKey.fromHexString("9cb68afb9481f4f06106761b4705a9ccaf5d4c96d3884fb5ed45a9ba51e31e1b"));
			this.add(PublicKey.fromHexString("b0055501349bdb37f9dfac7ef3d1a4be2907b6a0d9a55afc044b1fbade9b1358"));
			this.add(PublicKey.fromHexString("2071effe75fd8e6ed59da57a39d22c0c65469ee1c09c656af121e2b7c0647547"));
			this.add(PublicKey.fromHexString("2c1217e1050bc32024591b216cf8b508731081f79f05f3488ae39912b354d562"));
			this.add(PublicKey.fromHexString("c64e05fed6a37179b6510a03d8ccfd7e8e255b5deca5aba787c256f8ca24adcb"));
			this.add(PublicKey.fromHexString("25bd1f737c93fff88bc725a44f031fa5bc8f087528add6f588d7527e16cfe24e"));
			this.add(PublicKey.fromHexString("181b16610d68aeff82d5cb74d03239a33ff5b175750d4c813750bbfed413f1c8"));
			this.add(PublicKey.fromHexString("a55b473462780892f8f4c6e47309784ec748f3d490779e581605457c566b88cc"));
			this.add(PublicKey.fromHexString("8b33feb8c470bc0187d509b664383c2b9e0214b55f927452588767b757edb892"));
			this.add(PublicKey.fromHexString("897825b75e12aea7863486140775a6be93e810921a5988a64cd0f2ae49202a76"));
			this.add(PublicKey.fromHexString("73bc5557f69f87532f6a37579d8833144602cbe98d9cf6b5dd517c079ca7f9a5"));

			// Additional public keys with corresponding addresses starting with NBFUNDA - NBFUNDZ
			this.add(PublicKey.fromHexString("9bb55ba153ce6e0a42914fd39dc11aa0e553a17be1e0bf72f90d91287e7ea520"));
			this.add(PublicKey.fromHexString("882afdc37e991f2924b39cc84b1ef03eee762947395d7ff528a8c308ddc94eae"));
			this.add(PublicKey.fromHexString("a70d076b73440b1c78a6b239337cf6266a77864a51ad5ca1175f7465f987e3f6"));
			this.add(PublicKey.fromHexString("181a5f7bf978c40f06c1458ae00fe1541305585398f907fb73f60bd907c38e7b"));
			this.add(PublicKey.fromHexString("382f4b3051b45659bbf25d8b163d3623e2081ada9940750d84cfdec30e075662"));
			this.add(PublicKey.fromHexString("5e2aa5c87bf688f11fcab2f480c0c89264e2e6e44b5422d744c60826bcead492"));
			this.add(PublicKey.fromHexString("b3f4af0f3de8eef7838fb1cfd774397b049e1abed35e620ea3a0ba619ee625f9"));
			this.add(PublicKey.fromHexString("684de40525c90d28400101525454813e37a2ab63cb6648549dfe58cff7ed1274"));
			this.add(PublicKey.fromHexString("c4e58a2645110ee6f01570811391b6ecf0469afd91432cd06901429115a8929c"));
			this.add(PublicKey.fromHexString("ca02a7bf4e01676cca6269b2146405fa6b143a1ae09bfc49355c858b56779d1c"));
			this.add(PublicKey.fromHexString("27d92c571f5dc86eb4469c79cad2b424e11ea073516296ece1cadba6d5bc7344"));
			this.add(PublicKey.fromHexString("b80c4753e4db3e4bcf575effaaf6771e41a7c379c221f221542faddf740dbb9b"));
			this.add(PublicKey.fromHexString("f19693387d3f832ab2dfee257e2c3bbfe93b1fd07bce2311d338f73fc4fd98f1"));
			this.add(PublicKey.fromHexString("511ab951fd34726a11f90a5029b67a9e4adc987084712ba531e94f1e0b804fd2"));
			this.add(PublicKey.fromHexString("6890404e3da7d2f7752633b0832cbb21fa3eb71b1490ac1dbcd7f3d0b8db1992"));
			this.add(PublicKey.fromHexString("02b5f62457dbb66412a3695361a4d86c1e8e28763e738b3ee589dd97787cd148"));
			this.add(PublicKey.fromHexString("f3ef411e00ee659744832dd63f3433973c577419f5b9e8b0267a48fa39f6118c"));
			this.add(PublicKey.fromHexString("fc0febb390e25d2e3bc0711a5bc7cb9f629881cbceef3345de61799f32821d4f"));
			this.add(PublicKey.fromHexString("7a11f1ce863223d70101caa30b0f3bec4f3ae9f3345dceaa84ce20f4d6cf9d7e"));
			this.add(PublicKey.fromHexString("3e8da325573a618b430fc9253493983a757341379def058676f6ee853f461424"));
			this.add(PublicKey.fromHexString("c2ef6a409f3c9ef01116461ee5db48b523e13b921a9b8f816c8247198dd99932"));
			this.add(PublicKey.fromHexString("918b13b6745f1021018a48759845a3fe554b4aa6066f4fbb1378eef286fea8d9"));
			this.add(PublicKey.fromHexString("3e5cec9285a61f1e77d843e75654c31c7e5f4f72abd651db0cfc919f0cf13dba"));
			this.add(PublicKey.fromHexString("56db40dfcf9f454341396a7020d2cba735122c89de3da470a6a32a90365db50f"));
			this.add(PublicKey.fromHexString("98a15955414f44edd806db23aecac2ac60236ffa70752de1aa793f1b00621d59"));
			this.add(PublicKey.fromHexString("e06f1cbba59b07a9e82e090865ce319f06bc6adc975ce9df36812c19de1a6933"));

			// Additional public keys with corresponding addresses starting with NCFUNDA - NCFUNDZ
			this.add(PublicKey.fromHexString("1ebc7ac04d41e6481a2a614afbd6c7a2bd11619acf8d5bec2d227c54621ce20e"));
			this.add(PublicKey.fromHexString("5b5735628238b17fb673a07fd71f25c76e4ba486b740f1a6ba84e1f3e8e2f0ec"));
			this.add(PublicKey.fromHexString("921c08b4871938205dd3828063f64f7dd3e82ecef82d2eb60e023ad99e88f2c0"));
			this.add(PublicKey.fromHexString("2ba94b5a165bd171b485bb3cc903a8349adbaff55ebfd24c2a6bfedcf553910d"));
			this.add(PublicKey.fromHexString("f8e54d902a1a934f799225a160a48f8fa261ad96f669d8e9157f3799a41f9f54"));
			this.add(PublicKey.fromHexString("4df74b0e40b01275a7916bde35c92354ceb088d3bf302a1bbe649eefaebc66f6"));
			this.add(PublicKey.fromHexString("fa3cf6b89d652891d911180e3b7bd4818c4dd61d47d2b7454b3322dfc31306c0"));
			this.add(PublicKey.fromHexString("d0ddef2eca4c0b09a0bc53b306e9c77a1e6ad7a585cf784b6297bbffc7e71325"));
			this.add(PublicKey.fromHexString("1cc0b5d3e1790321013c52668030d343a7ffbc75b65a38255e33ee2977cd46d0"));
			this.add(PublicKey.fromHexString("7ece037aecce035469d26f5633f74f9d5f2b2755e4affc6a2266b7e203a934a8"));
			this.add(PublicKey.fromHexString("dcb8e3447f8f69f2f96bfe0da853d906e25b5913a3b41927fc98f228cb00a9ad"));
			this.add(PublicKey.fromHexString("45c6da8ef743f68257571f72536d4f51a5482a5acafb912633d3aaba9e8030cb"));
			this.add(PublicKey.fromHexString("84631c1438138a3c29980c906b0d9cc3a6d51f34c43b6f13f9875cda0f5de235"));
			this.add(PublicKey.fromHexString("c8a9ed948367012cea4c5b89a01614738f18d38ce228d93175c94e76cd1aac49"));
			this.add(PublicKey.fromHexString("ba229bacf05da028bab79b7e14e8c0b9fb52fe42bda8358abc0387e7c464dd8c"));
			this.add(PublicKey.fromHexString("9c5eb6c5c79e138c8a3e4a7691241f50f7e9c89df9dc30585c910e05e0a6e41e"));
			this.add(PublicKey.fromHexString("42b8605791be31935dc67f75a7588635e1952d46df1974286badef47b6ce029c"));
			this.add(PublicKey.fromHexString("363f1b5ea4f34816853032c9a9a17724e01f4742fd237a4c1791bf65fcda4ce6"));
			this.add(PublicKey.fromHexString("72e206a75cc7c9c8ef7dd91f359e33386543942cd5a1c8395f7c13351e0b025b"));
			this.add(PublicKey.fromHexString("79c16ebfebc2058450916d39ad5ff7b5277b0140ca8c23b7871473c7d50e0793"));
			this.add(PublicKey.fromHexString("531e6b6a1e13ad43d2dbc70d845c8c775e845672edabc3bbfda04e701b88ab46"));
			this.add(PublicKey.fromHexString("36094d8ac6ac8a6cc824180d79c1f7e17bc25b9859b0dd1b32edfd19cece096f"));
			this.add(PublicKey.fromHexString("4c4e507e576951cbf55efbf11411938b3abb50dfbf5d27a79b543e463a9eeb53"));
			this.add(PublicKey.fromHexString("3bb22358f7fd5112caa5138a7c11c5e3e2732e5dc8a8c43a9465196754a2e8e2"));
			this.add(PublicKey.fromHexString("6a3a4c9076dc7bf85bb939328180223b3946a25c36fd4e0cc0ea32ee3e5862e2"));
			this.add(PublicKey.fromHexString("a5a833f11b940d67d871040970eab1dd61267a240bee0adba2116484db9bb22a"));

			// Additional public keys with corresponding addresses starting with NDFUNDA - NDFUNDZ
			this.add(PublicKey.fromHexString("389211df83774be63ddd6fd72bbbc918ecd44e2574c72cc27f51952f3ce74964"));
			this.add(PublicKey.fromHexString("c3c169cbc88b97d50317cb53af63152ba82be86db5c060877979dcc8b3d19f11"));
			this.add(PublicKey.fromHexString("a40e91ee22e8fb42b16b91858c55132d7d5ae08b7339be8d23d3edbce66c417d"));
			this.add(PublicKey.fromHexString("2bee57dab2a89459ff163040f05936177731157a8db22d0efff52b69013ea4ad"));
			this.add(PublicKey.fromHexString("1b367590a64c778a5d289ae4983c2820d135544c09d1c45d5a1dfffe9868d602"));
			this.add(PublicKey.fromHexString("bd127252e27931abd007d3d6f8274c87607e8a2c4b466190ebfcb823f780854a"));
			this.add(PublicKey.fromHexString("532edc9bb1046227e9d51d73d6913c927a16476b537a9d6f9f36efb0fead222c"));
			this.add(PublicKey.fromHexString("1873b7dea228c2d5958f057fc281789bdb62e19b7d09adca555ef3babb720955"));
			this.add(PublicKey.fromHexString("04601981d0d3434c89f7f11c4eb74b9336dbdb6b2161706caf7b279f905725af"));
			this.add(PublicKey.fromHexString("84d632da42c4d2dcf3b13e63482361b515605e061d5a58e9e83cd4e6a153943e"));
			this.add(PublicKey.fromHexString("664024fce645a16cb554ff64757d4405da4314ece7f810733ac1d5cc3cd710ce"));
			this.add(PublicKey.fromHexString("318d3c2fc23454ddb6165a6a966df148afc9953b0d4c8522b50f15346248cbb9"));
			this.add(PublicKey.fromHexString("58443adf49a4ec8faa4dc7156c94e481154409454a65b96b94c05f8d30deb978"));
			this.add(PublicKey.fromHexString("43e8e90e08d4fbd0abfa56e0e92c9873100f5a6ef2bcdb2d86655dfad5cce188"));
			this.add(PublicKey.fromHexString("48473fbf50ddde524c533ebe361f5370e0f6819ac1cca0c95d4ddf9ca12fb119"));
			this.add(PublicKey.fromHexString("875439b91d8f5cd4a52d48fa8280348868b3a2d0631c60bcb5aaaaea49b7a469"));
			this.add(PublicKey.fromHexString("6687653f78afda56ce06102561110ac0297d81c8d65a5b85f856127c01100cfe"));
			this.add(PublicKey.fromHexString("cd639310b9fd73ffa1aa9dc1f945ca0f20074697b31cb4de3fad4e37625a0b75"));
			this.add(PublicKey.fromHexString("3c2a2c14199fa51bf96d7d8a15de20df5b598517767fed08f024a736ca2bbf8d"));
			this.add(PublicKey.fromHexString("9c160ec4dbd02e6922876ebe1223ead48977f8b7d2f2cbb50303b6a51696933b"));
			this.add(PublicKey.fromHexString("ac435fceddd6184a4c6dcce70aa0cbb119e996f7a5c09841ad2f4f506e31b950"));
			this.add(PublicKey.fromHexString("6c45add414e655ce7bdebf370c2567d77dea1b8912558b9a6a7005472711a6ff"));
			this.add(PublicKey.fromHexString("9e0ec9266e186c74d7ab27638694dd9f3dfa601641b20ef55100011cbcf68af8"));
			this.add(PublicKey.fromHexString("f6f024bc6558dbb955669afe6b6d2e2416a6a44eb893ee0f3c9b6142bd19f745"));
			this.add(PublicKey.fromHexString("0ac15257978fb10f4b76dd83b19e7d7ed3e13123cf653b3c33d10dd65197c7bc"));
			this.add(PublicKey.fromHexString("2de79193ac4e974b8d172c5b0f17ccc1f57b3c8c49c793a47040e94c5d02c4c8"));
		}
	};

	/**
	 * Gets a value indicating whether or not the given public key is blocked from harvesting.
	 *
	 * @param publicKey The public key.
	 * @return true if the public key is blocked from harvesting, false otherwise.
	 */
	public static boolean contains(final PublicKey publicKey) {
		return PUBLIC_KEYS.contains(publicKey);
	}

	/**
	 * Gets the collection of all public keys blocked from harvesting.
	 *
	 * @return The collection of public keys.
	 */
	public static Set<PublicKey> getAll() {
		return Collections.unmodifiableSet(PUBLIC_KEYS);
	}
}
