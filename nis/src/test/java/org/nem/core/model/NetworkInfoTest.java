package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

import java.util.*;

public class NetworkInfoTest {

	@Test
	public void canCreateNetworkInfo() {
		// Arrange:
		final NemesisBlockInfo nemesisBlockInfo = createNemesisBlockInfo();
		final NetworkInfo info = new NetworkInfo((byte)0xC8, 'Z', nemesisBlockInfo);

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0xC8));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('Z'));
		Assert.assertThat(info.getNemesisBlockInfo(), IsEqual.equalTo(nemesisBlockInfo));
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

		final NetworkInfo info = new NetworkInfo((byte)0xC8, 'Z', createNemesisBlockInfo());

		// Assert:
		Assert.assertThat(info.isCompatible(descToAddressMap.get("NON_BASE32_CHARS")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("UNKNOWN_NETWORK")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("COMPATIBLE")), IsEqual.equalTo(true));
		Assert.assertThat(info.isCompatible(descToAddressMap.get("NON_COMPATIBLE")), IsEqual.equalTo(false));
	}

	private static NemesisBlockInfo createNemesisBlockInfo() {
		return new NemesisBlockInfo(Hash.ZERO, Utils.generateRandomAddress(), Amount.ZERO, "");
	}
}
