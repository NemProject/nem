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
		final AccountMetaData metaData = Mockito.mock(AccountMetaData.class);
		final AccountMetaDataPair entity = new AccountMetaDataPair(accountInfo, metaData);

		// Assert:
		Assert.assertThat(entity.getAccount(), IsSame.sameInstance(accountInfo));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripAccountMetaDataPair() {
		// Arrange:
		final AccountMetaDataTest.TestContext context = new AccountMetaDataTest.TestContext(AccountStatus.UNLOCKED, AccountRemoteStatus.ACTIVE);
		final Address address = Utils.generateRandomAddress();

		// Act:
		final AccountMetaDataPair metaDataPair = createRoundTrippedPair(address, context.createAccountMetaData());

		// Assert:
		Assert.assertThat(metaDataPair.getAccount().getAddress(), IsEqual.equalTo(address));
		context.assertContext(metaDataPair.getMetaData());
	}

	private static AccountMetaDataPair createRoundTrippedPair(
			final Address address,
			final AccountMetaData metaData) {
		// Arrange:
		final AccountMetaDataPair metaDataPair = new AccountMetaDataPair(
				new AccountInfo(address, Amount.ZERO, BlockAmount.ZERO, null, 0.0),
				metaData);

		// Act:
		return new AccountMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, null));
	}
}
