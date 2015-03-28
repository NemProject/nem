package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.Collectors;

public class NonEligibleHarvesterPrivateKeysTest {
	private static final int EXPECTED_SET_SIZE = 6 + 4 * 26;
	private static final PrivateKey SUSTAINABILITY_FUND = PrivateKey.fromHexString("d764f9c66fa558ef0292de82e3dad56eebecfda54a74518187ae748289369f69");
	private static final PrivateKey MARKETING_FUND = PrivateKey.fromHexString("75b9041a73845bb646ca7cebb631a5d927deb9f31e88fe09865c743cbb6f05b8");
	private static final PrivateKey OPERATIONAL_FUND = PrivateKey.fromHexString("a07630d53dae153165d851aeb13e605f7514c8d87c0e1b39e522ab9bb68521cc");
	private static final PrivateKey DEVELOPER_PRE_V1_FUND = PrivateKey.fromHexString("89ff78fd5bd1ca43d9245b67b2ef2acb74c6328401bccbac269e621c019b8414");
	private static final PrivateKey DEVELOPER_POST_V1_FUND = PrivateKey.fromHexString("02691be329fdec69b0e298ba3638a352d1855187f92b76f6be25ba93bc0201b3");
	private static final PrivateKey CONTRIBUTOR_FUND = PrivateKey.fromHexString("5d059caa488fabccfa362df83245296386cda75a0115c258b9d4876d8c7b7163");
	private static final List<PrivateKey> CORE_FUNDS = Arrays.asList(
			SUSTAINABILITY_FUND,
			MARKETING_FUND,
			OPERATIONAL_FUND,
			DEVELOPER_PRE_V1_FUND,
			DEVELOPER_POST_V1_FUND,
			CONTRIBUTOR_FUND);

	@Test
	public void setSizesMatchExpectedValue() {
		// Assert:
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPrivateKeys().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPublicKeys().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
	}

	@Test
	public void existingFundsAtLaunchAreNonEligibleForHarvesting() {
		// Assert:
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(SUSTAINABILITY_FUND), IsEqual.equalTo(false));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(MARKETING_FUND), IsEqual.equalTo(false));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(OPERATIONAL_FUND), IsEqual.equalTo(false));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(DEVELOPER_PRE_V1_FUND), IsEqual.equalTo(false));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(DEVELOPER_POST_V1_FUND), IsEqual.equalTo(false));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(CONTRIBUTOR_FUND), IsEqual.equalTo(false));
	}

	@Test
	public void additionalAddressesStartWithExpectedPrefixes() {
		// Arrange:
		final String firstAddressChar = Character.toString(NetworkInfos.getDefault().getAddressStartChar());
		final List<String> expectedPrefixes = new ArrayList<>();
		for (int c = 65; c < 65 + 26; c++) {
			expectedPrefixes.add(firstAddressChar + "AFUND" + Character.toString((char)c));
			expectedPrefixes.add(firstAddressChar + "BFUND" + Character.toString((char)c));
			expectedPrefixes.add(firstAddressChar + "CFUND" + Character.toString((char)c));
			expectedPrefixes.add(firstAddressChar + "DFUND" + Character.toString((char)c));
		}

		// Act:
		final Set<String> addressPrefixes = NonEligibleHarvesterPrivateKeys.getPrivateKeys().stream()
				.filter(privateKey -> !CORE_FUNDS.contains(privateKey))
				.map(privateKey -> new KeyPair(privateKey).getPublicKey())
				.map(publicKey -> Address.fromPublicKey(publicKey).getEncoded().substring(0, 7))
				.collect(Collectors.toSet());

		// Assert:
		Assert.assertThat(addressPrefixes.size(), IsEqual.equalTo(expectedPrefixes.size()));
		Assert.assertThat(addressPrefixes, IsEquivalent.equivalentTo(expectedPrefixes));
	}

	@Test
	public void publicKeySetCanBeDerivedFromPrivateKeySet() {
		// Arrange:
		final Set<PublicKey> derivedPublicKeys = NonEligibleHarvesterPrivateKeys.getPrivateKeys().stream()
				.map(privateKey -> new KeyPair(privateKey).getPublicKey())
				.collect(Collectors.toSet());

		// Act:
		final Set<PublicKey> publicKeys = NonEligibleHarvesterPrivateKeys.getPublicKeys();

		// Assert:
		Assert.assertThat(publicKeys.size(), IsEqual.equalTo(derivedPublicKeys.size()));
		Assert.assertThat(publicKeys, IsEquivalent.equivalentTo(derivedPublicKeys));
	}

	@Test
	public void isEligiblePrivateKeyReturnsTrueForPrivateKeyNotInTheSet() {
		// Act:
		final boolean isEligible = NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(new KeyPair().getPrivateKey());

		// Assert:
		Assert.assertThat(isEligible, IsEqual.equalTo(true));
	}

	@Test
	public void isEligiblePublicKeyReturnsTrueForPublicKeyNotInTheSet() {
		// Act:
		final boolean isEligible = NonEligibleHarvesterPrivateKeys.isEligiblePublicKey(new KeyPair().getPublicKey());

		// Assert:
		Assert.assertThat(isEligible, IsEqual.equalTo(true));
	}
}
