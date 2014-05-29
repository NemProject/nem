package org.nem.peer.node;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class NodeTest {

	private final static NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);

	@Test
	public void ctorCanCreateNewNode() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(DEFAULT_ENDPOINT));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
	}

	@Test
	public void nodeCanBeCreatedFromHost() {
		// Act:
		final Node node = Node.fromHost("10.7.6.5");

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(NodeEndpoint.fromHost("10.7.6.5")));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("PC"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("Unknown"));
	}

	@Test
	public void nodeCanBeRoundTripped() {
		// Arrange:
		final Node originalNode = new Node(DEFAULT_ENDPOINT, "plat", "app");

		// Act:
		final Node node = new Node(Utils.roundtripSerializableEntity(originalNode, null));

		// Assert:
		Assert.assertThat(node.getEndpoint(), IsEqual.equalTo(DEFAULT_ENDPOINT));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void endpointCannotBeNull() {
		// Act:
		new Node(null, "plat", "app");
	}

	@Test
	public void currentVersionIsAssumedIfVersionIsNotSpecified() {
		// Arrange:
		final Node originalNode = new Node(DEFAULT_ENDPOINT, "plat", "app");
		final JsonSerializer serializer = new JsonSerializer();
		originalNode.serialize(serializer);

		final JSONObject object = serializer.getObject();
		object.remove("version");

		// Act:
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);
		final Node node = new Node(deserializer);

		// Assert:
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
	}

	@Test
	public void platformIsOptional() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, null, "app");

		// Assert:
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("PC"));
	}

	@Test
	public void applicationIsOptional() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", null);

		// Assert:
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("Unknown"));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

		// Assert:
		Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app"), IsEqual.equalTo(node));
		Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app"), IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app"), IsEqual.equalTo(node));
		Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2"), IsEqual.equalTo(node));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(node)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)node)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");
		final int hashCode = node.hashCode();

		// Assert:
		Assert.assertThat(
				new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app").hashCode(),
				IsEqual.equalTo(hashCode));
		Assert.assertThat(
				new Node(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app").hashCode(),
				IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(
				new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app").hashCode(),
				IsEqual.equalTo(hashCode));
		Assert.assertThat(
				new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2").hashCode(),
				IsEqual.equalTo(hashCode));
	}

	//endregion

	//toString

	@Test
	public void toStringIncludesHost() {
		// Act:
		final Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

		// Assert:
		Assert.assertThat(node.toString(), IsEqual.equalTo("Node 10.8.8.2"));
	}

	//endregion
}
