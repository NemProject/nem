package org.nem.nis.test;

import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.node.NodeEndpoint;
import org.nem.nis.time.synchronization.*;
import org.nem.nis.time.synchronization.filter.FilterConstants;

import java.util.*;

public class TimeSyncUtils {

	/**
	 * Creates count synchronization samples with a time offset that is tolerable.
	 *
	 * @param count The number of samples needed.
	 * @return the list of samples
	 */
	public static List<SynchronizationSample> createTolerableSamples(long startValue, int count, boolean sort) {
		List<SynchronizationSample> samples = new ArrayList<>();
		for (int i=1; i<=count; i++) {
			samples.add(createSynchronizationSample(startValue + i));
		}
		if (!sort) {
			Collections.shuffle(samples);
		}

		return samples;
	}

	/**
	 * Creates count synchronization samples with a time offset that is not tolerable.
	 *
	 * @param count The number of samples needed.
	 * @return the list of samples
	 */
	public static List<SynchronizationSample> createIntolerableSamples(int count) {
		List<SynchronizationSample> samples = new ArrayList<>();
		for (int i=1; i<=count; i++) {
			samples.add(createSynchronizationSample(FilterConstants.TOLERATED_DEVIATION_START + i));
		}

		return samples;
	}

	/**
	 * Creates a synchronization sample with a given time offset.
	 *
	 * @param timeOffset The time offset in ms.
	 * @return The synchronization sample
	 */
	public static SynchronizationSample createSynchronizationSample(long timeOffset) {
		return new SynchronizationSample(
				new NodeEndpoint("ftp", "10.8.8.2", 12),
				new CommunicationTimeStamps(new NetworkTimeStamp(0), new NetworkTimeStamp(10)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5 + timeOffset), new NetworkTimeStamp(5 + timeOffset)));
	}
}
