package org.nem.nis.time.synchronization;

import org.junit.Test;
import org.nem.nis.time.synchronization.filter.*;

import java.util.*;
public class TimeSynchronizationITCase {

	@Test
	public void synchronizationAlgorithmConvergesInNormalNetwork() {
		final int numberOfNodes = 4;
		final int timeOffsetSpread = 50000;
		final int numberOfRounds = 20;
		final Network network = setupNetwork(numberOfNodes, 10*numberOfNodes, timeOffsetSpread);
		for (int i=0; i<numberOfRounds; i++) {
			network.log("Synchronization round " + i);
			network.outputNodes();
			network.statistics();
			synchronizationRound(network);
		}

	}

	private void synchronizationRound(Network network) {
		network.getNodes().stream()
				.forEach(n -> {
					//network.log("Synchronizing " + n.getName());
					Set<TimeAwareNode> partners = network.selectSyncPartnersForNode(n);
					List<SynchronizationSample> samples = network.createSynchronizationSamples(n, partners);
					n.updateNetworkTime(samples);
				});
	}

	private Network setupNetwork(
			final int numberOfNodes,
			final int viewSize,
			final int timeOffsetSpread) {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
		final SynchronizationStrategy syncStrategy = new DefaultSynchronizationStrategy(filter);
		return new Network(numberOfNodes, syncStrategy, timeOffsetSpread, true, viewSize);
	}
}
