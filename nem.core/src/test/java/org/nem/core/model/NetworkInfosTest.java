package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

@RunWith(Enclosed.class)
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
			this.put("UNKNOWN_NETWORK", (byte)0x50);
			this.put("TEST_NETWORK", (byte)0x98);
			this.put("MAIN_NETWORK", (byte)0x68);
			this.put("MIJIN_NETWORK", (byte)0x60);
		}
	};

	//region default network

	public static class DefaultNetworkInfoTest {

		@After
		public void resetDefaultNetwork() {
			NetworkInfos.setDefault(null);
		}

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
	}

	//endregion

	//region unknown network

	public static class UnknownNetworkInfoTest {

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
			final Address address = DESC_TO_ADDRESS_MAP.get("UNKNOWN_NETWORK");

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromAddress(address), IllegalArgumentException.class);
		}

		@Test
		public void fromVersionThrowsIfGivenVersionIsAnUnknownNetworkVersion() {
			// Arrange:
			final byte version = DESC_TO_VERSION_MAP.get("UNKNOWN_NETWORK");

			// Assert:
			ExceptionAssert.assertThrows(v -> NetworkInfos.fromVersion(version), IllegalArgumentException.class);
		}
	}

	//endregion

	private static abstract class AbstractNetworkInfoTest {
		private final String identifier;
		private final String friendlyName;
		private final NetworkInfo networkInfo;

		protected AbstractNetworkInfoTest(final String identifier, final String friendlyName, final NetworkInfo networkInfo) {
			this.identifier = identifier;
			this.friendlyName = friendlyName;
			this.networkInfo = networkInfo;
		}

		@Test
		public void fromAddressReturnsNetworkIdenfitiedByNetworkAddress() {
			// Arrange:
			final Address address = DESC_TO_ADDRESS_MAP.get(this.identifier);

			// Act:
			final NetworkInfo networkInfo = NetworkInfos.fromAddress(address);

			// Assert:
			Assert.assertThat(networkInfo, IsSame.sameInstance(this.networkInfo));
		}

		@Test
		public void fromVersionReturnsNetworkIdenfitiedByNetworkVersion() {
			// Arrange:
			final byte version = DESC_TO_VERSION_MAP.get(this.identifier);

			// Act:
			final NetworkInfo networkInfo = NetworkInfos.fromVersion(version);

			// Assert:
			Assert.assertThat(networkInfo, IsSame.sameInstance(this.networkInfo));
		}

		@Test
		public void networkInfoIsOnlyCompatibleWithMatchingNetworkAddresses() {
			// Arrange:
			for (final Map.Entry<String, Address> entry : DESC_TO_ADDRESS_MAP.entrySet()) {
				// Act:
				final boolean isCompatible = this.networkInfo.isCompatible(entry.getValue());

				// Assert:
				Assert.assertThat(isCompatible, IsEqual.equalTo(this.identifier.equals(entry.getKey())));
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
			super("MAIN_NETWORK", "mainnet", NetworkInfos.getMainNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
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
	}

	public static class TestNetworkInfoTest extends AbstractNetworkInfoTest {

		public TestNetworkInfoTest() {
			super("TEST_NETWORK", "testnet", NetworkInfos.getTestNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
			final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
			final PublicKey expectedPublicKey = PublicKey.fromHexString("e59ef184a612d4c3c4d89b5950eb57262c69862b2f96e59c5043bf41765c482f");
			Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x98));
			Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
			Assert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
			Assert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
			Assert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(8000000000L)));
			Assert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis-testnet.bin"));
		}
	}

	public static class MijinNetworkInfoTest extends AbstractNetworkInfoTest {

		public MijinNetworkInfoTest() {
			super("MIJIN_NETWORK", "mijin", NetworkInfos.getMijinNetworkInfo());
		}

		@Override
		protected void assertNetworkInfo(final NetworkInfo info) {
			final Hash expectedGenerationHash = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");
			final PublicKey expectedPublicKey = PublicKey.fromHexString("57b4832d9232ee410e93d595207cffc2b9e9c5002472c4b0bb3bb10a4ce152e3");
			Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x60));
			Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('M'));
			Assert.assertThat(info.getNemesisBlockInfo().getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
			Assert.assertThat(info.getNemesisBlockInfo().getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
			Assert.assertThat(info.getNemesisBlockInfo().getAmount(), IsEqual.equalTo(Amount.fromNem(9000000000L)));
			Assert.assertThat(info.getNemesisBlockInfo().getDataFileName(), IsEqual.equalTo("nemesis-mijinnet.bin"));
		}
	}
}
