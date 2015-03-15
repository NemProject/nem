package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class NetworkInfosTest {
	private static final Map<String, Address> DESC_TO_ADDRESS_MAP = new HashMap<String, Address>() {
		{
			this.put("NON_BASE32_CHARS", Address.fromEncoded("TAAAAAAAAAA1BBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("UNKNOWN_NETWORK", Address.fromEncoded("YAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("TEST_NETWORK", Address.fromEncoded("TAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("MAIN_NETWORK", Address.fromEncoded("NAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
		}
	};

	@After
	public void resetDefaultNetwork() {
		NetworkInfos.setDefault(null);
	}

	//region network constants

	@Test
	public void mainNetworkInfoIsCorrect() {
		// Arrange:
		final NetworkInfo info = NetworkInfos.getMainNetworkInfo();

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x68));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('N'));
		Assert.assertThat(info.getNemesisAddress().charAt(0), IsEqual.equalTo('N'));
	}

	@Test
	public void mainNetworkInfoIsOnlyCompatibleWithMainNetworkAddresses() {
		// Arrange:
		final NetworkInfo info = NetworkInfos.getMainNetworkInfo();

		// Assert:
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("NON_BASE32_CHARS")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("UNKNOWN_NETWORK")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("TEST_NETWORK")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("MAIN_NETWORK")), IsEqual.equalTo(true));
	}

	@Test
	public void testNetworkInfoIsCorrect() {
		// Arrange:
		final NetworkInfo info = NetworkInfos.getTestNetworkInfo();

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x98));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
		Assert.assertThat(info.getNemesisAddress().charAt(0), IsEqual.equalTo('T'));
	}

	@Test
	public void testNetworkInfoIsOnlyCompatibleWithTestNetworkAddresses() {
		// Arrange:
		final NetworkInfo info = NetworkInfos.getTestNetworkInfo();

		// Assert:
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("NON_BASE32_CHARS")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("UNKNOWN_NETWORK")), IsEqual.equalTo(false));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("TEST_NETWORK")), IsEqual.equalTo(true));
		Assert.assertThat(info.isCompatible(DESC_TO_ADDRESS_MAP.get("MAIN_NETWORK")), IsEqual.equalTo(false));
	}

	//endregion

	//region default network

	@Test
	public void defaultNetworkIsTestNetworkByDefault() {
		// Assert:
		Assert.assertThat(
				NetworkInfos.getDefault(),
				IsSame.sameInstance(NetworkInfos.getTestNetworkInfo()));
	}

	@Test
	public void defaultNetworkCanBeChangedToMainNetwork() {
		// Act:
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

		// Assert:
		Assert.assertThat(
				NetworkInfos.getDefault(),
				IsSame.sameInstance(NetworkInfos.getMainNetworkInfo()));
	}

	@Test
	public void defaultNetworkCanBeReset() {
		// Arrange:
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

		// Act:
		NetworkInfos.setDefault(null);

		// Assert:
		Assert.assertThat(
				NetworkInfos.getDefault(),
				IsSame.sameInstance(NetworkInfos.getTestNetworkInfo()));
	}

	@Test
	public void defaultNetworkCannotBeChangedAfterBeingSet() {
		// Arrange:
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

		// Act:
		ExceptionAssert.assertThrows(
				v -> NetworkInfos.setDefault(NetworkInfos.getTestNetworkInfo()),
				IllegalStateException.class);
	}

	//endregion

	//region fromAddress

	@Test(expected = IllegalArgumentException.class)
	public void fromAddressThrowsIfEncodedAddressContainsInvalidCharacters() {
		// Arrange:
		final Address address = DESC_TO_ADDRESS_MAP.get("NON_BASE32_CHARS");

		// Assert:
		NetworkInfos.fromAddress(address);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromAddressThrowsIfEncodedAddressHasUnknownNetworkVersion() {
		// Arrange:
		final Address address = DESC_TO_ADDRESS_MAP.get("UNKNOWN_NETWORK");

		// Assert:
		NetworkInfos.fromAddress(address);
	}

	@Test
	public void fromAddressReturnsTestNetworkInfoWhenGivenAValidTestNetAddress() {
		// Arrange:
		final Address address = DESC_TO_ADDRESS_MAP.get("TEST_NETWORK");

		// Act:
		final NetworkInfo networkInfo = NetworkInfos.fromAddress(address);

		// Assert:
		Assert.assertThat(networkInfo, IsSame.sameInstance(NetworkInfos.getTestNetworkInfo()));
	}

	@Test
	public void fromAddressReturnsMainNetworkInfoWhenGivenAValidMainNetAddress() {
		// Arrange:
		final Address address = DESC_TO_ADDRESS_MAP.get("MAIN_NETWORK");

		// Act:
		final NetworkInfo networkInfo = NetworkInfos.fromAddress(address);

		// Assert:
		Assert.assertThat(networkInfo, IsSame.sameInstance(NetworkInfos.getMainNetworkInfo()));
	}

	//endregion
}
