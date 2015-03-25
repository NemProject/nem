package org.nem.core;

import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;

import java.util.ArrayList;

public class VanityGenerator {

	// TODO 20150325 - if we keep this, i'd move it to NemesisBlockCreator
	@Before
	public void initNetwork() {
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
	}

	@Test
	public void findAddresses() {
		ArrayList<String> addresses = new ArrayList<String>();
		addresses.add("NACONTR");
		addresses.add("NBCONTR");
		addresses.add("NCONTR");
		addresses.add("NDCONTR");
		addresses.add("NAMARKE");
		addresses.add("NBMARKE");
		addresses.add("NCMARKE");
		addresses.add("NDMARKE");
		addresses.add("NASUSTAI");
		addresses.add("NBSUSTAI");
		addresses.add("NCSUSTAI");
		addresses.add("NDSUSTAI");
		addresses.add("NAOPERAT");
		addresses.add("NBOPERAT");
		addresses.add("NCOPERAT");
		addresses.add("NDOPERAT");
		try {
			while (true) {
				final KeyPair keyPair= new KeyPair();
				final Address address = Address.fromPublicKey(keyPair.getPublicKey());
				for (String partialAddress : addresses) {
					if (address.getEncoded().startsWith(partialAddress)) {
						System.out.println(keyPair.getPrivateKey().toString() + " : " + keyPair.getPublicKey().toString() + " : " + address.getEncoded());
					}
				}
			}
		} catch(Exception e) {
		}
	}
}
