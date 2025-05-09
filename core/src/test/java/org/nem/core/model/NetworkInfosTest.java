package org.nem.core.model;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.ExceptionAssert;

@RunWith(Enclosed.class)
@SuppressWarnings("serial")
public class NetworkInfosTest {
	private static final Map<String, Address> DESC_TO_ADDRESS_MAP = new HashMap<String, Address>() {
		{
			this.put("NON_BASE32_CHARS", Address.fromEncoded("TAAAAAAAAAA1BBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("UNKNOWN_NETWORK", Address.fromEncoded("YAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("TEST_NETWORK", Address.fromEncoded("TAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("MAIN_NETWORK", Address.fromEncoded("NAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
			this.put("MIJIN_NETWORK", Address.fromEncoded("MAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDD"));
		}
	};

	private static final Map<String, Byte> DESC_TO_VERSION_MAP = new HashMap<String, Byte>() {
		{
			this.put("UNKNOWN_NETWORK", (byte) 0x50);
			this.put("TEST_NETWORK", (byte) 0x98);
			this.put("MAIN_NETWORK", (byte) 0x68);
			this.put("MIJIN_NETWORK", (byte) 0x60);
		}
	};
	private static final Map<String, String> DESC_TO_FRIENDLY_NAME_MAP = new HashMap<String, String>() {
		{
			this.put("UNKNOWN_NETWORK", "unknownnet");
			this.put("TEST_NETWORK", "testnet");
			this.put("MAIN_NETWORK", "mainnet");
			this.put("MIJIN_NETWORK", "mijinnet");
		}
	};

	// region default network

	public static class DefaultNetworkInfoTest {

		@After
		public void resetDefaultNetwork() {
			NetworkInfos.setDefault(null);
		}

		@Test
		public void defaultNetworkIsTestNetworkByDefault() {
			// Assert:
			MatcherAssert.assertThat(NetworkInfos.getDefault(), IsSame.sameInstance(NetworkInfos.getTestNetworkInfo()));
		}

		@Test
		public void defaultNetworkCanBeChangedToMainNetwork() {
			// Act:
			NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

			// Assert:
			MatcherAssert.assertThat(NetworkInfos.getDefault(), IsSame.sameInstance(NetworkInfos.getMainNetworkInfo()));
		}

		@Test
		public void defaultNetworkCanBeReset() {
			// Arrange:
			NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

			// Act:
			NetworkInfos.setDefault(null);

			// Assert:
			MatcherAssert.assertThat(NetworkInfos.getDefault(), IsSame.sameInstance(NetworkInfos.getTestNetworkInfo()));
		}

		@Test
		public void defaultNetworkCannotBeChangedAfterBeingSet() {
			// Arrange:
			NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());

			// Act:
			ExceptionAssert.assertThrows(v -> NetworkInfos.setDefault(NetworkInfos.getTestNetworkInfo()), IllegalStateException.class);
		}
	}

	// endregion

	// region unknown network

	public static class UnknownNetworkInfoTest {
		private final String unknownIdentifier = "UNKNOWN_NETWORK";

		@Test
		public void fromAddressThrowsIfEncodedAddressContainsInvalidCharacters() {
			// Arrange:
			final Address address = DESC_TO_ADDRESS_MAP.get("NON_BASE32_CHARS");

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromAddress(address), IllegalArgumentException.class);
		}

		@Test
		public void fromAddressThrowsIfEncodedAddressHasUnknownNetworkVersion() {
			// Arrange:
			final Address address = DESC_TO_ADDRESS_MAP.get(this.unknownIdentifier);

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromAddress(address), IllegalArgumentException.class);
		}

		@Test
		public void fromVersionThrowsIfGivenVersionIsAnUnknownNetworkVersion() {
			// Arrange:
			final byte version = DESC_TO_VERSION_MAP.get(this.unknownIdentifier);

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromVersion(version), IllegalArgumentException.class);
		}

		@Test
		public void fromFriendlyNameThrowsIfGivenFriendlyNameIsUnknown() {
			// Arrange:
			final String friendlyName = DESC_TO_FRIENDLY_NAME_MAP.get(this.unknownIdentifier);

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromFriendlyName(friendlyName), IllegalArgumentException.class);
		}

		@Test
		public void isKnownNetworkFriendlyNameReturnsFalseWhenNetworkNameIsUnknown() {
			// Arrange:
			final String friendlyName = DESC_TO_FRIENDLY_NAME_MAP.get(this.unknownIdentifier);

			// Act:
			final boolean isKnown = NetworkInfos.isKnownNetworkFriendlyName(friendlyName);

			// Assert:
			MatcherAssert.assertThat(isKnown, IsEqual.equalTo(false));
		}
	}

	// endregion

	private static abstract class AbstractNetworkInfoTest {
		private final String identifier;
		private final NetworkInfo networkInfo;

		protected AbstractNetworkInfoTest(final String identifier, final NetworkInfo networkInfo) {
			this.identifier = identifier;
			this.networkInfo = networkInfo;
		}

		@Test
		public void fromAddressReturnsNetworkIdenfitiedByNetworkAddress() {
			// Arrange:
			final Address address = DESC_TO_ADDRESS_MAP.get(this.identifier);

			// Act:
			final NetworkInfo networkInfo = NetworkInfos.fromAddress(address);

			// Assert:
			MatcherAssert.assertThat(networkInfo, IsSame.sameInstance(this.networkInfo));
		}

		@Test
		public void fromVersionReturnsNetworkIdenfitiedByNetworkVersion() {
			// Arrange:
			final byte version = DESC_TO_VERSION_MAP.get(this.identifier);

			// Act:
			final NetworkInfo networkInfo = NetworkInfos.fromVersion(version);

			// Assert:
			MatcherAssert.assertThat(networkInfo, IsSame.sameInstance(this.networkInfo));
		}

		@Test
		public void fromFriendlyNameReturnsNetworkIdentifiedByFriendlyName() {
			// Arrange:
			final String friendlyName = DESC_TO_FRIENDLY_NAME_MAP.get(this.identifier);

			// Act:
			final NetworkInfo networkInfo = NetworkInfos.fromFriendlyName(friendlyName);

			// Assert:
			MatcherAssert.assertThat(networkInfo, IsSame.sameInstance(this.networkInfo));
		}

		@Test
		public void isKnownNetworkFriendlyNameReturnsTrueWhenNetworkNameIsKnown() {
			// Arrange:
			final String friendlyName = DESC_TO_FRIENDLY_NAME_MAP.get(this.identifier);

			// Act:
			final boolean isKnown = NetworkInfos.isKnownNetworkFriendlyName(friendlyName);

			// Assert:
			MatcherAssert.assertThat(isKnown, IsEqual.equalTo(true));
		}

		@Test
		public void networkInfoIsOnlyCompatibleWithMatchingNetworkAddresses() {
			// Arrange:
			for (final Map.Entry<String, Address> entry : DESC_TO_ADDRESS_MAP.entrySet()) {
				// Act:
				final boolean isCompatible = this.networkInfo.isCompatible(entry.getValue());

				// Assert:
				MatcherAssert.assertThat(isCompatible, IsEqual.equalTo(this.identifier.equals(entry.getKey())));
			}
		}

		@Test
		public void networkInfoIsCorrect() {
			this.assertNetworkInfo(this.networkInfo);
		}

		protected abstract void assertNetworkInfo(final NetworkInfo networkInfo);
	}

	public static class MainNetworkInfoTest extends AbstractNetworkInfoTest {

		public MainNetworkInfoTest() {
			super("MAIN_NETWORK", NetworkInfos.getMainNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
			// Assert:
			final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
			final PublicKey expectedPublicKey = PublicKey.fromHexString("8d07f90fb4bbe7715fa327c926770166a11be2e494a970605f2e12557f66c9b9");
			MatcherAssert.assertThat(info.getVersion(), IsEqual.equalTo((byte) 0x68));
			MatcherAssert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('N'));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(9000000240L)));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis.bin"));
		}
	}

	public static class TestNetworkInfoTest extends AbstractNetworkInfoTest {

		public TestNetworkInfoTest() {
			super("TEST_NETWORK", NetworkInfos.getTestNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
			final Hash expectedGenerationHash = Hash.fromHexString("33496b75b6e5827cd11f50070df2dd38b31e20398b166cb719dd544d4844ed59");
			final PublicKey expectedPublicKey = PublicKey.fromHexString("d8e06b38d4ce227fe735eb64bec55d6b9708cf91bcbcbe7e09f36ffd8b97763d");
			MatcherAssert.assertThat(info.getVersion(), IsEqual.equalTo((byte) 0x98));
			MatcherAssert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(9000000000L)));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis-testnet.bin"));
		}
	}

	public static class MijinNetworkInfoTest extends AbstractNetworkInfoTest {

		public MijinNetworkInfoTest() {
			super("MIJIN_NETWORK", NetworkInfos.getMijinNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
			final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
			final PublicKey expectedPublicKey = PublicKey.fromHexString("57b4832d9232ee410e93d595207cffc2b9e9c5002472c4b0bb3bb10a4ce152e3");
			MatcherAssert.assertThat(info.getVersion(), IsEqual.equalTo((byte) 0x60));
			MatcherAssert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('M'));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(9000000000L)));
			MatcherAssert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis-mijinnet.bin"));
		}
	}
}
