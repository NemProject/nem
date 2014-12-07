package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

import java.util.Arrays;
import java.util.List;

public class MultisigModificationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account multisig = Utils.generateRandomAccount();
		final Account cosigner = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = Arrays.asList(new MultisigModification(MultisigModificationType.Unknown, cosigner));
		final MultisigModificationNotification notification = new MultisigModificationNotification(multisig, modifications);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.CosignatoryModification));
		Assert.assertThat(notification.getMultisigAccount(), IsEqual.equalTo(multisig));
		Assert.assertThat(notification.getModifications().size(), IsEqual.equalTo(1));
		final MultisigModification modification = notification.getModifications().get(0);
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(cosigner));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(MultisigModificationType.Unknown));
	}
}