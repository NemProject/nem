package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class AccountMetaDataPairTest {

	@Test
	public void canCreateAccountMetaDataPair() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final AccountMetaData metaData = new AccountMetaData(AccountStatus.UNLOCKED);
		final AccountMetaDataPair entity = new AccountMetaDataPair(account, metaData);

		// Assert:
		Assert.assertThat(entity.getAccount(), IsSame.sameInstance(account));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripAccountMetaDataPair() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		final AccountMetaDataPair metaDataPair = createRoundTrippedPair(account, AccountStatus.LOCKED);

		// Assert:
		Assert.assertThat(metaDataPair.getAccount(), IsEqual.equalTo(account));
		Assert.assertThat(metaDataPair.getMetaData().getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
	}

	private static AccountMetaDataPair createRoundTrippedPair(
			final Account account,
			final AccountStatus status) {
		// Arrange:
		final AccountMetaData metaData = new AccountMetaData(status);
		final AccountMetaDataPair metaDataPair = new AccountMetaDataPair(account, metaData);

		// Act:
		return new AccountMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, null));
	}
}
