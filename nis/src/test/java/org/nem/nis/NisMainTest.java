package org.nem.nis;

import org.junit.Test;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.JsonSerializer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NisMainTest {

	private static final List<String> PRIVATE_KEY_STRINGS = Arrays.asList(
			"983bb01d05edecfaef55df9486c111abb6299c754a002069b1d0ef4537441bda",
			"c2dd81157ecbe0bda5d0a2d38e826887da201b05d5fa0b6b241186f731b37674",
			"053ba4b2b2204668668f371f1c76276b849a1e29402660d6c82631423f3560dd",
			"1f8d73a13abaf266bb819c390e26bff0b3ea46b5366798f7489d20ae7149bd7b",
			"71655699ce872415bed291cc05e8f06b29a6b14a1da18dc1ea84d092a401e572",
			"823541e7e0a9e61387bcc66dabf3e0b9257ca168437a01907f82c6012ecc896f",
			"c0f885f4e70dd86cf10d926fee80d2bb051e00bafaa1b2d54771d8b1096498fb",
			"3029c55412442244defb01deef360db9b6ddf4779479e1436e67028dc44ca5f7",
			"1e2fcb717b7f10b631224c949529e878f4188a961c9d10ed3863eda93b77f5a3",
			"fb1a7d3399ef4722fb04b017cef8762fcc42a43eee987b55bb3a3948ea7cb44e",
			"e8da26bf835b3caca4712b8ca7cf893dce6e1cd1e00fe8601a392fea043f69df");

	private static final List<String> ENCODED_ADDRESS_STRINGS = Arrays.asList(
			"TALICELCD3XPH4FFI5STGGNSNSWPOTG5E4DS2TOS",
			"TALICEW2K5Q6O5MQ3UK4TEW4ND7QSA4PFIBEXDK4",
			"TALICERWZAJZ33IDFCLS7H44ULQTDNMG5KU7Y4UL",
			"TALICE4AQNH5TE7O43RZ5FPJ3AC6HCFTSOO7B3GF",
			"TATHIESLV5OI35KOLL3GODH2ZGSRUAI4GTS7IEBO",
			"TBMAKOTAFIG5P4EYBO7XLPNNSKRUCYQOZPDW27UA",
			"TDPATMAMYAICKQ7SPFFE3TRTHYW2XF773VTTHYUI",
			"TDGIMREMR5NSRFUOMPI5OOHLDATCABNPC5ID2SVA",
			"TD2T562S4H3XT3QADUYYWEJ4EKJAGUARHGGOLHQQ",
			"TCLOITQWQ4KWWA6SEYUCG6VIVTXNH35LBCFYV4GN",
			"TCKRYSTAID2VC2ZW3MPM2FHKIFV2YZUJVMYPPP24");

	private static final List<KeyPair> KEY_PAIRS = PRIVATE_KEY_STRINGS.stream()
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
			if (encodedAddress.equalsIgnoreCase(address.getEncoded())) {
				return Address.fromEncoded(encodedAddress);
			}
		}

		throw new IllegalArgumentException(String.format("could not find %s", address));
	}
}