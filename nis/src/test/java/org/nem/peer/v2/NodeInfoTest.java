package org.nem.peer.v2;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidParameterException;

public class NodeInfoTest {

    private final static NodeEndpoint DEFAULT_ENDPOINT = new NodeEndpoint("ftp", "10.8.8.2", 12);

    @Test
    public void ctorCanCreateNewNodeInfo() throws Exception {
        // Act:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");

        // Assert:
        Assert.assertThat(info.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
        Assert.assertThat(info.getPlatform(), IsEqual.equalTo("plat"));
        Assert.assertThat(info.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(info.getApplication(), IsEqual.equalTo("app"));
    }

    @Test
    public void nodeInfoCanBeRoundTripped() throws Exception {
        // Arrange:
        NodeInfo originalInfo = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");

        // Act:
        NodeInfo info = new NodeInfo(Utils.roundtripSerializableEntity(originalInfo, null));

        // Assert:
        Assert.assertThat(info.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
        Assert.assertThat(info.getPlatform(), IsEqual.equalTo("plat"));
        Assert.assertThat(info.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(info.getApplication(), IsEqual.equalTo("app"));
    }

    @Test(expected = InvalidParameterException.class)
    public void endpointCannotBeNull() {
        // Act:
        new NodeInfo(null, "plat", "app");
    }

    @Test
    public void currentVersionIsAssumedIfVersionIsNotSpecified() {
        // Arrange:
        NodeInfo originalInfo = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");
        JsonSerializer serializer = new JsonSerializer();
        originalInfo.serialize(serializer);

        JSONObject object = serializer.getObject();
        object.remove("version");

        // Act:
        JsonDeserializer deserializer = new JsonDeserializer(object, null);
        NodeInfo info = new NodeInfo(deserializer);

        // Assert:
        Assert.assertThat(info.getVersion(), IsEqual.equalTo(2));
    }

    @Test
    public void platformIsOptional() {
        // Act:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, null, "app");

        // Assert:
        Assert.assertThat(info.getPlatform(), IsEqual.equalTo("PC"));
    }

    @Test
    public void applicationIsOptional() {
        // Act:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, "plat", null);

        // Assert:
        Assert.assertThat(info.getApplication(), IsEqual.equalTo(null));
    }

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");

        // Assert:
        Assert.assertThat(new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app"), IsEqual.equalTo(info));
        Assert.assertThat(new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app"), IsNot.not(IsEqual.equalTo(info)));
        Assert.assertThat(new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app"), IsNot.not(IsEqual.equalTo(info)));
        Assert.assertThat(new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2"), IsNot.not(IsEqual.equalTo(info)));
        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(info)));
        Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)info)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        NodeInfo info = new NodeInfo(DEFAULT_ENDPOINT, "plat", "app");
        int hashCode = info.hashCode();

        // Assert:
        Assert.assertThat(
            new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app").hashCode(),
            IsEqual.equalTo(hashCode));
        Assert.assertThat(
            new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 13), "plat", "app").hashCode(),
            IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(
            new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat2", "app").hashCode(),
            IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(
            new NodeInfo(new NodeEndpoint("ftp", "10.8.8.2", 12), "plat", "app2").hashCode(),
            IsNot.not(IsEqual.equalTo(hashCode)));
    }

    //endregion
}
