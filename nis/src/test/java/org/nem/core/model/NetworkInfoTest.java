package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class NetworkInfoTest {

	@Test
	public void mainNetworkInfoIsCorrect() {
		// Arrange:
		final NetworkInfo info = NetworkInfo.getMainNetworkInfo();

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x68));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('N'));
		Assert.assertThat(info.getNemesisAccountId().charAt(0), IsEqual.equalTo('N'));
	}

	@Test
	public void testNetworkInfoIsCorrect() {
		// Arrange:
		final NetworkInfo info = NetworkInfo.getTestNetworkInfo();

		// Assert:
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x98));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
		Assert.assertThat(info.getNemesisAccountId().charAt(0), IsEqual.equalTo('T'));
	}

	@Test
	public void defaultNetworkIsTestNetwork() {
		// Assert:
		Assert.assertThat(
				NetworkInfo.getDefault(),
				IsSame.sameInstance(NetworkInfo.getTestNetworkInfo()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void fromAddressThrowsIfEncodedAddressContainsInvalidCharacters() {
		// Arrange:
		final Address address = Address.fromEncoded("TAAAAAAAAAA1BBBBBBBBBCCCCCCCCCCDDDDDDDDD");

		// Assert:
		NetworkInfo.fromAddress(address);
	}

	@Test (expected = IllegalArgumentException.class)
	public void fromAddressThrowsIfEncodedAddressHasUnknownNetworkVersion() {
		// Arrange:
		final Address address = Address.fromEncoded("YAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD");

		// Assert:
		NetworkInfo.fromAddress(address);
	}

	@Test
	public void fromAddressReturnsTestNetworkInfoWhenGivenAValidTestNetAddress() {
		// Arrange:
		final Address address = Address.fromEncoded("TAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD");

		// Act:
		final NetworkInfo networkInfo = NetworkInfo.fromAddress(address);
		final NetworkInfo expectedNetworkInfo = NetworkInfo.getTestNetworkInfo();

		// Assert:
		assertIsSameNetworkInfo(networkInfo, expectedNetworkInfo);
	}

	@Test
	public void fromAddressReturnsMainNetworkInfoWhenGivenAValidMainNetAddress() {
		// Arrange:
		final Address address = Address.fromEncoded("NAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD");

		// Act:
		final NetworkInfo networkInfo = NetworkInfo.fromAddress(address);
		final NetworkInfo expectedNetworkInfo = NetworkInfo.getMainNetworkInfo();

		// Assert:
		assertIsSameNetworkInfo(networkInfo, expectedNetworkInfo);
	}

	private void assertIsSameNetworkInfo(final NetworkInfo networkInfo1, final NetworkInfo networkInfo2) {
		Assert.assertThat(networkInfo1.getVersion(), IsEqual.equalTo(networkInfo2.getVersion()));
		Assert.assertThat(networkInfo1.getAddressStartChar(), IsEqual.equalTo(networkInfo2.getAddressStartChar()));
		Assert.assertThat(networkInfo1.getNemesisAccountId(), IsEqual.equalTo(networkInfo2.getNemesisAccountId()));
	}
}
