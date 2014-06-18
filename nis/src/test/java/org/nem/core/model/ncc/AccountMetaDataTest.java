package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.AccountStatus;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class AccountMetaDataTest {

	@Test
	public void canCreateAccountMetaData() {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(AccountStatus.UNLOCKED);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.UNLOCKED));
	}

	@Test
	public void canRoundTripAccountMetaData() {
		// Arrange:
		final AccountMetaData metaData = createRoundTrippedAccountMetaData(AccountStatus.LOCKED);

		// Assert:
		Assert.assertThat(metaData.getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
	}

	private static AccountMetaData createAccountMetaData(AccountStatus status) {
		return new AccountMetaData(status);
	}

	private static AccountMetaData createRoundTrippedAccountMetaData(final AccountStatus status) {
		// Arrange:
		final AccountMetaData metaData = createAccountMetaData(status);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(metaData, null);
		return new AccountMetaData(deserializer);
	}
}
