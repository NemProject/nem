package org.nem.peer;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidParameterException;

public class NodeTest {

    private final static NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);

    @Test
    public void ctorCanCreateNewNode() throws Exception {
        // Act:
        Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

        // Assert:
        Assert.assertThat(node.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
        Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
        Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
    }

    @Test
    public void nodeCanBeRoundTripped() throws Exception {
        // Arrange:
        Node originalNode = new Node(DEFAULT_ENDPOINT, "plat", "app");

        // Act:
        Node node = new Node(Utils.roundtripSerializableEntity(originalNode, null));

        // Assert:
        Assert.assertThat(node.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
        Assert.assertThat(node.getPlatform(), IsEqual.equalTo("plat"));
        Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(node.getApplication(), IsEqual.equalTo("app"));
    }

    @Test(expected = InvalidParameterException.class)
    public void endpointCannotBeNull() {
        // Act:
        new Node(null, "plat", "app");
    }

    @Test
    public void currentVersionIsAssumedIfVersionIsNotSpecified() {
        // Arrange:
        Node originalNode = new Node(DEFAULT_ENDPOINT, "plat", "app");
        JsonSerializer serializer = new JsonSerializer();
        originalNode.serialize(serializer);

        JSONObject object = serializer.getObject();
        object.remove("version");

        // Act:
        JsonDeserializer deserializer = new JsonDeserializer(object, null);
        Node node = new Node(deserializer);

        // Assert:
        Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
    }

    @Test
    public void platformIsOptional() {
        // Act:
        Node node = new Node(DEFAULT_ENDPOINT, null, "app");

        // Assert:
        Assert.assertThat(node.getPlatform(), IsEqual.equalTo("PC"));
    }

    @Test
    public void applicationIsOptional() {
        // Act:
        Node node = new Node(DEFAULT_ENDPOINT, "plat", null);

        // Assert:
        Assert.assertThat(node.getApplication(), IsEqual.equalTo(null));
    }

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

        // Assert:
        Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app"), IsEqual.equalTo(node));
        Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app"), IsNot.not(IsEqual.equalTo(node)));
        Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app"), IsEqual.equalTo(node));
        Assert.assertThat(new Node(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2"), IsEqual.equalTo(node));
        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(node)));
        Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)node)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");
        int hashCode = node.hashCode();

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
        Node node = new Node(DEFAULT_ENDPOINT, "plat", "app");

        // Assert:
        Assert.assertThat(node.toString(), IsEqual.equalTo("Node 10.8.8.2"));
    }

    //endregion
}
