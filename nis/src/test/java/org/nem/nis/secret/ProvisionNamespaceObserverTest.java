package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Stream;

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
		Mockito.verify(context.namespaceCache, Mockito.times(1)).add(namespaceCaptor.capture());
		final Namespace namespace = namespaceCaptor.getValue();
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(context.namespaceId));
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.owner));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(NOTIFY_BLOCK_HEIGHT)));
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

	@Test
	public void notifyExecuteAddsExistingMosaicsToOwners() {
		// Arrange:
		// - owners
		final Account namespaceOwner = Utils.generateRandomAccount();
		List<Address> tokensOwners = Arrays.asList(Utils.generateRandomAddress(), namespaceOwner.getAddress());
		List<Address> coinsOwners = Arrays.asList(Utils.generateRandomAddress(), namespaceOwner.getAddress());

		// - namespace cache setup
		final DefaultNamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		addNamespace(namespaceCache, namespaceOwner, "foo");
		addNamespace(namespaceCache, namespaceOwner, "foo.bar");
		addMosaic(namespaceCache, "foo", "tokens");
		addMosaic(namespaceCache, "foo.bar", "coins");
		addOwners(namespaceCache, createMosaicId("foo", "tokens"), tokensOwners);
		addOwners(namespaceCache, createMosaicId("foo.bar", "coins"), coinsOwners);
		namespaceCache.commit();

		// - account state cache setup
		final DefaultAccountStateCache accountStateCache = new DefaultAccountStateCache().copy();
		Stream.concat(tokensOwners.stream(), coinsOwners.stream()).forEach(accountStateCache::findStateByAddress);
		accountStateCache.commit();

		final ProvisionNamespaceObserver observer = new ProvisionNamespaceObserver(namespaceCache, accountStateCache);

		// Act:
		observer.notify(new ProvisionNamespaceNotification(namespaceOwner, new NamespaceId("foo")),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), NotificationTrigger.Execute));

		// Assert:
		MatcherAssert.assertThat(accountStateCache.findStateByAddress(namespaceOwner.getAddress()).getAccountInfo().getMosaicIds(),
				IsEquivalent.equivalentTo(Arrays.asList(createMosaicId("foo", "tokens"), createMosaicId("foo.bar", "coins"))));
		MatcherAssert.assertThat(accountStateCache.findStateByAddress(tokensOwners.get(0)).getAccountInfo().getMosaicIds(),
				IsEquivalent.equivalentTo(Collections.singletonList(createMosaicId("foo", "tokens"))));
		MatcherAssert.assertThat(accountStateCache.findStateByAddress(coinsOwners.get(0)).getAccountInfo().getMosaicIds(),
				IsEquivalent.equivalentTo(Collections.singletonList(createMosaicId("foo.bar", "coins"))));
	}

	private static void addMosaic(final NamespaceCache cache, final String namespaceName, final String mosaicName) {
		cache.get(new NamespaceId(namespaceName)).getMosaics().add(Utils.createMosaicDefinition(namespaceName, mosaicName));
	}

	private static void addNamespace(final NamespaceCache cache, final Account owner, final String namespaceName) {
		cache.add(new Namespace(new NamespaceId(namespaceName), owner, BlockHeight.ONE));
	}

	private static MosaicId createMosaicId(final String namespaceName, final String mosaicName) {
		return new MosaicId(new NamespaceId(namespaceName), mosaicName);
	}
	private static void addOwners(final NamespaceCache cache, final MosaicId mosaicId, final Collection<Address> owners) {
		owners.forEach(owner -> cache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getBalances().incrementBalance(owner,
				Quantity.fromValue(1)));
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final ProvisionNamespaceObserver observer = context.createObserver();

		// Act:
		observer.notify(new BalanceTransferNotification(Utils.generateRandomAccount(), Utils.generateRandomAccount(), Amount.fromNem(123)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.namespaceCache, Mockito.never()).add(Mockito.any());
		Mockito.verify(context.namespaceCache, Mockito.never()).remove(Mockito.any());
	}

	// endregion

	private void notifyProvisionNamespace(final TestContext context, final NotificationTrigger notificationTrigger) {
		// Arrange:
		final ProvisionNamespaceObserver observer = context.createObserver();

		// Act:
		observer.notify(new ProvisionNamespaceNotification(context.owner, context.namespaceId),
				NisUtils.createBlockNotificationContext(new BlockHeight(NOTIFY_BLOCK_HEIGHT), notificationTrigger));
	}

	private class TestContext {
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final Account owner = Utils.generateRandomAccount();
		private final NamespaceId namespaceId = new NamespaceId("foo");

		private ProvisionNamespaceObserver createObserver() {
			return new ProvisionNamespaceObserver(this.namespaceCache, this.accountStateCache);
		}
	}
}
