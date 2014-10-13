package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class AccountMetaDataPairTest {

	@Test
	public void canCreateAccountMetaDataPair() {
		// Arrange:
		final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		final AccountMetaData metaData = new AccountMetaData(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE);
		final AccountMetaDataPair entity = new AccountMetaDataPair(accountInfo, metaData);

		// Assert:
		Assert.assertThat(entity.getAccount(), IsSame.sameInstance(accountInfo));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripAccountMetaDataPair() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Act:
		final AccountMetaDataPair metaDataPair = createRoundTrippedPair(address, AccountStatus.LOCKED, AccountRemoteStatus.ACTIVATED);

		// Assert:
		Assert.assertThat(metaDataPair.getAccount().getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(metaDataPair.getMetaData().getStatus(), IsEqual.equalTo(AccountStatus.LOCKED));
		Assert.assertThat(metaDataPair.getMetaData().getRemoteStatus(), IsEqual.equalTo(AccountRemoteStatus.ACTIVATED));
	}

	private static AccountMetaDataPair createRoundTrippedPair(
			final Address address,
			final AccountStatus status,
			final AccountRemoteStatus remoteStatus) {
		// Arrange:
		final AccountMetaData metaData = new AccountMetaData(status, remoteStatus);
		final AccountMetaDataPair metaDataPair = new AccountMetaDataPair(
				new AccountInfo(address, Amount.ZERO, BlockAmount.ZERO, null, 0.0),
				metaData);

		// Act:
		return new AccountMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, null));
	}
}
