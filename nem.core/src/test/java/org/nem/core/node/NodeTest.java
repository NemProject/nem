package org.nem.core.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class NodeTest {

	private static final NodeIdentity DEFAULT_IDENTITY = new WeakNodeIdentity("bob");
	private static final NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);
	private static final NodeMetaData DEFAULT_META_DATA = new NodeMetaData(null, null);

	//region construction

	@Test
	public void canCreateNewNodeWithoutMetaData() {
		// Arrange:
		final NodeIdentity identity = new WeakNodeIdentity("alice");
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);

		// Act:
		final Node node = new Node(identity, endpoint);

		// Assert:
		Assert.assertThat(node.getIdentity(), IsSame.sameInstance(identity));
		Assert.assertThat(node.getEndpoint(), IsSame.sameInstance(endpoint));
		Assert.assertThat(node.getMetaData(), IsEqual.equalTo(new NodeMetaData(null, null)));
	}

	@Test
	public void canCreateNewNodeWithMetaData() {
		// Arrange:
		final NodeIdentity identity = new WeakNodeIdentity("alice");
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final NodeMetaData metaData = new NodeMetaData("p", "a");

		// Act:
		final Node node = new Node(identity, endpoint, metaData);

		// Assert:
		Assert.assertThat(node.getIdentity(), IsSame.sameInstance(identity));
		Assert.assertThat(node.getEndpoint(), IsSame.sameInstance(endpoint));
		Assert.assertThat(node.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void nodeCanBeRoundTripped() {
		// Arrange:
		final NodeIdentity identity = new WeakNodeIdentity("alice");
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final NodeMetaData metaData = new NodeMetaData("p", "a");
		final Node originalNode = new Node(identity, endpoint, metaData);

		// Act:
		final Node node = new Node(Utils.roundtripSerializableEntity(originalNode, null));

		// Assert:
		Assert.assertThat(node.getIdentity(), IsEqual.equalTo(identity));
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(endpoint));
		Assert.assertThat(node.getMetaData(), IsEqual.equalTo(metaData));
	}

	@Test
	public void nodeCanBeDeserializedWithoutMetaData() {
		// Arrange:
		final NodeIdentity identity = new WeakNodeIdentity("alice");
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final Node originalNode = new Node(identity, endpoint);

		final JsonSerializer serializer = new JsonSerializer(true);
		originalNode.serialize(serializer);
		serializer.getObject().remove("metaData");

		// Act:
		final Node node = new Node(new JsonDeserializer(serializer.getObject(), null));

		// Assert:
		Assert.assertThat(node.getIdentity(), IsEqual.equalTo(identity));
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(endpoint));
		Assert.assertThat(node.getMetaData(), IsEqual.equalTo(new NodeMetaData(null, null)));
	}

	@Test
	public void identityCannotBeNull() {
		// Act:
		ExceptionAssert.assertThrows(v -> new Node(null, DEFAULT_ENDPOINT), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new Node(null, DEFAULT_ENDPOINT, DEFAULT_META_DATA),
				IllegalArgumentException.class);
	}

	@Test
	public void endpointCannotBeNull() {
		// Act:
		ExceptionAssert.assertThrows(v -> new Node(DEFAULT_IDENTITY, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new Node(DEFAULT_IDENTITY, null, DEFAULT_META_DATA),
				IllegalArgumentException.class);
	}

	//endregion

	//region setters

	@Test
	public void canChangeEndpoint() {
		// Arrange:
		final Node node = new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT);
		final NodeEndpoint endpoint = NodeEndpoint.fromHost("10.0.0.2");

		// Act:
		node.setEndpoint(endpoint);

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsSame.sameInstance(endpoint));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotChangeEndpointToNull() {
		// Act:
		new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT).setEndpoint(null);
	}

	@Test
	public void canChangeMetaData() {
		// Arrange:
		final Node node = new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT);
		final NodeMetaData metaData = new NodeMetaData("aaa", "ppp");

		// Act:
		node.setMetaData(metaData);

		// Assert:
		Assert.assertThat(node.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotChangeMetaDataToNull() {
		// Act:
		new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT).setMetaData(null);
	}

	//endregion

	//region equals / hashCode

	private static final Map<String, Node> DESC_TO_NODE_MAP = new HashMap<String, Node>() {
		{
			this.put("default", new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT, DEFAULT_META_DATA));
			this.put("diff-identity", new Node(new WeakNodeIdentity("alice"), DEFAULT_ENDPOINT, DEFAULT_META_DATA));
			this.put("diff-endpoint", new Node(DEFAULT_IDENTITY, new NodeEndpoint("http", "localhost", 8080), DEFAULT_META_DATA));
			this.put("diff-meta-data", new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT, new NodeMetaData("p", "a")));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT, DEFAULT_META_DATA);

		// Assert:
		Assert.assertThat(DESC_TO_NODE_MAP.get("default"), IsEqual.equalTo(node));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-identity"), IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-endpoint"), IsEqual.equalTo(node));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-meta-data"), IsEqual.equalTo(node));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(DEFAULT_IDENTITY, IsNot.not(IsEqual.equalTo((Object)node)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_IDENTITY, DEFAULT_ENDPOINT, DEFAULT_META_DATA);
		final int hashCode = node.hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_NODE_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-identity").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-endpoint").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-meta-data").hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final NodeIdentity identity = new WeakNodeIdentity("alice");
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);
		final NodeMetaData metaData = new NodeMetaData("p", "a");
		final Node node = new Node(identity, endpoint, metaData);

		// Assert:
		Assert.assertThat(node.toString(), IsEqual.equalTo("Node [(Weak Id) alice] @ [localhost]"));
	}

	//endregion
}
