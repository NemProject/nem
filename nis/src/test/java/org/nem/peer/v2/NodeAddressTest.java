package org.nem.peer.v2;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidParameterException;

public class NodeAddressTest {

    //region constructor

    @Test
    public void ctorCanCreateNewNodeAddress() throws Exception {
        // Act:
        final NodeAddress address = new NodeAddress("ftp", "10.8.8.2", 12);

        // Assert:
        final URL expectedUrl = new URL("ftp", "10.8.8.2", 12, "/");
        Assert.assertThat(address.getBaseUrl(), IsEqual.equalTo(expectedUrl));
        assertApiUrlsAreCorrect(expectedUrl, address);
    }

    @Test
    public void nodeAddressCanBeRoundTripped() throws Exception {
        // Arrange:
        final NodeAddress originalAddress = new NodeAddress("ftp", "10.8.8.2", 12);

        // Act:
        final NodeAddress address = new NodeAddress(Utils.roundtripSerializableEntity(originalAddress, null));

        // Assert:
        final URL expectedUrl = new URL("ftp", "10.8.8.2", 12, "/");
        Assert.assertThat(address.getBaseUrl(), IsEqual.equalTo(expectedUrl));
        assertApiUrlsAreCorrect(expectedUrl, address);
    }

    private static void assertApiUrlsAreCorrect(final URL url, final NodeAddress address) throws Exception {
        Assert.assertThat(address.getApiUrl(NodeApiId.REST_NODE_INFO), IsEqual.equalTo(new URL(url, "node/info")));
        Assert.assertThat(address.getApiUrl(NodeApiId.REST_ADD_PEER), IsEqual.equalTo(new URL(url, "peer/new")));
        Assert.assertThat(address.getApiUrl(NodeApiId.REST_NODE_PEER_LIST), IsEqual.equalTo(new URL(url, "node/peer-list")));
        Assert.assertThat(address.getApiUrl(NodeApiId.REST_CHAIN), IsEqual.equalTo(new URL(url, "chain")));
    }

    @Test(expected = InvalidParameterException.class)
    public void ctorFailsIfProtocolIsInvalid() throws Exception {
        // Act:
        new NodeAddress("xyz", "10.8.8.2", 12);
    }

    @Test(expected = InvalidParameterException.class)
    public void ctorFailsIfProtocolIsHostIsInvalid() throws Exception {
        // Act:
        new NodeAddress("ftp", "10.8.8.2.1", 12);
    }

    @Test
    public void blankAddressResolvesToLocalHost() throws Exception {
        // Assert:
        assertHostNameResolvesToLocalhost(null);
        assertHostNameResolvesToLocalhost("");
    }

    private static void assertHostNameResolvesToLocalhost(final String hostName) {
        // Act:
        final NodeAddress address = new NodeAddress("ftp", hostName, 12);

        // Assert:
        Assert.assertThat(address.getBaseUrl().getHost(), IsEqual.equalTo("localhost"));
    }

    //endregion

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        NodeAddress address = new NodeAddress("ftp", "10.8.8.2", 12);

        // Assert:
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.2", 12), IsEqual.equalTo(address));
        Assert.assertThat(new NodeAddress("http", "10.8.8.2", 12), IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.1", 12), IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.2", 13), IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(address)));
        Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)address)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        NodeAddress address = new NodeAddress("ftp", "10.8.8.2", 12);
        int hashCode = address.hashCode();

        // Assert:
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.2", 12).hashCode(), IsEqual.equalTo(hashCode));
        Assert.assertThat(new NodeAddress("http", "10.8.8.2", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.1", 12).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(new NodeAddress("ftp", "10.8.8.2", 13).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
    }

    //endregion
}
