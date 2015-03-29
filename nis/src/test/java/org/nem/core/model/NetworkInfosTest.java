package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;
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

	private static final Map<String, Byte> DESC_TO_VERSION_MAP = new HashMap<String, Byte>() {
		{
			this.put("UNKNOWN_NETWORK", (byte)0x50);
			this.put("TEST_NETWORK", (byte)0x98);
			this.put("MAIN_NETWORK", (byte)0x68);
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
		final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
		final PublicKey expectedPublicKey = PublicKey.fromHexString("8d07f90fb4bbe7715fa327c926770166a11be2e494a970605f2e12557f66c9b9");
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x68));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('N'));
		Assert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
		Assert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		Assert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(9000000240L)));
		Assert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis.bin"));
	}

	@Test
	public void mainNetworkInfoIsOnlyCompatibleWithMainNetworkAddresses() {
		// Assert:
		assertNetworkMatches("MAIN_NETWORK", NetworkInfos.getMainNetworkInfo());
	}

	@Test
	public void testNetworkInfoIsCorrect() {
		// Arrange:
		final NetworkInfo info = NetworkInfos.getTestNetworkInfo();

		// Assert:
		final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
		final PublicKey expectedPublicKey = PublicKey.fromHexString("e59ef184a612d4c3c4d89b5950eb57262c69862b2f96e59c5043bf41765c482f");
		Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x98));
		Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
		Assert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
		Assert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		Assert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(8000000000L)));
		Assert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis-testnet.bin"));
	}

	@Test
	public void testNetworkInfoIsOnlyCompatibleWithTestNetworkAddresses() {
		// Assert:
		assertNetworkMatches("TEST_NETWORK", NetworkInfos.getTestNetworkInfo());
	}

	private static void assertNetworkMatches(final String description, final NetworkInfo info) {
		// Arrange:
		for (final Map.Entry<String, Address> entry : DESC_TO_ADDRESS_MAP.entrySet()) {
			// Act:
			final boolean isCompatible = info.isCompatible(entry.getValue());

			// Assert:
			Assert.assertThat(isCompatible, IsEqual.equalTo(description.equals(entry.getKey())));
		}
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
		// Assert:
		assertFromAddressReturnsNetwork("TEST_NETWORK", NetworkInfos.getTestNetworkInfo());
	}

	@Test
	public void fromAddressReturnsMainNetworkInfoWhenGivenAValidMainNetAddress() {
		// Assert:
		assertFromAddressReturnsNetwork("MAIN_NETWORK", NetworkInfos.getMainNetworkInfo());
	}

	private static void assertFromAddressReturnsNetwork(final String description, final NetworkInfo expectedNetworkInfo) {
		// Arrange:
		final Address address = DESC_TO_ADDRESS_MAP.get(description);

		// Act:
		final NetworkInfo networkInfo = NetworkInfos.fromAddress(address);

		// Assert:
		Assert.assertThat(networkInfo, IsSame.sameInstance(expectedNetworkInfo));
	}

	//endregion

	//region fromVersion

	@Test(expected = IllegalArgumentException.class)
	public void fromVersionThrowsIfGivenVersionIsAnUnknownNetworkVersion() {
		// Arrange:
		final byte version = DESC_TO_VERSION_MAP.get("UNKNOWN_NETWORK");

		// Assert:
		NetworkInfos.fromVersion(version);
	}

	@Test
	public void fromVersionReturnsTestNetworkInfoWhenGivenTestNetworkVersion() {
		// Assert:
		assertFromVersionReturnsNetwork("TEST_NETWORK", NetworkInfos.getTestNetworkInfo());
	}

	@Test
	public void fromVersionReturnsMainNetworkInfoWhenGivenMainNetworkVersion() {
		// Assert:
		assertFromVersionReturnsNetwork("MAIN_NETWORK", NetworkInfos.getMainNetworkInfo());
	}

	private static void assertFromVersionReturnsNetwork(final String description, final NetworkInfo expectedNetworkInfo) {
		// Arrange:
		final byte version = DESC_TO_VERSION_MAP.get(description);

		// Act:
		final NetworkInfo networkInfo = NetworkInfos.fromVersion(version);

		// Assert:
		Assert.assertThat(networkInfo, IsSame.sameInstance(expectedNetworkInfo));
	}

	//endregion
}
