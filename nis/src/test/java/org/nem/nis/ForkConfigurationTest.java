package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.NemProperties;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

import java.util.*;
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

		// Act:
		final ForkConfiguration config = new ForkConfiguration(new NemProperties(properties));

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(2345)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(hashStrings).map(Hash::fromHexString).collect(Collectors.toList())));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(fallbackHashStrings).map(Hash::fromHexString).collect(Collectors.toList())));
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
		ExceptionAssert.assertThrows(v -> new ForkConfiguration(new NemProperties(properties)), CryptoException.class);
	}

	// endregion

	// region basic

	@Test
	public void canCreateDefaultConfiguration() {
		// Act:
		final ForkConfiguration config = new ForkConfiguration();

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));
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
		final ForkConfiguration config = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(1234)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(), IsEqual.equalTo(hashes));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(), IsEqual.equalTo(fallbackHashes));
	}

	@Test
	public void canReadDefaultConfiguration() {
		// Arrange:
		final Properties properties = new Properties();

		// Act:
		final ForkConfiguration config = new ForkConfiguration(new NemProperties(properties));

		// Assert:
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));
		MatcherAssert.assertThat(config.getTreasuryReissuanceForkFallbackTransactionHashes(), IsEqual.equalTo(new ArrayList<Hash>()));
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
