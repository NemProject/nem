package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import java.util.*;

public class AccountMetaDataTest {

	//region create

	@Test
	public void canCreateAccountMetaDataWithNeitherCosignatoriesNorCosignatoriesOf() {
		// Assert:
		assertCanCreateAccountMetaData(new ArrayList<>(), new ArrayList<>());
	}

	@Test
	public void canCreateAccountMetaDataWithCosignatoriesOfButNotCosignatories() {
		// Assert:
		assertCanCreateAccountMetaData(createAccountInfos(), new ArrayList<>());
	}

	@Test
	public void canCreateAccountMetaDataWithCosignatoriesButNotCosignatoriesOf() {
		// Assert:
		assertCanCreateAccountMetaData(new ArrayList<>(), createAccountInfos());
	}

	@Test
	public void canCreateAccountMetaDataWithBothCosignatoriesAndCosignatoriesOf() {
		// Assert:
		assertCanCreateAccountMetaData(createAccountInfos(), createAccountInfos());
	}

	private static void assertCanCreateAccountMetaData(
			final List<AccountInfo> multisigAccounts,
			final List<AccountInfo> cosignatoryAccounts) {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(
				AccountStatus.UNLOCKED,
				AccountRemoteStatus.ACTIVE,
				multisigAccounts,
				cosignatoryAccounts);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.ACTIVE));

		if (multisigAccounts.isEmpty()) {
			Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(true));
		} else {
			Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(false));
			assertAccountInfos(metaData.getCosignatoryOf(), multisigAccounts);
		}

		if (cosignatoryAccounts.isEmpty()) {
			Assert.assertThat(metaData.getCosignatories().isEmpty(), IsEqual.equalTo(true));
		} else {
			Assert.assertThat(metaData.getCosignatories().isEmpty(), IsEqual.equalTo(false));
			assertAccountInfos(metaData.getCosignatories(), cosignatoryAccounts);
		}
	}

	//endregion

	//region roundtrip

	@Test
	public void canRoundTripAccountMetaDataWithNeitherCosignatoriesNorCosignatoriesOf() {
		// Assert:
		assertCanRoundTrip(new ArrayList<>(), new ArrayList<>());
	}

	@Test
	public void canRoundTripAccountMetaDataWithCosignatoriesOfButNotCosignatories() {
		// Assert:
		assertCanRoundTrip(createAccountInfos(), new ArrayList<>());
	}

	@Test
	public void canRoundTripAccountMetaDataWithCosignatoriesButNotCosignatoriesOf() {
		// Assert:
		assertCanRoundTrip(new ArrayList<>(), createAccountInfos());
	}

	@Test
	public void canRoundTripAccountMetaDataWithBothCosignatoriesAndCosignatoriesOf() {
		// Assert:
		assertCanRoundTrip(createAccountInfos(), createAccountInfos());
	}

	private static void assertCanRoundTrip(
			final List<AccountInfo> multisigAccounts,
			final List<AccountInfo> cosignatoryAccounts) {
		// Arrange:
		final AccountMetaData metaData = createRoundTrippedAccountMetaData(
				createAccountMetaData(
						AccountStatus.LOCKED,
						AccountRemoteStatus.DEACTIVATING,
						multisigAccounts,
						cosignatoryAccounts));

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.DEACTIVATING));

		if (multisigAccounts.isEmpty()) {
			Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(true));
		} else {
			Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(false));
			assertAccountInfos(metaData.getCosignatoryOf(), multisigAccounts);
		}

		if (cosignatoryAccounts.isEmpty()) {
			Assert.assertThat(metaData.getCosignatories().isEmpty(), IsEqual.equalTo(true));
		} else {
			Assert.assertThat(metaData.getCosignatories().isEmpty(), IsEqual.equalTo(false));
			assertAccountInfos(metaData.getCosignatories(), cosignatoryAccounts);
		}
	}

	//endregion

	private static AccountMetaData createAccountMetaData(
			final AccountStatus status,
			final AccountRemoteStatus remoteStatus,
			final List<AccountInfo> multisigAccounts,
			final List<AccountInfo> cosignatoryAccounts) {
		return new AccountMetaData(status, remoteStatus, multisigAccounts, cosignatoryAccounts);
	}

	private static AccountMetaData createRoundTrippedAccountMetaData(final AccountMetaData metaData) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new AccountMetaData(deserializer);
	}

	private static List<AccountInfo> createAccountInfos() {
		return Arrays.asList(
				new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(123), Amount.fromNem(111), new BlockAmount(234), "account1", 0.1),
				new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(345), Amount.fromNem(333), new BlockAmount(456), "account2", 0.2));
	}

	private static void assertAccountInfos(final List<AccountInfo> actual, final List<AccountInfo> expected) {
		Assert.assertThat(actual.size(), IsEqual.equalTo(expected.size()));
		for (int i = 0; i < actual.size(); ++i) {
			assertAccountInfo(actual.get(i), expected.get(i));
		}
	}

	private static void assertAccountInfo(final AccountInfo actual, final AccountInfo expected) {
		Assert.assertThat(actual.getAddress(), IsEqual.equalTo(expected.getAddress()));
		Assert.assertThat(actual.getBalance(), IsEqual.equalTo(expected.getBalance()));
		Assert.assertThat(actual.getVestedBalance(), IsEqual.equalTo(expected.getVestedBalance()));
		Assert.assertThat(actual.getNumHarvestedBlocks(), IsEqual.equalTo(expected.getNumHarvestedBlocks()));
		Assert.assertThat(actual.getLabel(), IsEqual.equalTo(expected.getLabel()));
		Assert.assertThat(actual.getImportance(), IsEqual.equalTo(expected.getImportance()));
	}
}
