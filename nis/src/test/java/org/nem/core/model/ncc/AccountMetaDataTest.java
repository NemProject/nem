package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import java.util.*;

public class AccountMetaDataTest {

	@Test
	public void canCreateAccountMetaDataForNonCosignerAccount() {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.ACTIVE));
		Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canCreateAccountMetaDataForCosignerAccount() {
		// Arrange:
		final List<AccountInfo> multisigAccounts = createAccountInfos();
		final AccountMetaData metaData = createAccountMetaData(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE, multisigAccounts);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.ACTIVE));
		Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(false));
		assertAccountInfos(metaData.getCosignatoryOf(), multisigAccounts);
	}

	@Test
	public void canRoundTripAccountMetaDataForNonCosignerAccount() {
		// Arrange:
		final AccountMetaData metaData = createRoundTrippedAccountMetaData(
				createAccountMetaData(AccountStatus.LOCKED, AccountRemoteStatus.DEACTIVATING));

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.DEACTIVATING));
		Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void canRoundTripAccountMetaDataForCosignerAccount() {
		// Arrange:
		final List<AccountInfo> multisigAccounts = createAccountInfos();
		final AccountMetaData metaData = createRoundTrippedAccountMetaData(
				createAccountMetaData(AccountStatus.LOCKED, AccountRemoteStatus.DEACTIVATING, multisigAccounts));

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.DEACTIVATING));
		Assert.assertThat(metaData.getCosignatoryOf().isEmpty(), IsEqual.equalTo(false));
		assertAccountInfos(metaData.getCosignatoryOf(), multisigAccounts);
	}


	private static AccountMetaData createAccountMetaData(final AccountStatus status, final AccountRemoteStatus remoteStatus) {
		return createAccountMetaData(status, remoteStatus, new ArrayList<>());
	}

	private static AccountMetaData createAccountMetaData(
			final AccountStatus status,
			final AccountRemoteStatus remoteStatus,
			final List<AccountInfo> multisigAccounts) {
		return new AccountMetaData(status, remoteStatus, multisigAccounts);
	}

	private static AccountMetaData createRoundTrippedAccountMetaData(final AccountMetaData metaData) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new AccountMetaData(deserializer);
	}

	private static List<AccountInfo> createAccountInfos() {
		return Arrays.asList(
				new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(123), new BlockAmount(234), "account1", 0.1),
				new AccountInfo(Utils.generateRandomAddress(), Amount.fromNem(345), new BlockAmount(456), "account2", 0.2));
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
		Assert.assertThat(actual.getNumHarvestedBlocks(), IsEqual.equalTo(expected.getNumHarvestedBlocks()));
		Assert.assertThat(actual.getLabel(), IsEqual.equalTo(expected.getLabel()));
		Assert.assertThat(actual.getImportance(), IsEqual.equalTo(expected.getImportance()));
	}
}
