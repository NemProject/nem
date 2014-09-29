package org.nem.core.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.net.URL;
import java.util.*;

public class NodeEndpointTest {

	//region constructor

	@Test
	public void ctorCanCreateNewNodeEndpoint() throws Exception {
		// Act:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);

		// Assert:
		final URL expectedUrl = new URL("ftp", "10.8.8.2", 12, "/");
		Assert.assertThat(endpoint.getBaseUrl(), IsEqual.equalTo(expectedUrl));
		assertApiUrlsAreCorrect(expectedUrl, endpoint);
	}

	@Test
	public void nodeEndpointCanBeCreatedFromHost() throws Exception {
		// Act:
		final NodeEndpoint endpoint = NodeEndpoint.fromHost("10.8.8.2");

		// Assert:
		final URL expectedUrl = new URL("http", "10.8.8.2", 7890, "/");
		Assert.assertThat(endpoint.getBaseUrl(), IsEqual.equalTo(expectedUrl));
		assertApiUrlsAreCorrect(expectedUrl, endpoint);
	}

	@Test
	public void nodeEndpointCanBeRoundTripped() throws Exception {
		// Arrange:
		final NodeEndpoint originalEndpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);

		// Act:
		final NodeEndpoint endpoint = new NodeEndpoint(Utils.roundtripSerializableEntity(originalEndpoint, null));

		// Assert:
		final URL expectedUrl = new URL("ftp", "10.8.8.2", 12, "/");
		Assert.assertThat(endpoint.getBaseUrl(), IsEqual.equalTo(expectedUrl));
		assertApiUrlsAreCorrect(expectedUrl, endpoint);
	}

	private static void assertApiUrlsAreCorrect(final URL url, final NodeEndpoint endpoint) throws Exception {
		final Map<NodeApiId, String> apiIdToPathMap = new HashMap<>();
		apiIdToPathMap.put(NodeApiId.REST_BLOCK_AT, "block/at");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_BLOCKS_AFTER, "chain/blocks-after");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_HASHES_FROM, "chain/hashes-from");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_LAST_BLOCK, "chain/last-block");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_SCORE, "chain/score");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_HEIGHT, "chain/height");
		apiIdToPathMap.put(NodeApiId.REST_NODE_CAN_YOU_SEE_ME, "node/cysm");
		apiIdToPathMap.put(NodeApiId.REST_NODE_EXTENDED_INFO, "node/extended-info");
		apiIdToPathMap.put(NodeApiId.REST_NODE_INFO, "node/info");
		apiIdToPathMap.put(NodeApiId.REST_NODE_PEER_LIST, "node/peer-list/all");
		apiIdToPathMap.put(NodeApiId.REST_NODE_PEER_LIST_ACTIVE, "node/peer-list/active");
		apiIdToPathMap.put(NodeApiId.REST_NODE_PING, "node/ping");
		apiIdToPathMap.put(NodeApiId.REST_PUSH_BLOCK, "push/block");
		apiIdToPathMap.put(NodeApiId.REST_PUSH_TRANSACTION, "push/transaction");
		apiIdToPathMap.put(NodeApiId.REST_TRANSACTIONS_UNCONFIRMED, "transactions/unconfirmed");
		apiIdToPathMap.put(NodeApiId.REST_TIME_SYNC_NETWORK_TIME, "time-sync/network-time");

		for (final NodeApiId apiId : NodeApiId.values()) {
			if (!apiIdToPathMap.containsKey(apiId)) {
				Assert.fail(String.format("path for '%s' is not being tested", apiId));
			}

			Assert.assertThat(endpoint.getApiUrl(apiId), IsEqual.equalTo(new URL(url, apiIdToPathMap.get(apiId))));
		}
	}

	//region invalid parameters

	@Test(expected = IllegalArgumentException.class)
	public void ctorFailsIfProtocolIsInvalid() throws Exception {
		// Act:
		new NodeEndpoint("xyz", "10.8.8.2", 12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorFailsIfProtocolIsHostIsInvalid() throws Exception {
		// Act:
		new NodeEndpoint("ftp", "10.8.8.2.1", 12);
	}

	//endregion

	//region host name resolution

	@Test
	public void blankHostResolvesToLocalHost() throws Exception {
		// Assert:
		assertHostNameResolvesTo(null, "localhost");
		assertHostNameResolvesTo("", "localhost");
		assertHostNameResolvesTo("  \t ", "localhost");
	}

	private static void assertHostNameResolvesTo(final String hostName, final String expectedHostName) {
		// Act:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", hostName, 12);

		// Assert:
		Assert.assertThat(endpoint.getBaseUrl().getHost(), IsEqual.equalTo(expectedHostName));
	}

	//endregion

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);

		// Assert:
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 12), IsEqual.equalTo(endpoint));
		Assert.assertThat(new NodeEndpoint("http", "10.8.8.2", 12), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.1", 12), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 13), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)endpoint)));
	}

	@Test
	public void endpointsWithEquivalentHostNamesAreEqual() {
		// Arrange:
		final NodeEndpoint endpointWithHostIp = new NodeEndpoint("http", "127.0.0.1", 12);
		final NodeEndpoint endpointWithHostName = new NodeEndpoint("http", "localhost", 12);

		// Assert:
		Assert.assertThat(endpointWithHostIp, IsEqual.equalTo(endpointWithHostName));
		Assert.assertThat(endpointWithHostIp.hashCode(), IsEqual.equalTo(endpointWithHostName.hashCode()));
	}

	@Test
	public void endpointsWithEquivalentHostNamesAreEqualAfterDeserialization() {
		// Arrange:
		final NodeEndpoint endpointWithHostIp =
				new NodeEndpoint(Utils.roundtripSerializableEntity(new NodeEndpoint("http", "127.0.0.1", 12), null));
		final NodeEndpoint endpointWithHostName =
				new NodeEndpoint(Utils.roundtripSerializableEntity(new NodeEndpoint("http", "localhost", 12), null));

		// Assert:
		Assert.assertThat(endpointWithHostIp, IsEqual.equalTo(endpointWithHostName));
		Assert.assertThat(endpointWithHostIp.hashCode(), IsEqual.equalTo(endpointWithHostName.hashCode()));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);
		final int hashCode = endpoint.hashCode();

		// Assert:
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 12).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeEndpoint("http", "10.8.8.2", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.1", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 13).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	@Test
	public void endpointCreatedAroundHostNameIsEquivalentToEndpointCreatedAroundResolvedAddress() {
		// Arrange:
		final NodeEndpoint endpoint1 = new NodeEndpoint("ftp", "localhost", 12);
		final NodeEndpoint endpoint2 = new NodeEndpoint("ftp", "127.0.0.1", 12);

		// Assert:
		Assert.assertThat(endpoint1, IsEqual.equalTo(endpoint2));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsCorrectRepresentationWhenEndpointHasHostIp() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "127.0.0.1", 12);

		// Assert:
		Assert.assertThat(endpoint.toString(), IsEqual.equalTo("ftp://127.0.0.1:12/"));
	}

	@Test
	public void toStringReturnsCorrectRepresentationWhenEndpointHasHostName() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "localhost", 12);

		// Assert:
		Assert.assertThat(endpoint.toString(), IsEqual.equalTo("ftp://localhost:12/"));
	}

	//endregion
}
