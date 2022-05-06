package org.nem.peer.trust.simulation;

import org.hamcrest.core.IsEqual;
import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.nem.peer.trust.*;

import java.net.URL;
import java.util.*;
import java.util.logging.*;

public class NetworkSimulatorITCase {
	private static final Logger LOGGER = Logger.getLogger(NetworkSimulatorITCase.class.getName());

	@Test
	public void simulateEigenTrust() {
		// Act:
		runTest(new EigenTrust());
	}

	@Test
	public void simulateEigenPlusPlusTrust() {
		// Act:
		runTest(new EigenTrustPlusPlus());
	}

	private static void runTest(final EigenTrust trust) {
		final URL url = NetworkSimulator.class.getClassLoader().getResource("");
		if (null == url || null == url.getFile()) {
			throw new IllegalArgumentException("could not find output file");
		}

		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 10; ++i) {
			final Config config = getConfig(i * 0.10);
			final NetworkSimulator simulator = new NetworkSimulator(config, trust, 0.1);

			final String outputFileName = url.getFile() + String.format("%s_%d.txt", trust.getClass().getSimpleName(), i);

			final long startTime = System.currentTimeMillis();
			final boolean result = simulator.run(outputFileName, 1000);
			final long stopTime = System.currentTimeMillis();

			MatcherAssert.assertThat(result, IsEqual.equalTo(true));
			MatcherAssert.assertThat(trust.getNumComputations(), IsEqual.equalTo(trust.getNumConvergences()));
			builder.append(System.lineSeparator());
			builder.append(
					String.format(
							"Honest: %02d%% --> %06.3f%% failed; %04d ms",
							i * 10,
							simulator.getFailedPercentage(),
							stopTime - startTime));
		}

		LOGGER.log(Level.INFO, builder.toString());
	}

	private static Config getConfig(final double evilNodeHonestDataProbability) {
		// address;evil;pre-trusted;honest data probability;honest feedback probability;leech;collusive

		final List<Config.Entry> entries = new ArrayList<>();
		// pre-trusted
		entries.add(new Config.Entry("110.110.110.001", false, true, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.002", false, true, 1.0, 1.0, false, false));

		// good
		entries.add(new Config.Entry("110.110.110.001", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.002", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.003", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.004", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.005", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.006", false, false, 1.0, 1.0, false, false));
		entries.add(new Config.Entry("110.110.110.007", false, false, 1.0, 1.0, false, false));

		// evil
		entries.add(new Config.Entry("210.110.110.001", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.002", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.003", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.004", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.005", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.006", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.007", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.008", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.009", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		entries.add(new Config.Entry("210.110.110.010", true, false, evilNodeHonestDataProbability, 0.3, false, true));
		return new Config(entries);
	}
}
