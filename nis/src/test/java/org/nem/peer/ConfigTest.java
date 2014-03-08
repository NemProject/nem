package org.nem.peer;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.*;

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
        final Node node = config.getLocalNode();

        // Assert:
        Assert.assertThat(node.getEndpoint().getBaseUrl(), IsEqual.equalTo(new URL("http", "10.0.0.8", 7890, "/")));
        Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Mac"));
        Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(node.getApplication(), IsEqual.equalTo("FooBar"));
    }

    @Test
     public void wellKnownPeersAreInitializedCorrectly() {
        // Arrange:
        final String[] knownHosts = new String[] { "10.0.0.5", "10.0.0.8", "10.0.0.3" };
        final Config config = new Config(ConfigFactory.createTestJsonConfig(knownHosts));

        // Act:
        final Set<NodeEndpoint> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
        Assert.assertThat(wellKnownPeers.contains(createConfigEndpoint("10.0.0.5")), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains(createConfigEndpoint("10.0.0.8")), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains(createConfigEndpoint("10.0.0.3")), IsEqual.equalTo(true));
    }

    @Test
    public void wellKnownPeersAreEmptyIfNotSpecified() {
        // Arrange:
        final JSONObject jsonConfig = ConfigFactory.createTestJsonConfig();
        jsonConfig.remove("knownPeers");
        final Config config = new Config(jsonConfig);

        // Act:
        final Set<NodeEndpoint> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(0));
    }

    //region Factories

    private static NodeEndpoint createConfigEndpoint(final String host) {
        return new NodeEndpoint("ftp", host, 12);
    }

    private static Config createTestConfig() {
        return ConfigFactory.createDefaultTestConfig();
    }

    //endregion
}
