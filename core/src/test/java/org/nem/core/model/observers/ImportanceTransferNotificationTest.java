package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
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
		final ImportanceTransferNotification notification = new ImportanceTransferNotification(lessor, lessee,
				ImportanceTransferMode.Activate);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.ImportanceTransfer));
		MatcherAssert.assertThat(notification.getLessor(), IsEqual.equalTo(lessor));
		MatcherAssert.assertThat(notification.getLessee(), IsEqual.equalTo(lessee));
		MatcherAssert.assertThat(notification.getMode(), IsEqual.equalTo(ImportanceTransferMode.Activate));
	}
}
