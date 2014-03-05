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
        Assert.assertThat(info.getAddress().getBaseUrl(), IsEqual.equalTo(new URL("http", "10.0.0.8", 7890, "/")));
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
        final Set<String> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
        Assert.assertThat(wellKnownPeers.contains("Alpha"), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains("Sigma"), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains("Gamma"), IsEqual.equalTo(true));
    }

    @Test
    public void wellKnownPeersAreEmptyIfNotSpecified() {
        // Arrange:
        final JSONObject jsonConfig = createTestJsonConfig();
        jsonConfig.remove("knownPeers");
        final Config config = createTestConfig(jsonConfig);

        // Act:
        final Set<String> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(0));
    }

    @Test
    public void wellKnownPeersContainingAllWhitespaceAreIgnored() {
        // Arrange:
        final JSONObject jsonConfig = createTestJsonConfig(new String[] { "", "Beta", "  ", "Alpha", "\t \t" });
        final Config config = createTestConfig(jsonConfig);

        // Act:
        final Set<String> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(2));
        Assert.assertThat(wellKnownPeers.contains("Beta"), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains("Alpha"), IsEqual.equalTo(true));
    }

    //region Factories

    private static JSONObject createTestJsonConfig(final String[] hostNames) {
        JSONObject jsonConfig = new JSONObject();

        JSONObject jsonAddress = new JSONObject();
        jsonAddress.put("protocol", "http");
        jsonAddress.put("address", "10.0.0.8");
        jsonAddress.put("port", 7890);
        jsonConfig.put("address", jsonAddress);

        jsonConfig.put("platform", "Mac");
        jsonConfig.put("application", "FooBar");

        JSONArray jsonWellKnownPeers = new JSONArray();
        Collections.addAll(jsonWellKnownPeers, hostNames);
        jsonConfig.put("knownPeers", jsonWellKnownPeers);
        return jsonConfig;
    }

    private static JSONObject createTestJsonConfig() {
        return createTestJsonConfig(new String[] { "Alpha", "Sigma", "Gamma" });
    }

    private static Config createTestConfig() {
        return createTestConfig(createTestJsonConfig());
    }

    private static Config createTestConfig(final JSONObject jsonConfig) {
        return new Config(jsonConfig);
    }

    //endregion
}
