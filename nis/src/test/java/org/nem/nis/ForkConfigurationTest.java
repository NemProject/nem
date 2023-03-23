package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.NemProperties;
import org.nem.core.model.NetworkInfo;
import org.nem.core.model.NetworkInfos;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ForkConfigurationTest {
	// region test helpers

	private static void canParseWithValidHashes(final String separator, final String prefix, String postfix) {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("nis.treasuryReissuanceForkHeight", "2345");

		final String[] hashStrings = new String[]{
				"19E0E3B991FAD3D24312A9E99D04F25C04BA3806A4F1F827B52BE403B1F6ADB9",
				"B9741D02EADFCBD714EE36B727F09F08FBA2A3744AFB3805B13E5CCE4D434AA1",
				"2F89C4126DD94C7FA29874EE12B1E9F1E51B5F89DA0445551AEE0C7BCEAD0B63"
		};
		final String[] fallbackHashStrings = new String[]{
				"959DB36769A4DA13D80CF8393EA8E3FDF7ADFAB618888BD825AE423D46B0D857",
				"3B5B803B1B8F88DB4B4BA79828CF677EAA5E7ED796409B47D93DBB38ED36FC73"
		};
		properties.setProperty("nis.treasuryReissuanceForkTransactionHashes",
				String.join("|", hashStrings[0], prefix + hashStrings[1] + postfix, hashStrings[2]));
		properties.setProperty("nis.treasuryReissuanceForkFallbackTransactionHashes", String.join("|", fallbackHashStrings));

		properties.setProperty("nis.multisigMOfNForkHeight", "1");
		properties.setProperty("nis.mosaicsForkHeight", "2");
		properties.setProperty("nis.feeForkHeight", "3");
		properties.setProperty("nis.remoteAccountForkHeight", "4");
		properties.setProperty("nis.mosaicRedefinitionForkHeight", "5");
		properties.setProperty("nis.secondFeeForkHeight", "6");

		// Act:
		final ForkConfiguration config = new ForkConfiguration.Builder(new NemProperties(properties)).build();

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(2345)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(hashStrings).map(Hash::fromHexString).collect(Collectors.toList())));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(fallbackHashStrings).map(Hash::fromHexString).collect(Collectors.toList())));

		MatcherAssert.assertThat(config.getmultisigMOfNForkHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(config.getMosaicsForkHeight(), IsEqual.equalTo(new BlockHeight(2)));
		MatcherAssert.assertThat(config.getFeeForkHeight(), IsEqual.equalTo(new BlockHeight(3)));
		MatcherAssert.assertThat(config.getRemoteAccountForkHeight(), IsEqual.equalTo(new BlockHeight(4)));
		MatcherAssert.assertThat(config.getMosaicRedefinitionForkHeight(), IsEqual.equalTo(new BlockHeight(5)));
		MatcherAssert.assertThat(config.getSecondFeeForkHeight(), IsEqual.equalTo(new BlockHeight(6)));
	}

	private static void cannotParseWithInvalidHashes(final String separator, final String hashString) {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("nis.treasuryReissuanceForkHeight", "2345");

		final String[] hashStrings = new String[]{
				"19E0E3B991FAD3D24312A9E99D04F25C04BA3806A4F1F827B52BE403B1F6ADB9", hashString,
				"2F89C4126DD94C7FA29874EE12B1E9F1E51B5F89DA0445551AEE0C7BCEAD0B63"
		};
		final String[] fallbackHashStrings = new String[]{
				"959DB36769A4DA13D80CF8393EA8E3FDF7ADFAB618888BD825AE423D46B0D857",
				"3B5B803B1B8F88DB4B4BA79828CF677EAA5E7ED796409B47D93DBB38ED36FC73"
		};
		properties.setProperty("nis.treasuryReissuanceForkTransactionHashes", String.join(separator, hashStrings));
		properties.setProperty("nis.treasuryReissuanceForkFallbackTransactionHashes", String.join("|", fallbackHashStrings));

		// Act + Assert
		ExceptionAssert.assertThrows(v -> new ForkConfiguration.Builder(new NemProperties(properties)).build(), CryptoException.class);
	}

	private static void canReadConfiguration(final int version, final Supplier<ForkConfiguration> configSupplier) {

		// Act:
		final ForkConfiguration config = configSupplier.get();

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));

		MatcherAssert.assertThat(config.getFeeForkHeight(), IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.FEE_FORK(version))));
		MatcherAssert.assertThat(config.getSecondFeeForkHeight(),
				IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.SECOND_FEE_FORK(version))));
		MatcherAssert.assertThat(config.getMosaicsForkHeight(),
				IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.MOSAICS_FORK(version))));
		MatcherAssert.assertThat(config.getmultisigMOfNForkHeight(),
				IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version))));
		MatcherAssert.assertThat(config.getRemoteAccountForkHeight(),
				IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.REMOTE_ACCOUNT_FORK(version))));
		MatcherAssert.assertThat(config.getMosaicRedefinitionForkHeight(),
				IsEqual.equalTo(new BlockHeight(BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(version))));
	}


	// endregion

	// region basic

	@Test
	public void canCreateDefaultConfiguration() {
		// Arrange:
		final int version = NetworkInfos.getDefault().getVersion() << 24;

		canReadConfiguration(version, () -> new ForkConfiguration.Builder().build());;
	}

	@Test
	public void canCreateCustomConfiguration() {
		// Arrange:
		final String[] hashStrings = new String[]{
				"19E0E3B991FAD3D24312A9E99D04F25C04BA3806A4F1F827B52BE403B1F6ADB9",
				"B9741D02EADFCBD714EE36B727F09F08FBA2A3744AFB3805B13E5CCE4D434AA1"
		};
		final String[] fallbackHashStrings = new String[]{
				"2F89C4126DD94C7FA29874EE12B1E9F1E51B5F89DA0445551AEE0C7BCEAD0B63"
		};
		final List<Hash> hashes = Arrays.stream(hashStrings).map(Hash::fromHexString).collect(Collectors.toList());
		final List<Hash> fallbackHashes = Arrays.stream(fallbackHashStrings).map(Hash::fromHexString).collect(Collectors.toList());

		// Act:
		final ForkConfiguration config = new ForkConfiguration.Builder().treasuryReissuanceForkHeight(new BlockHeight(1234))
				.treasuryReissuanceForkTransactionHashes(hashes).treasuryReissuanceForkFallbackTransactionHashes(fallbackHashes)
				.feeForkHeight(new BlockHeight(1)).mosaicsForkHeight(new BlockHeight(2)).multisigMOfNForkHeight(new BlockHeight(3))
				.remoteAccountForkHeight(new BlockHeight(4)).mosaicRedefinitionForkHeight(new BlockHeight(5))
				.secondFeeForkHeight(new BlockHeight(6)).build();

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(), IsEqual.equalTo(hashes));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(), IsEqual.equalTo(fallbackHashes));

		MatcherAssert.assertThat(config.getFeeForkHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(config.getMosaicsForkHeight(), IsEqual.equalTo(new BlockHeight(2)));
		MatcherAssert.assertThat(config.getmultisigMOfNForkHeight(), IsEqual.equalTo(new BlockHeight(3)));
		MatcherAssert.assertThat(config.getRemoteAccountForkHeight(), IsEqual.equalTo(new BlockHeight(4)));
		MatcherAssert.assertThat(config.getMosaicRedefinitionForkHeight(), IsEqual.equalTo(new BlockHeight(5)));
		MatcherAssert.assertThat(config.getSecondFeeForkHeight(), IsEqual.equalTo(new BlockHeight(6)));
	}

	@Test
	public void canReadDefaultConfiguration() {
		// Arrange:
		final int version = NetworkInfos.getDefault().getVersion() << 24;

		canReadConfiguration(version, () -> new ForkConfiguration.Builder(new NemProperties(new Properties())).build());;
	}

	@Test
	public void canReadMainnetConfiguration() {
		// Arrange:
		final NetworkInfo networkInfo = NetworkInfos.getMainNetworkInfo();
		final int version = networkInfo.getVersion() << 24;

		canReadConfiguration(version, () -> new ForkConfiguration.Builder(new NemProperties(new Properties()), networkInfo).build());
	}


	@Test
	public void canReadCustomConfiguration() {
		canParseWithValidHashes("|", "", "");
	}

	// endregion

	// region hashes parsing

	@Test
	public void hashesCanBeParsedWithWhitespace() {
		canParseWithValidHashes("|", "   ", "");
		canParseWithValidHashes("|", "", "   ");
		canParseWithValidHashes("|", "   ", "   ");
	}

	@Test
	public void hashesCannotBeParsedWithInvalidSeparator() {
		cannotParseWithInvalidHashes(",", "B9741D02EADFCBD714EE36B727F09F08FBA2A3744AFB3805B13E5CCE4D434AA1");
	}

	@Test
	public void hashesCannotBeParsedWithInvalidHash() {
		cannotParseWithInvalidHashes("|", "B9741D02EADFCBD714EE36B727F09F08FBA2A3744AFB3805B13E5CCE4D434AA$");
	}

	// endregion
}
