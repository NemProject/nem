package org.nem.peer.trust.simulation;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;
import org.nem.peer.trust.EigenTrust;
import org.nem.peer.trust.EigenTrustPlusPlus;
import org.nem.peer.trust.SimpleTrust;

// address;evil;pretrusted;honest data probability;honest feedback probability;leech;collusive

public class NetworkSimulatorTest {

//    @Test
//    public void testPeerNetworkNULL() {
//        try {
//            EigenTrust eigenTrust = new EigenTrust();
//            eigenTrust.analyze(null);
//            fail("Analyze with NULL parameter possible.");
//        } catch (IllegalArgumentException ex) {
//            // As expected
//        }
//    }

    @Test
    public void testNetworkSimulator() throws Exception {
            URL url = NetworkSimulator.class.getClassLoader().getResource("");
//            if (null == )

            // SimpleTrust
            final Config config = Config.loadFromFile("/Users/pjlongo/gits/nem-infrastructure-server/src/test/java/org/nem/peer/trust/simulation/Nodes.txt");
            final NetworkSimulator simulator = new NetworkSimulator(config, new SimpleTrust(), 0.1);
//            assertTrue(simulator.initialize(url.getFile() + "trust/Nodes.txt"));
            assertTrue(simulator.run(url.getFile() + "SimpleTrust.txt", 1000));

//            // EigenTrust
//            simulator = new NetworkSimulator(new EigenTrust(), 0.1);
//            assertTrue(simulator.initialize(url.getFile() + "trust/Nodes.txt"));
//            assertTrue(simulator.run(url.getFile() + "trust/" + simulator.getTrustModelName() + ".txt", 1000));
//
//            // EigenTrustPlusPlus
//            simulator = new NetworkSimulator(new EigenTrustPlusPlus(), 0.1);
//            assertTrue(simulator.initialize(url.getFile() + "trust/Nodes.txt"));
//            assertTrue(simulator.run(url.getFile() + "trust/" + simulator.getTrustModelName() + ".txt", 1000));
    }
}