package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;

import java.util.*;

public class NonEligibleHarvesterPrivateKeysTest {
	private static final int EXPECTED_SET_SIZE = 6 + 4 * 26 - 1;
	private static final PrivateKey SUSTAINABILITY_FUND = PrivateKey.fromHexString("d764f9c66fa558ef0292de82e3dad56eebecfda54a74518187ae748289369f69");
	private static final PrivateKey MARKETING_FUND = PrivateKey.fromHexString("75b9041a73845bb646ca7cebb631a5d927deb9f31e88fe09865c743cbb6f05b8");
	private static final PrivateKey OPERATIONAL_FUND = PrivateKey.fromHexString("a07630d53dae153165d851aeb13e605f7514c8d87c0e1b39e522ab9bb68521cc");
	private static final PrivateKey DEVELOPER_PRE_V1_FUND = PrivateKey.fromHexString("89ff78fd5bd1ca43d9245b67b2ef2acb74c6328401bccbac269e621c019b8414");
	private static final PrivateKey DEVELOPER_POST_V1_FUND = PrivateKey.fromHexString("02691be329fdec69b0e298ba3638a352d1855187f92b76f6be25ba93bc0201b3");
	private static final PrivateKey CONTRIBUTOR_FUND = PrivateKey.fromHexString("5d059caa488fabccfa362df83245296386cda75a0115c258b9d4876d8c7b7163");

	@Before
	public void initNetwork() {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
	}

	@Test
	public void setSizesMatchExpectedValue() {
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPrivateKeys().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPublicKeys().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.getAddresses().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
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
		final Set<Address> addresses = NonEligibleHarvesterPrivateKeys.getAddresses();
		final List<String> expectedPrefixes = new ArrayList<>();
		for (int c = 65; c < 65 + 26; c++) {
			expectedPrefixes.add("NAFUND" + Character.toString ((char) c));
			expectedPrefixes.add("NBFUND" + Character.toString ((char) c));
			expectedPrefixes.add("NCFUND" + Character.toString ((char) c));
			expectedPrefixes.add("NDFUND" + Character.toString ((char) c));
		}

		expectedPrefixes.remove("NCFUNDC");

		// Assert:
		expectedPrefixes.stream().forEach(prefix -> {
			Assert.assertThat(addresses.stream().anyMatch(a -> a.getEncoded().startsWith(prefix)), IsEqual.equalTo(true));
		});
	}

	@Test
	public void publicKeySetCanBeDerivedFromPrivateKeySet() {
		// Assert:
		NonEligibleHarvesterPrivateKeys.getPublicKeys().forEach(pub -> {
			Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPrivateKeys().stream()
					.anyMatch(priv -> new KeyPair(priv).getPublicKey().equals(pub)), IsEqual.equalTo(true));
		});
	}

	@Test
	public void addressSetCanBeDerivedFromPublicKeySet() {
		// Assert:
		NonEligibleHarvesterPrivateKeys.getAddresses().forEach(address -> {
			Assert.assertThat(NonEligibleHarvesterPrivateKeys.getPublicKeys().stream()
					.anyMatch(pub -> Address.fromPublicKey(pub).equals(address)), IsEqual.equalTo(true));
		});
	}

	@Test
	public void isEligiblePrivateKeyReturnsTrueForPrivateKeyWhichIsNotInTheSet() {
		// Assert:
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePrivateKey(new KeyPair().getPrivateKey()), IsEqual.equalTo(true));
	}

	@Test
	public void isEligiblePublicKeyReturnsTrueForPublicKeyWhichIsNotInTheSet() {
		// Assert:
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligiblePublicKey(new KeyPair().getPublicKey()), IsEqual.equalTo(true));
	}

	@Test
	public void isEligibleAddressReturnsTrueForAddressWhichIsNotInTheSet() {
		// Assert:
		Assert.assertThat(NonEligibleHarvesterPrivateKeys.isEligibleAddress(Utils.generateRandomAddress()), IsEqual.equalTo(true));
	}
}
