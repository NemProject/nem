package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.viewmodels.ExtendedNodeExperiencePair;
import org.nem.nis.service.ChainServices;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.score.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class NodeControllerTest {

	//region getInfo / getExtendedInfo

	@Test
	public void getInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runInfoTest(context, c -> c.controller.getInfo(), n -> n);
	}

	@Test
	public void getInfoAuthenticatedReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runInfoTest(
				context,
				c -> c.controller.getInfo(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runInfoTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, Node> getNode) {
		// Act:
		final T response = action.apply(context);
		final Node node = getNode.apply(response);

		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(context.network.getLocalNode()));
		return response;
	}

	@Test
	public void getExtendedInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Node node = context.controller.getExtendedInfo().getNode();

		// Assert:
		Assert.assertThat(node, IsSame.sameInstance(context.network.getLocalNode()));
	}

	@Test
	public void getExtendedInfoReturnsCommonStarterApplicationMetaData() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ApplicationMetaData appMetaData = context.controller.getExtendedInfo().getAppMetaData();

		// Assert:
		Assert.assertThat(appMetaData, IsSame.sameInstance(CommonStarter.META_DATA));
	}

	//endregion

	//region getPeerList / getActivePeerList

	@Test
	public void getPeerListReturnsNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NodeCollection nodes = context.controller.getPeerList();

		// Assert:
		Assert.assertThat(nodes, IsSame.sameInstance(context.network.getNodes()));
	}

	@Test
	public void getActivePeerListReturnsActiveNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runActivePeerListTest(context, c -> c.controller.getActivePeerList(), l -> l);
	}

	@Test
	public void getActivePeerListAuthenticatedReturnsActiveNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runActivePeerListTest(
				context,
				c -> c.controller.getActivePeerList(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runActivePeerListTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, SerializableList<Node>> getActivePeerList) {
		// Arrange:
		final NodeCollection nodeCollection = context.network.getNodes();
		nodeCollection.update(PeerUtils.createNodeWithHost("10.0.0.2"), NodeStatus.INACTIVE);
		nodeCollection.update(PeerUtils.createNodeWithHost("10.0.0.4"), NodeStatus.ACTIVE);
		nodeCollection.update(PeerUtils.createNodeWithHost("10.0.0.3"), NodeStatus.FAILURE);
		nodeCollection.update(PeerUtils.createNodeWithHost("10.0.0.7"), NodeStatus.ACTIVE);

		// Act:
		final T response = action.apply(context);
		final SerializableList<Node> nodes = getActivePeerList.apply(response);

		// Assert:
		final List<Node> expectedNodes = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.4"),
				PeerUtils.createNodeWithHost("10.0.0.7"));
		Assert.assertThat(nodes.asCollection(), IsEquivalent.equivalentTo(expectedNodes));
		return response;
	}

	//endregion

	@Test
	public void getExperiencesReturnsExtendedLocalNodeExperiences() {
		// Arrange:
		final TestContext context = new TestContext();
		final PeerNetwork network = context.network;
		Mockito.when(network.getLocalNodeAndExperiences()).thenReturn(
				new NodeExperiencesPair(
						PeerUtils.createNodeWithName("l"),
						Arrays.asList(
								new NodeExperiencePair(PeerUtils.createNodeWithName("n"), new NodeExperience(0, 1)),
								new NodeExperiencePair(PeerUtils.createNodeWithName("e"), new NodeExperience(1, 0)),
								new NodeExperiencePair(PeerUtils.createNodeWithName("m"), new NodeExperience(1, 0)))));

		Mockito.when(context.host.getSyncAttempts(PeerUtils.createNodeWithName("n"))).thenReturn(7);
		Mockito.when(context.host.getSyncAttempts(PeerUtils.createNodeWithName("e"))).thenReturn(0);
		Mockito.when(context.host.getSyncAttempts(PeerUtils.createNodeWithName("m"))).thenReturn(2);

		// Act:
		final Collection<ExtendedNodeExperiencePair> pairs = context.controller.getExperiences().asCollection();

		// Assert:
		final List<ExtendedNodeExperiencePair> expectedPairs = Arrays.asList(
				new ExtendedNodeExperiencePair(PeerUtils.createNodeWithName("n"), new NodeExperience(0, 1), 7),
				new ExtendedNodeExperiencePair(PeerUtils.createNodeWithName("e"), new NodeExperience(1, 0), 0),
				new ExtendedNodeExperiencePair(PeerUtils.createNodeWithName("m"), new NodeExperience(1, 0), 2));
		Assert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void pingActivatesSourceNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeCollection nodes = context.network.getNodes();

		final Node sourceNode = PeerUtils.createNodeWithName("alice");
		final NodeExperiencesPair pair = new NodeExperiencesPair(sourceNode, new ArrayList<>());

		// Arrange: sanity
		Assert.assertThat(nodes.getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.FAILURE));

		// Act:
		context.controller.ping(pair);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.ACTIVE));
	}

	@Test
	public void pingSetsSourceNodeExperiences() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(
				PeerUtils.createNodeWithName("alice"),
				new ArrayList<>());

		// Act:
		context.controller.ping(pair);

		// Assert:
		Mockito.verify(context.network, Mockito.times(1)).setRemoteNodeExperiences(pair);
	}

	@Test
	public void canYouSeeMeReturnsTheRemoteAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeEndpoint localEndpoint = new NodeEndpoint("ftp", "localhost", 123);

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getScheme()).thenReturn("https");
		Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.123");
		Mockito.when(request.getRemotePort()).thenReturn(97);

		// Act:
		final NodeEndpoint endpoint = context.controller.canYouSeeMe(localEndpoint, request);

		// Assert:
		// (1) scheme and address come from the servlet request
		// (2) port comes from the original local node endpoint
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("https", "10.0.0.123", 123)));
	}

	@Test
	public void bootDelegatesToPeerNetworkHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final ArgumentCaptor<Node> nodeArgument = ArgumentCaptor.forClass(Node.class);
		Mockito.when(context.host.boot(nodeArgument.capture())).thenReturn(CompletableFuture.completedFuture(null));

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = createLocalNodeDeserializer(identity);

		// Act:
		context.controller.boot(deserializer);

		// Assert:
		Mockito.verify(context.host, Mockito.times(1)).boot(Mockito.any(Node.class));
		Assert.assertThat(nodeArgument.getValue().getIdentity(), IsEqual.equalTo(identity));
	}

	@Test
	public void activePeersMaxChainHeightDelegatesToChainServices() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		// TODO 20140927 J-B: probably should validate the return value too
		// TODO 20140928 BR -> J: done.
		BlockHeight height = context.controller.activePeersMaxChainHeight();

		// Assert:
		Mockito.verify(context.services, Mockito.times(1)).getMaxChainHeightAsync(context.localNode);
		Assert.assertThat(height, IsEqual.equalTo(new BlockHeight(123)));
	}

	private static JsonDeserializer createLocalNodeDeserializer(final NodeIdentity identity) {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final NodeMetaData metaData = new NodeMetaData("p", "a", NodeVersion.ZERO);

		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeObject("identity", childSerializer -> {
			childSerializer.writeBigInteger("private-key", identity.getKeyPair().getPrivateKey().getRaw());
			childSerializer.writeString("name", "trudy");
		});
		serializer.writeObject("endpoint", endpoint);
		serializer.writeObject("metaData", metaData);

		return new JsonDeserializer(serializer.getObject(), null);
	}

	private static class TestContext {
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final NodeController controller;
		private final ChainServices services;
		private final Node localNode;

		private TestContext() {
			this.localNode = PeerUtils.createNodeWithName("l");
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(this.localNode);
			Mockito.when(this.network.getNodes()).thenReturn(new NodeCollection());

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.services = Mockito.mock(ChainServices.class);
			Mockito.when(this.services.getMaxChainHeightAsync(localNode)).thenReturn(CompletableFuture.completedFuture(new BlockHeight(123)));

			this.controller = new NodeController(this.host, this.services);
		}
	}
}
