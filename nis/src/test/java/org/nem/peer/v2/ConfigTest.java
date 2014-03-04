package org.nem.peer.v2;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;

import java.net.URL;
import java.util.Set;

public class ConfigTest {

    @Test
    public void networkNameIsInitializedCorrectly() {
        // Act:
        final Config config = createTestConfig();

        // Assert:
        Assert.assertThat(config.getNetworkName(), IsEqual.equalTo("Default Network"));
    }

    @Test
    public void localNodeIsInitializedCorrectly() throws Exception {
        // Act:
        final Config config = createTestConfig();
        final NodeInfo info = config.getLocalNode().getInfo();

        // Assert:
        Assert.assertThat(info.getAddress().getBaseUrl(), IsEqual.equalTo(new URL("http", "10.0.0.8", 80, "/")));
        Assert.assertThat(info.getPlatform(), IsEqual.equalTo("Mac"));
        Assert.assertThat(info.getProtocol(), IsEqual.equalTo("https"));
        Assert.assertThat(info.getVersion(), IsEqual.equalTo(2));
        Assert.assertThat(info.getApplication(), IsEqual.equalTo("FooBar"));
    }

    @Test
     public void wellKnownPeersAreInitializedCorrectly() {
        // Act:
        final Config config = createTestConfig();
        final Set<String> wellKnownPeers = config.getWellKnownPeers();

        // Assert:
        Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
        Assert.assertThat(wellKnownPeers.contains("Alpha"), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains("Sigma"), IsEqual.equalTo(true));
        Assert.assertThat(wellKnownPeers.contains("Gamma"), IsEqual.equalTo(true));
    }

    public static Config createTestConfig() {
        JSONObject jsonConfig = new JSONObject();
        jsonConfig.put("myAddress", "10.0.0.8");
        jsonConfig.put("myPlatform", "Mac");

        JSONArray jsonWellKnownPeers = new JSONArray();
        jsonWellKnownPeers.add("Alpha");
        jsonWellKnownPeers.add("Sigma");
        jsonWellKnownPeers.add("Gamma");
        jsonConfig.put("knownPeers", jsonWellKnownPeers);
        return new Config("FooBar", jsonConfig);
    }
}
