package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.test.NisUtils;

public class ProvisionNamespaceObserverTest {
	private static final int NOTIFY_BLOCK_HEIGHT = 111;

	// region provision namespace

	@Test
	public void notifyExecuteCallsNamespaceCacheAddWithExpectedNamespace() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyProvisionNamespace(context, NotificationTrigger.Execute);

		// Assert:
		final ArgumentCaptor<Namespace> namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
		Mockito.verify(context.namespaceCache, Mockito.only()).add(namespaceCaptor.capture());
		final Namespace namespace = namespaceCaptor.getValue();
		Assert.assertThat(namespace.getId(), IsEqual.equalTo(context.namespaceId));
		Assert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.owner));
		Assert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(NOTIFY_BLOCK_HEIGHT + 1440 * 365)));
	}

	@Test
	public void notifyUndoCallsNamespaceCacheRemoveWithExpectedNamespaceId() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		this.notifyProvisionNamespace(context, NotificationTrigger.Undo);

		// Assert:
		Mockito.verify(context.namespaceCache, Mockito.only()).remove(context.namespaceId);
	}

	// endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new BalanceTransferNotification(
						Utils.generateRandomAccount(),
						Utils.generateRandomAccount(),
						Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.namespaceCache, Mockito.never()).add(Mockito.any());
		Mockito.verify(context.namespaceCache, Mockito.never()).remove(Mockito.any());
	}

	//endregion

	private void notifyProvisionNamespace(
			final TestContext context,
			final NotificationTrigger notificationTrigger) {
		// Arrange:
		final ProvisionNamespaceObserver observer = context.createObserver();

		// Act:
		observer.notify(
				new ProvisionNamespaceNotification(context.owner, context.namespaceId),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private class TestContext {
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final Account owner = Utils.generateRandomAccount();
		private final NamespaceId namespaceId = Mockito.mock(NamespaceId.class);

		private ProvisionNamespaceObserver createObserver() {
			return new ProvisionNamespaceObserver(this.namespaceCache);
		}
	}
}
