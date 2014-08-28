package org.nem.nis.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.node.*;
import org.nem.nis.time.synchronization.*;
import org.nem.nis.time.synchronization.filter.FilterConstants;

import java.security.SecureRandom;
import java.util.*;

public class TimeSyncUtils {

	private static final KeyPair KEY_PAIR = new KeyPair();

	/**
	 * Creates count sorted synchronization samples with a time offset that is tolerable.
	 *
	 * @param startValue The time offset to start with.
	 * @param count The number of samples needed.
	 * @return the sorted list of samples
	 */
	public static List<SynchronizationSample> createTolerableSortedSamples(final long startValue, final int count) {
		final List<SynchronizationSample> samples = createTolerableSamples(startValue, count);
		Collections.sort(samples);

		return samples;
	}

	/**
	 * Creates count unsorted synchronization samples with a time offset that is tolerable.
	 *
	 * @param startValue The time offset to start with.
	 * @param count The number of samples needed.
	 * @return the unsorted list of samples
	 */
	public static List<SynchronizationSample> createTolerableUnsortedSamples(final long startValue, final int count) {
		final List<SynchronizationSample> samples = createTolerableSamples(startValue, count);
		Collections.shuffle(samples);

		return samples;
	}

	private static List<SynchronizationSample> createTolerableSamples(final long startValue, final int count) {
		final List<SynchronizationSample> samples = new ArrayList<>();
		for (int i=1; i<=count; i++) {
			samples.add(createSynchronizationSample(startValue + i));
		}

		return samples;
	}

	/**
	 * Creates count sorted synchronization samples with random time offsets that are all tolerable.
	 *
	 * @param count The number of samples needed.
	 * @param mean The mean time offset the samples should have.
	 * @return the sorted list of samples
	 */
	public static List<SynchronizationSample> createRandomTolerableSortedSamplesAroundMean(final int count, final long mean) {
		final List<SynchronizationSample> samples = createRandomTolerableSamplesAroundMean(count, mean);
		Collections.sort(samples);

		return samples;
	}

	/**
	 * Creates count unsorted synchronization samples with random time offsets that are all tolerable.
	 *
	 * @param count The number of samples needed.
	 * @param mean The mean time offset the samples should have.
	 * @return the unsorted list of samples
	 */
	public static List<SynchronizationSample> createRandomTolerableUnsortedSamplesAroundMean(final int count, final long mean) {
		final List<SynchronizationSample> samples = createRandomTolerableSamplesAroundMean(count, mean);
		Collections.shuffle(samples);

		return samples;
	}

	private static List<SynchronizationSample> createRandomTolerableSamplesAroundMean(final int count, final long mean) {
		final SecureRandom random = new SecureRandom();
		final List<SynchronizationSample> samples = new ArrayList<>();
		if (count % 2 == 1) {
			samples.add(createSynchronizationSample(mean));
		}
		for (int i = 0; i < count/2; i++) {
			final int value = random.nextInt(1000);
			samples.add(createSynchronizationSample(mean + value));
			samples.add(createSynchronizationSample(mean - value));
		}

		return samples;
	}

	/**
	 * Creates count synchronization samples with a time offset that is not tolerable.
	 *
	 * @param count The number of samples needed.
	 * @return the list of samples
	 */
	public static List<SynchronizationSample> createIntolerableSamples(final int count) {
		final List<SynchronizationSample> samples = new ArrayList<>();
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
	public static SynchronizationSample createSynchronizationSample(final long timeOffset) {
		return new SynchronizationSample(
				new Node(new NodeIdentity(KEY_PAIR, "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(0), new NetworkTimeStamp(10)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5 + timeOffset), new NetworkTimeStamp(5 + timeOffset)));
	}

	/**
	 * Creates a synchronization sample.
	 *
	 * @param keyPair The remote node's key pair.
	 * @param localSendTimeStamp The local send time stamp.
	 * @param localReceiveTimeStamp The local receive time stamp.
	 * @param remoteSendTimeStamp The remote send time stamp.
	 * @param remoteReceiveTimeStamp The remote receive time stamp.
	 * @return The synchronization sample
	 */
	public static SynchronizationSample createSynchronizationSample(
			final KeyPair keyPair,
			final long localSendTimeStamp,
			final long localReceiveTimeStamp,
			final long remoteSendTimeStamp,
			final long remoteReceiveTimeStamp) {
		return new SynchronizationSample(
				new Node(new NodeIdentity(keyPair, "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(localSendTimeStamp), new NetworkTimeStamp(localReceiveTimeStamp)),
				new CommunicationTimeStamps(new NetworkTimeStamp(remoteSendTimeStamp), new NetworkTimeStamp(remoteReceiveTimeStamp)));
	}

}
