package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class MultisigMinCosignatoriesModificationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account multisig = Utils.generateRandomAccount();
		final MultisigMinCosignatoriesModification modification = new MultisigMinCosignatoriesModification(3);
		final MultisigMinCosignatoriesModificationNotification notification = new MultisigMinCosignatoriesModificationNotification(multisig,
				modification);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MinCosignatoriesModification));
		MatcherAssert.assertThat(notification.getMultisigAccount(), IsEqual.equalTo(multisig));
		MatcherAssert.assertThat(notification.getModification(), IsEqual.equalTo(modification));
	}
}
