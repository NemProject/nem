package org.nem.nis.connect;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.node.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.ExceptionAssert;
import org.nem.deploy.CommonStarter;
import org.nem.nis.FatalConfigException;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.cache.DefaultAccountCache;
import org.nem.peer.Config;
import org.nem.peer.connect.*;
import org.nem.peer.node.ImpersonatingPeerException;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class HttpConnectorITCase {
	private static final Logger LOGGER = Logger.getLogger(HttpConnectorITCase.class.getName());

	private final Communicator communicator = new HttpCommunicator(
			new HttpMethodClient<>(),
			CommunicationMode.JSON,
			new DeserializationContext(new DefaultAccountCache()));
	private final HttpConnector connector = new HttpConnector(this.communicator);

	@Test
	public void canCommunicateWithNemNinjaWithExpectedBootAddress() {
		// Arrange:
		final PublicKey publicKey = PublicKey.fromHexString("3302e7703ee9f364c25bbfebb9c12ac91fa9dcd69e09a5d4f3830d71505a2350");
		final Node node = createNemNinjaNode(publicKey);

		// Act:
		final Node remoteNode = this.connector.getInfo(node).join();

		// Assert:
		MatcherAssert.assertThat(remoteNode.getIdentity().getAddress().getPublicKey(), IsEqual.equalTo(publicKey));
	}

	@Test
	public void cannotCommunicateWithNemNinjaWithUnexpectedBootAddress() {
		// Arrange:
		final PublicKey publicKey = PublicKey.fromHexString("494e58ec8855c7a6087411506cbadbce35ce0ee76ba0baf2305c2196606fac41");
		final Node node = createNemNinjaNode(publicKey);

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> this.connector.getInfo(node).join(),
				ImpersonatingPeerException.class);
	}

	private static Node createNemNinjaNode(final PublicKey publicKey) {
		final Address address = Address.fromPublicKey(publicKey);
		return new Node(
				new NodeIdentity(new KeyPair(address.getPublicKey())),
				NodeEndpoint.fromHost("alice2.nem.ninja"));
	}

	@Test
	public void analyzePreTrustedPeers() {
		// Arrange:
		final Config config = new Config(
				new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("localhost")),
				loadJsonObject("peers-config_testnet.json"),
				CommonStarter.META_DATA.getVersion(),
				0,
				new NodeFeature[] {});

		// Act:
		final boolean result = this.analyzeNodes(config.getPreTrustedNodes().getNodes());

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
	}

	private enum NodeStatus {
		ONLINE,
		IMPERSONATING,
		TIMED_OUT,
		FAILED
	}

	private boolean analyzeNodes(final Collection<Node> nodes) {
		final Map<NodeStatus, Integer> nodeStatusCounts = new HashMap<>();

		for (final Node node : nodes) {
			final NodeStatus status = this.analyzeNode(node);
			nodeStatusCounts.put(status, nodeStatusCounts.getOrDefault(status, 0) + 1);
		}

		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%s nodes", nodes.size()));
		for (final Map.Entry<NodeStatus, Integer> pair : nodeStatusCounts.entrySet()) {
			builder.append(System.lineSeparator());
			builder.append(String.format("%s nodes -> %s", pair.getValue(), pair.getKey()));
		}

		LOGGER.info(builder.toString());
		return 0 == nodeStatusCounts.getOrDefault(NodeStatus.IMPERSONATING, 0);
	}

	private NodeStatus analyzeNode(final Node node) {
		try {
			this.connector.getInfo(node).join();
			LOGGER.info(String.format("%s is configured correctly!", node));
			return NodeStatus.ONLINE;
		} catch (final CompletionException e) {
			final Throwable innerException = e.getCause();
			LOGGER.warning(String.format("%s is misbehaving: %s", node, innerException));
			if (ImpersonatingPeerException.class.isAssignableFrom(innerException.getClass())) {
				return NodeStatus.IMPERSONATING;
			} else if (InactivePeerException.class.isAssignableFrom(innerException.getClass())) {
				return NodeStatus.TIMED_OUT;
			} else {
				return NodeStatus.FAILED;
			}
		}
	}

	private static JSONObject loadJsonObject(final String configFileName) {
		try {
			try (final InputStream fin = NisPeerNetworkHost.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin) {
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));
				}

				return (JSONObject)JSONValue.parse(fin);
			}
		} catch (final Exception e) {
			throw new FatalConfigException("Exception encountered while loading config", e);
		}
	}
}
