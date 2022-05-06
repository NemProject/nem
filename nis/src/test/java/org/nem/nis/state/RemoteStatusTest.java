package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.AccountRemoteStatus;

public class RemoteStatusTest {

	// region toAccountRemoteStatus

	@Test
	public void remoteStatusCanBeConvertedIntoAccountRemoteStatus() {
		// Assert:
		MatcherAssert.assertThat(RemoteStatus.values().length, IsEqual.equalTo(9));
		assertMapping(RemoteStatus.NOT_SET, AccountRemoteStatus.INACTIVE);
		assertMapping(RemoteStatus.OWNER_INACTIVE, AccountRemoteStatus.INACTIVE);
		assertMapping(RemoteStatus.OWNER_ACTIVATING, AccountRemoteStatus.ACTIVATING);
		assertMapping(RemoteStatus.OWNER_ACTIVE, AccountRemoteStatus.ACTIVE);
		assertMapping(RemoteStatus.OWNER_DEACTIVATING, AccountRemoteStatus.DEACTIVATING);
		assertMapping(RemoteStatus.REMOTE_INACTIVE, AccountRemoteStatus.REMOTE);
		assertMapping(RemoteStatus.REMOTE_ACTIVATING, AccountRemoteStatus.REMOTE);
		assertMapping(RemoteStatus.REMOTE_ACTIVE, AccountRemoteStatus.REMOTE);
		assertMapping(RemoteStatus.REMOTE_DEACTIVATING, AccountRemoteStatus.REMOTE);
	}

	private static void assertMapping(final RemoteStatus remoteStatus, final AccountRemoteStatus accountRemoteStatus) {
		// Assert:
		MatcherAssert.assertThat(remoteStatus.toAccountRemoteStatus(), IsEqual.equalTo(accountRemoteStatus));
	}

	// endregion
}
