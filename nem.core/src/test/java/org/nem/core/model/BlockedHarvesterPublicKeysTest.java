package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;
import java.util.stream.Collectors;

public class BlockedHarvesterPublicKeysTest {
	private static final int EXPECTED_SET_SIZE = 6 + 4 * 26;

	private static final List<PublicKey> CORE_FUNDS = Arrays.asList(
			PublicKey.fromHexString("b74e3914b13cb742dfbceef110d85bad14bd3bb77051a08be93c0f8a0651fde2"),
			PublicKey.fromHexString("f41b99320549741c5cce42d9e4bb836d98c50ed5415d0c3c2912d1bb50e6a0e5"),
			PublicKey.fromHexString("6f4dc5fa7b531f56543a4b567fefea2fcf080be71af73aca9925968eeee2ad0f"),
			PublicKey.fromHexString("6c8267a0550072a7b89e79bd91cf621862a36bc41d4463f9fcf416759b5dd801"),
			PublicKey.fromHexString("56a7ae8caca7356fffe98e1dfdf3f4218bb837b5ec6aae927a964e2ff1861e20"),
			PublicKey.fromHexString("1ef1ba8f753b4931bfceab0ad6a08892f1735f304f6c33ce04b41d66fa10cb4b"));

	@Test
	public void setSizesMatchExpectedValue() {
		// Assert:
		Assert.assertThat(BlockedHarvesterPublicKeys.getAll().size(), IsEqual.equalTo(EXPECTED_SET_SIZE));
	}

	@Test
	public void existingFundsAtLaunchAreBlockedFromHarvesting() {
		// Assert:
		for (final PublicKey coreFundPublicKey : CORE_FUNDS) {
			Assert.assertThat(BlockedHarvesterPublicKeys.contains(coreFundPublicKey), IsEqual.equalTo(true));
		}
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
		final Set<String> addressPrefixes = BlockedHarvesterPublicKeys.getAll().stream()
				.filter(publicKey -> !CORE_FUNDS.contains(publicKey))
				.map(publicKey -> Address.fromPublicKey(publicKey).getEncoded().substring(0, 7))
				.collect(Collectors.toSet());

		// Assert:
		Assert.assertThat(addressPrefixes.size(), IsEqual.equalTo(expectedPrefixes.size()));
		Assert.assertThat(addressPrefixes, IsEquivalent.equivalentTo(expectedPrefixes));
	}

	@Test
	public void containsReturnsFalseForPublicKeyNotInTheSet() {
		// Act:
		final boolean isContained = BlockedHarvesterPublicKeys.contains(new KeyPair().getPublicKey());

		// Assert:
		Assert.assertThat(isContained, IsEqual.equalTo(false));
	}
}
