package org.nem.nis;

import org.junit.*;

public class NisPeerNetworkHostTest {

    @Test
    public void defaultHostCanBeBooted() {
        // Arrange:
        final NisPeerNetworkHost host = new NisPeerNetworkHost();

        // Act:
        host.boot();
    }
}
