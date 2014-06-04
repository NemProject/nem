package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.util.*;

public class NodeTest {

	private final static NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);

	@Test
	public void ctorCanCreateNewNode() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app", "ver");

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(DEFAULT_ENDPOINT));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo("ver"));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
	}

	@Test
	public void nodeCanBeCreatedFromHost() {
		// Act:
		final Node node = Node.fromHost("10.7.6.5");

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(NodeEndpoint.fromHost("10.7.6.5")));
		Assert.assertThat(node.getPlatform(), IsNull.nullValue());
		Assert.assertThat(node.getVersion(), IsNull.nullValue());
		Assert.assertThat(node.getApplication(), IsNull.nullValue());
	}

	@Test
	public void nodeCanBeCreatedFromEndpoint() {
		// Act:
		final Node node = Node.fromEndpoint(new NodeEndpoint("ftp", "10.7.6.5", 12));

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("ftp", "10.7.6.5", 12)));
		Assert.assertThat(node.getPlatform(), IsNull.nullValue());
		Assert.assertThat(node.getVersion(), IsNull.nullValue());
		Assert.assertThat(node.getApplication(), IsNull.nullValue());
	}

	@Test
	public void nodeCanBeRoundTripped() {
		// Arrange:
		final Node originalNode = new Node(DEFAULT_ENDPOINT, "plat", "app", "ver");

		// Act:
		final Node node = new Node(Utils.roundtripSerializableEntity(originalNode, null));

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(DEFAULT_ENDPOINT));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo("ver"));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void endpointCannotBeNull() {
		// Act:
		new Node(null, "plat", "app", "ver");
	}

	@Test
	public void versionIsOptional() {
		// Arrange:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app", null);

		// Assert:
		Assert.assertThat(node.getVersion(), IsNull.nullValue());
	}

	@Test
	public void platformIsOptional() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, null, "app", "ver");

		// Assert:
		Assert.assertThat(node.getPlatform(), IsNull.nullValue());
	}

	@Test
	public void applicationIsOptional() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", null, "ver");

		// Assert:
		Assert.assertThat(node.getApplication(), IsNull.nullValue());
	}

	//region equals / hashCode

	private static final Map<String, Node> DESC_TO_NODE_MAP = new HashMap<String, Node>() {
		{
			put("default", new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app", "ver"));
			put("diff-endpoint", new Node(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app", "ver"));
			put("diff-plat", new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app", "ver"));
			put("diff-app", new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2", "ver"));
			put("diff-ver", new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app", "ver2"));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app", "ver");

		// Assert:
		Assert.assertThat(DESC_TO_NODE_MAP.get("default"), IsEqual.equalTo(node));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-endpoint"), IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-plat"), IsEqual.equalTo(node));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-app"), IsEqual.equalTo(node));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-ver"), IsEqual.equalTo(node));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)node)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app", "ver");
		final int hashCode = node.hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_NODE_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-endpoint").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-plat").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-app").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_NODE_MAP.get("diff-ver").hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	//toString

	@Test
	public void toStringIncludesHost() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app", "ver");

		// Assert:
		Assert.assertThat(node.toString(), IsEqual.equalTo("Node 10.8.8.2"));
	}

	//endregion
}
