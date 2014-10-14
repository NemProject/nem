package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class AccountMetaDataTest {

	@Test
	public void canCreateAccountMetaData() {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.ACTIVE));
	}

	@Test
	public void canRoundTripAccountMetaData() {
		// Arrange:
		final AccountMetaData metaData = createRoundTrippedAccountMetaData(AccountStatus.LOCKED, AccountRemoteStatus.DEACTIVATING);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
		Assert.assertThat(metaData.getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.DEACTIVATING));
	}

	private static AccountMetaData createAccountMetaData(final AccountStatus status, final AccountRemoteStatus remoteStatus) {
		return new AccountMetaData(status, remoteStatus);
	}

	private static AccountMetaData createRoundTrippedAccountMetaData(final AccountStatus status, final AccountRemoteStatus remoteStatus) {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(status, remoteStatus);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new AccountMetaData(deserializer);
	}
}
