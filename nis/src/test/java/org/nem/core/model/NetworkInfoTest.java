package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

import java.util.*;

public class NetworkInfoTest {

	@Test
	public void canCreateNetworkInfo() {
		// Arrange:
		final NetworkInfo info = new NetworkInfo((byte)0x55, 'Z', "Z Nemesis Address");

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x55));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('Z'));
		Assert.assertThat(info.getNemesisAddress(), IsEqual.equalTo("Z Nemesis Address"));
	}

	@Test
	public void isCompatibleOnlyReturnsTrueForCompatibleAddresses() {
		// Arrange:
		final Map<String, Address> descToAddressMap = new HashMap<String, Address>() {
			{
				this.put("NON_BASE32_CHARS", Address.fromEncoded("TAAAAAAAAAA1BBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
				this.put("UNKNOWN_NETWORK", Address.fromEncoded("YAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
				this.put("COMPATIBLE", Address.fromEncoded("ZAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
				this.put("NON_COMPATIBLE", Address.fromEncoded("NAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			}
		};

		final NetworkInfo info = NetworkInfos.getMainNetworkInfo();

		// Assert:
		Assert.assertThat(info.isCompatible(descToAddressMap.get("NON_BASE32_CHARS")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("UNKNOWN_NETWORK")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("COMPATIBLE")), IsEqual.equalTo(true));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("NON_COMPATIBLE")), IsEqual.equalTo(false));
	}
}
