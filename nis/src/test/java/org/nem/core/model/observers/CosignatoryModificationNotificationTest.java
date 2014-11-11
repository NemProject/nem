package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;

public class CosignatoryModificationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account multisig = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final CosignatoryModificationNotification notification = new CosignatoryModificationNotification(multisig, cosigner, 7);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.CosignatoryModification));
		Assert.assertThat(notification.getMultisigAccount(), IsEqual.equalTo(multisig));
		Assert.assertThat(notification.getCosignatoryAccount(), IsEqual.equalTo(cosigner));
		Assert.assertThat(notification.getModificationType(), IsEqual.equalTo(7));
	}
}