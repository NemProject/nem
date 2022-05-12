package org.nem.peer.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.core.time.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.*;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.trust.NodeSelector;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.*;

public class NodeExperiencesUpdaterTest {

	@Test
	public void updateDelegatesToNodeSelectorForNodeSelection() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.updater.update(context.selector);

		// Assert:
		Mockito.verify(context.selector, Mockito.only()).selectNode();
	}

	@Test
	public void updateReturnsFalseWhenThereAreNoCommunicationPartners() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void updateDelegatesToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node remoteNode = context.makeSelectorReturnRemoteNode();
		final NodeExperiencesPair pair = createPair();
		Mockito.when(context.connector.getNodeExperiences(remoteNode)).thenReturn(CompletableFuture.completedFuture(pair));
		Mockito.when(context.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(123));

		// Act:
		context.updater.update(context.selector);

		// Assert:
		Mockito.verify(context.connector, Mockito.only()).getNodeExperiences(remoteNode);
		Mockito.verify(context.timeProvider, Mockito.only()).getCurrentTime();
		Mockito.verify(context.state, Mockito.only()).setRemoteNodeExperiences(pair, new TimeInstant(123));
	}

	@Test
	public void updateIsAsync() {
		// Arrange:
		final TestContext context = new TestContext();
		context.makeSelectorReturnRemoteNode();
		Mockito.when(context.connector.getNodeExperiences(Mockito.any())).thenReturn(CompletableFuture.supplyAsync(() -> {
			ExceptionUtils.propagateVoid(() -> Thread.sleep(300));
			return null;
		}));

		// Act:
		final CompletableFuture<Boolean> future = context.updater.update(context.selector);

		// Assert:
		MatcherAssert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	@Test
	public void updateReturnsFalseWhenRemoteSuppliedTooManyExperiences() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node remoteNode = context.makeSelectorReturnRemoteNode();
		final NodeExperiencesPair pair = createPairWithTooManyExperiences();
		Mockito.when(context.connector.getNodeExperiences(remoteNode)).thenReturn(CompletableFuture.completedFuture(pair));
		Mockito.when(context.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(123));

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
	}

	private NodeExperiencesPair createPairWithTooManyExperiences() {
		final List<NodeExperiencePair> pairs = IntStream.range(0, 110)
				.mapToObj(i -> new NodeExperiencePair(NodeUtils.createNodeWithName("alice"), new NodeExperience(1, 2)))
				.collect(Collectors.toList());
		return createPair(pairs);
	}

	private NodeExperiencesPair createPair() {
		final NodeExperiencePair pair = new NodeExperiencePair(NodeUtils.createNodeWithName("alice"), new NodeExperience(1, 2));
		return createPair(Collections.singletonList(pair));
	}

	private NodeExperiencesPair createPair(final List<NodeExperiencePair> pairs) {
		return new NodeExperiencesPair(NodeUtils.createNodeWithName("bob"), pairs);
	}

	private static class TestContext {
		private final PeerConnector connector = Mockito.mock(PeerConnector.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final NodeSelector selector = Mockito.mock(NodeSelector.class);
		private final NodeExperiencesUpdater updater = new NodeExperiencesUpdater(this.connector, this.timeProvider, this.state);

		public Node makeSelectorReturnRemoteNode() {
			final Node remoteNode = NodeUtils.createNodeWithName("p");
			Mockito.when(this.selector.selectNode()).thenReturn(remoteNode);
			return remoteNode;
		}
	}
}
