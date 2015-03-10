package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

public class ImportanceTransferNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account lessor = Utils.generateRandomAccount();
		final Account lessee = Utils.generateRandomAccount();
		final ImportanceTransferNotification notification = new ImportanceTransferNotification(lessor, lessee, ImportanceTransferMode.Activate);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.ImportanceTransfer));
		Assert.assertThat(notification.getLessor(), IsEqual.equalTo(lessor));
		Assert.assertThat(notification.getLessee(), IsEqual.equalTo(lessee));
		Assert.assertThat(notification.getMode(), IsEqual.equalTo(ImportanceTransferMode.Activate));
	}
}