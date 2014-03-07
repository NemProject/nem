package org.nem.peer.v2;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;

import java.net.URL;
import java.util.*;

public class ConfigTest {

    @Test
    public void networkNameIsInitializedCorrectly() {
        // Arrange:
        final Config config = createTestConfig();

        // Assert:
        Assert.assertThat(config.getNetworkName(), IsEqual.equalTo("Default Network"));
    }

    @Test
    public void localNodeIsInitializedCorrectly() throws Exception {
        // Arrange:
        final Config config = createTestConfig();

        // Act:
        final Node localNode = config.getLocalNode();
        final NodeInfo info = localNode.getInfo();

        // Assert:
        Assert.assertThat(info.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("http", "10.0.0.8", 7890, "/")));
        Assert.assertThat(info.getPlatform(), IsEqual.equalTo("Mac"));
        Assert.assertThat(info.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(info.getApplication(), IsEqual.equalTo("FooBar"));
        Assert.assertThat(localNode.getStatus(), IsEqual.equalTo(NodeStatus.INACTIVE));
    }

    @Test
     public void wellKnownPeersAreInitializedCorrectly() {
        // Arrange:
        final Config config = createTestConfig();

        // Act:
        final Set<NodeEndpoint> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
        Assert.assertThat(wellKnownPeers.contains(new NodeEndpoint("ftp", "10.0.0.5", 12)), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains(new NodeEndpoint("ftp", "10.0.0.8", 12)), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains(new NodeEndpoint("ftp", "10.0.0.3", 12)), IsEqual.equalTo(true));

    }

    @Test
    public void wellKnownPeersAreEmptyIfNotSpecified() {
        // Arrange:
        final JSONObject jsonConfig = createTestJsonConfig();
        jsonConfig.remove("knownPeers");
        final Config config = createTestConfig(jsonConfig);

        // Act:
        final Set<NodeEndpoint> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(0));
    }

    //region Factories

    private static JSONObject createEndpointJsonObject(final String protocol, final String host, final int port) {
        JSONObject jsonEndpoint = new JSONObject();
        jsonEndpoint.put("protocol", protocol);
        jsonEndpoint.put("host", host);
        jsonEndpoint.put("port", port);
        return jsonEndpoint;
    }

    private static JSONObject createTestJsonConfig(final String[] hostNames) {
        JSONObject jsonConfig = new JSONObject();

        jsonConfig.put("endpoint", createEndpointJsonObject("http", "10.0.0.8", 7890));

        jsonConfig.put("platform", "Mac");
        jsonConfig.put("application", "FooBar");

        JSONArray jsonWellKnownPeers = new JSONArray();
        for (final String hostName : hostNames)
            jsonWellKnownPeers.add(createEndpointJsonObject("ftp", hostName, 12));

        jsonConfig.put("knownPeers", jsonWellKnownPeers);
        return jsonConfig;
    }

    private static JSONObject createTestJsonConfig() {
        return createTestJsonConfig(new String[] { "10.0.0.5", "10.0.0.8", "10.0.0.3" });
    }

    private static Config createTestConfig() {
        return createTestConfig(createTestJsonConfig());
    }

    private static Config createTestConfig(final JSONObject jsonConfig) {
        return new Config(jsonConfig);
    }

    //endregion
}
