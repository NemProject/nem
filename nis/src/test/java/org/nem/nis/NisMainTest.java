package org.nem.nis;

import org.junit.Test;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.JsonSerializer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NisMainTest {

	private static List<String> PRIVATE_KEY_STRINGS = Arrays.asList(
			"308b13a47a241084aac7112aae5552a8227477ab6c6e2b5b3a996c9cd60da387",
			"00c9658e78146a62ee0bc222a25ca79667b7a7e5225b839fce1d4aa20e64aecf1a",
			"37c5c01c109d58a0d5f7fd48ec11507866808dddf72d4b5baeae6d3c7f506150",
			"009b815cc4f967bae9fc08cff7b5087967af5b2c6d8a07a45eb39488fe8a38d079",
			"0091c38fe1de082eef0d728e19bdf7df9de8804f093cbef04e3f0e5d93bc326b6e",
			"38caf638f4f193fcc689b3eeb3c73849ffe68c12302f24138582d7d8d0cdf918",
			"3bb3ac743674dc021589c84df808c2bdd5b3a561dcdf6559e602667ef9417eaa",
			"5b193f29962d9a821479ef335c6a24b2e83bec9942c01d46333f9aa1b3cf2a64",
			"00924d2e6740e223139e89a416ee9cad110c68744bdf27788c4ffe0e00447bf6db");

	private static List<String> ENCODED_ADDRESS_STRINGS = Arrays.asList(
			"TbloodZW6W4DUVL4NGAQXHZXFQJLNHPDXHULLHZW",
			"TAthiesMY6QO6XKPCBZFEVVVFVL2UT3ESDHAVGL7",
			"TDmakotEWZNTXYDSCYKAVGRHFSE6K33BSUATKQBT",
			"TDpatemA4HXS7D44AQNT6VH3AHKDSNVC3MYROLEZ",
			"TBgimreUQQ5ZQX6C3IGLBSVPMROHCMPEIHY4GV2L",
			"TDIUWEjaguaWGXI56V5MO7GJAQGHJXE2IZXEK6S5",
			"TCZloitrAOV4F5J2H2ACC4KXHHTKLQHN3G7HV4B4",
			"TDHDSTFY757SELOAE3FU7U7krystoP6FFB7XXSYH",
			"TD53NLTDK7EMSutopiAK4RSYQ523VBS3C62UMJC5");
	
	private static List<KeyPair> KEY_PAIRS = PRIVATE_KEY_STRINGS.stream()
			.map(s -> new KeyPair(PrivateKey.fromHexString(s)))
			.collect(Collectors.toList());

	@Test
	public void printOutPrivateKeys() {
		System.out.println("*** private keys ***");
		printOutPairs(keyPair -> (String)JsonSerializer.serializeToJson(keyPair.getPrivateKey()).get("value"));
	}

	@Test
	public void printOutPublicKeys() {
		System.out.println("*** public keys ***");
		printOutPairs(keyPair -> (String)JsonSerializer.serializeToJson(keyPair.getPublicKey()).get("value"));
	}

	private static void printOutPairs(final Function<KeyPair, String> toString) {
		for (final KeyPair keyPair : KEY_PAIRS) {
			final Address address = getAddress(keyPair);
			final String serializedKey = toString.apply(keyPair);
			System.out.println(String.format("'%s': '%s',", address.getEncoded(), serializedKey));
		}
	}

	private static Address getAddress(final KeyPair keyPair) {
		final Address address = Address.fromPublicKey(keyPair.getPublicKey());

		for (final String encodedAddress : ENCODED_ADDRESS_STRINGS) {
			if (encodedAddress.equalsIgnoreCase(address.getEncoded()))
				return Address.fromEncoded(encodedAddress);
		}

		throw new IllegalArgumentException(String.format("could not find %s", address));
	}
}