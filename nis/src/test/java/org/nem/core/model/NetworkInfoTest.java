package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class NetworkInfoTest {

    @Test
    public void mainNetworkInfoIsCorrect() {
        // Arrange:
        final NetworkInfo info = NetworkInfo.getMainNetworkInfo();

        // Assert:
        Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x68));
        Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('N'));
        Assert.assertThat(info.getGenesisAccountId().charAt(0), IsEqual.equalTo('N'));
        for (final String accountId : info.getGenesisRecipientAccountIds())
            Assert.assertThat(accountId.charAt(0), IsEqual.equalTo('N'));
    }

    @Test
    public void testNetworkInfoIsCorrect() {
        // Arrange:
        final NetworkInfo info = NetworkInfo.getTestNetworkInfo();

        // Assert:
        Assert.assertThat(info.getVersion(), IsEqual.equalTo((byte)0x98));
        Assert.assertThat(info.getAddressStartChar(), IsEqual.equalTo('T'));
        Assert.assertThat(info.getGenesisAccountId().charAt(0), IsEqual.equalTo('T'));
        for (final String accountId : info.getGenesisRecipientAccountIds())
            Assert.assertThat(accountId.charAt(0), IsEqual.equalTo('T'));
    }

    @Test
    public void defaultNetworkIsTestNetwork() {
        // Assert:
        Assert.assertThat(
            NetworkInfo.getDefault(),
            IsSame.sameInstance(NetworkInfo.getTestNetworkInfo()));
    }
}
