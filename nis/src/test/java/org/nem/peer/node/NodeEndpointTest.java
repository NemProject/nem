package org.nem.peer.node;

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
		final Map<NodeApiId, String > apiIdToPathMap = new HashMap<>();
		apiIdToPathMap.put(NodeApiId.REST_BLOCK_AT, "block/at");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_BLOCKS_AFTER, "chain/blocks-after");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_HASHES_FROM, "chain/hashes-from");
		apiIdToPathMap.put(NodeApiId.REST_CHAIN_LAST_BLOCK, "chain/last-block");
		apiIdToPathMap.put(NodeApiId.REST_NODE_CAN_YOU_SEE_ME, "node/cysm");
		apiIdToPathMap.put(NodeApiId.REST_NODE_EXTENDED_INFO, "node/extended-info");
		apiIdToPathMap.put(NodeApiId.REST_NODE_INFO, "node/info");
		apiIdToPathMap.put(NodeApiId.REST_NODE_PEER_LIST, "node/peer-list");
		apiIdToPathMap.put(NodeApiId.REST_NODE_PING, "node/ping");
		apiIdToPathMap.put(NodeApiId.REST_PUSH_BLOCK, "push/block");
		apiIdToPathMap.put(NodeApiId.REST_PUSH_TRANSACTION, "push/transaction");

		for (final NodeApiId apiId : NodeApiId.values()) {
			if (!apiIdToPathMap.containsKey(apiId))
				Assert.fail(String.format("path for '%s' is not being tested", apiId));

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
		assertHostNameResolvesTo(null, "127.0.0.1");
		assertHostNameResolvesTo("", "127.0.0.1");
	}

	@Test
	public void knownHostNameIsResolvedToAddress() {
		// Assert:
		assertHostNameResolvesTo("localhost", "127.0.0.1");
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
		NodeEndpoint endpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);

		// Assert:
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 12), IsEqual.equalTo(endpoint));
		Assert.assertThat(new NodeEndpoint("http", "10.8.8.2", 12), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.1", 12), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 13), IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(endpoint)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)endpoint)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		NodeEndpoint endpoint = new NodeEndpoint("ftp", "10.8.8.2", 12);
		int hashCode = endpoint.hashCode();

		// Assert:
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 12).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeEndpoint("http", "10.8.8.2", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.1", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeEndpoint("ftp", "10.8.8.2", 13).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	@Test
	public void endpointCreatedAroundHostNameIsEquivalentToEndpointCreatedAroundResolvedAddress() {
		// Arrange:
		NodeEndpoint endpoint1 = new NodeEndpoint("ftp", "localhost", 12);
		NodeEndpoint endpoint2 = new NodeEndpoint("ftp", "127.0.0.1", 12);

		// Assert:
		Assert.assertThat(endpoint1, IsEqual.equalTo(endpoint2));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsBaseUrlStringRepresentation() {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("ftp", "127.0.0.1", 12);

		// Assert:
		Assert.assertThat(endpoint.toString(), IsEqual.equalTo(endpoint.getBaseUrl().toString()));
	}

	//endregion
}
