package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class MultisigModificationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account multisig = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final MultisigModification modification = new MultisigModification(MultisigModificationType.Add_Cosignatory, cosigner);
		final MultisigModificationNotification notification = new MultisigModificationNotification(multisig, modification);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.CosignatoryModification));
		Assert.assertThat(notification.getMultisigAccount(), IsEqual.equalTo(multisig));
		Assert.assertThat(notification.getModification(), IsEqual.equalTo(modification));
	}
}