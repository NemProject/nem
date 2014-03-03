package org.nem.peer.v2;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidParameterException;

public class NodeAddressTest {

    //region constructor

    @Test
    public void ctorCanCreateNewNodeAddress() throws Exception {
        // Act:
        NodeAddress address = new NodeAddress("ftp", "10.8.8.2", 12);

        // Assert:
        Assert.assertThat(address.getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
    }

    @Test
    public void nodeAddressCanBeRoundTripped() throws Exception {
        // Arrange:
        NodeAddress originalAddress = new NodeAddress("ftp", "10.8.8.2", 12);

        // Act:
        NodeAddress address = new NodeAddress(Utils.roundtripSerializableEntity(originalAddress, null));

        // Assert:
        Assert.assertThat(address.getBaseUrl(), IsEqual.equalTo(new URL("ftp", "10.8.8.2", 12, "/")));
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
