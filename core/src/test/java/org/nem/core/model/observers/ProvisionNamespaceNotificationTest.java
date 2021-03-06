package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class ProvisionNamespaceNotificationTest {

	@Test
	public void canCreateProvisionNamespaceNotification() {
		// Arrange:
		final Account owner = Utils.generateRandomAccount();
		final NamespaceId namespaceId = new NamespaceId("foo.bar");

		// Act:
		final ProvisionNamespaceNotification notification = new ProvisionNamespaceNotification(owner, namespaceId);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.ProvisionNamespace));
		MatcherAssert.assertThat(notification.getOwner(), IsEqual.equalTo(owner));
		MatcherAssert.assertThat(notification.getNamespaceId(), IsEqual.equalTo(new NamespaceId("foo.bar")));
	}
}
