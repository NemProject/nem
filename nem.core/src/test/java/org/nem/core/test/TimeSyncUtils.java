package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.core.time.NetworkTimeStamp;
import org.nem.core.time.synchronization.*;

import java.security.SecureRandom;
import java.util.*;

public class TimeSyncUtils {

	private static final long MINUTE = 60L * 1000L;
	private static final long TOLERATED_DEVIATION_START = 120 * MINUTE;
	private static final KeyPair KEY_PAIR = new KeyPair();

	/**
	 * Creates count sorted synchronization samples with a time offset that is tolerable.
	 *
	 * @param startValue The time offset to start with.
	 * @param count The number of samples needed.
	 * @return the sorted list of samples
	 */
	public static List<TimeSynchronizationSample> createTolerableSortedSamples(final long startValue, final int count) {
		final List<TimeSynchronizationSample> samples = createTolerableSamples(startValue, count);
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
	public static List<TimeSynchronizationSample> createTolerableUnsortedSamples(final long startValue, final int count) {
		final List<TimeSynchronizationSample> samples = createTolerableSamples(startValue, count);
		Collections.shuffle(samples);

		return samples;
	}

	private static List<TimeSynchronizationSample> createTolerableSamples(final long startValue, final int count) {
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			samples.add(createTimeSynchronizationSample(startValue + i));
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
	public static List<TimeSynchronizationSample> createRandomTolerableSortedSamplesAroundMean(final int count, final long mean) {
		final List<TimeSynchronizationSample> samples = createRandomTolerableSamplesAroundMean(count, mean);
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
	public static List<TimeSynchronizationSample> createRandomTolerableUnsortedSamplesAroundMean(final int count, final long mean) {
		final List<TimeSynchronizationSample> samples = createRandomTolerableSamplesAroundMean(count, mean);
		Collections.shuffle(samples);

		return samples;
	}

	private static List<TimeSynchronizationSample> createRandomTolerableSamplesAroundMean(final int count, final long mean) {
		final SecureRandom random = new SecureRandom();
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		if (count % 2 == 1) {
			samples.add(createTimeSynchronizationSample(mean));
		}
		for (int i = 0; i < count / 2; i++) {
			final int value = random.nextInt(1000);
			samples.add(createTimeSynchronizationSample(mean + value));
			samples.add(createTimeSynchronizationSample(mean - value));
		}

		return samples;
	}

	/**
	 * Creates count synchronization samples with a time offset that is not tolerable.
	 *
	 * @param count The number of samples needed.
	 * @return the list of samples
	 */
	public static List<TimeSynchronizationSample> createIntolerableSamples(final int count) {
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			samples.add(createTimeSynchronizationSample(TOLERATED_DEVIATION_START + i));
		}

		return samples;
	}

	/**
	 * Creates a synchronization sample with a given time offset.
	 *
	 * @param timeOffset The time offset in ms.
	 * @return The time synchronization sample
	 */
	private static TimeSynchronizationSample createTimeSynchronizationSample(final long timeOffset) {
		return new TimeSynchronizationSample(
				new Node(new NodeIdentity(KEY_PAIR, "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(0), new NetworkTimeStamp(10)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5 + timeOffset), new NetworkTimeStamp(5 + timeOffset)));
	}

	/**
	 * Creates a synchronization sample with a given time offset.
	 *
	 * @param startValue The time offset to start with.
	 * @param count The number of samples needed.
	 * @return The time synchronization sample
	 */
	public static List<TimeSynchronizationSample> createTimeSynchronizationSamplesWithDifferentKeyPairs(final int startValue, final int count) {
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			samples.add(createTimeSynchronizationSampleWithKeyPair(new KeyPair(), startValue + i));
		}

		return samples;
	}

	/**
	 * Creates a synchronization sample with a given time offset.
	 *
	 * @param count The number of samples needed.
	 * @param mean The mean time offset the samples should have.
	 * @return The time synchronization sample
	 */
	public static List<TimeSynchronizationSample> createRandomTolerableSamplesWithDifferentKeyPairsAroundMean(final int count, final long mean) {
		final SecureRandom random = new SecureRandom();
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		if (count % 2 == 1) {
			samples.add(createTimeSynchronizationSampleWithKeyPair(new KeyPair(), mean));
		}
		for (int i = 0; i < count / 2; i++) {
			final int value = random.nextInt(1000);
			samples.add(createTimeSynchronizationSampleWithKeyPair(new KeyPair(), mean + value));
			samples.add(createTimeSynchronizationSampleWithKeyPair(new KeyPair(), mean - value));
		}

		return samples;
	}

	/**
	 * Creates a synchronization sample with a given time offset.
	 *
	 * @param keyPair The key pair to tie the node to.
	 * @param timeOffset The time offset in ms.
	 * @return The time synchronization sample
	 */
	private static TimeSynchronizationSample createTimeSynchronizationSampleWithKeyPair(final KeyPair keyPair, final long timeOffset) {
		return new TimeSynchronizationSample(
				new Node(new NodeIdentity(keyPair, "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(0), new NetworkTimeStamp(10)),
				new CommunicationTimeStamps(new NetworkTimeStamp(5 + timeOffset), new NetworkTimeStamp(5 + timeOffset)));
	}

	/**
	 * Creates a synchronization sample with a given duration.
	 *
	 * @param duration The duration in ms.
	 * @return The time synchronization sample
	 */
	public static TimeSynchronizationSample createTimeSynchronizationSampleWithDuration(final long duration) {
		return new TimeSynchronizationSample(
				new Node(new NodeIdentity(new KeyPair(), "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(0), new NetworkTimeStamp(duration)),
				new CommunicationTimeStamps(new NetworkTimeStamp(duration / 2), new NetworkTimeStamp(duration / 2)));
	}

	/**
	 * Creates a synchronization sample.
	 *
	 * @param keyPair The remote node's key pair.
	 * @param localSendTimeStamp The local send time stamp.
	 * @param localReceiveTimeStamp The local receive time stamp.
	 * @param remoteSendTimeStamp The remote send time stamp.
	 * @param remoteReceiveTimeStamp The remote receive time stamp.
	 * @return The time synchronization sample
	 */
	public static TimeSynchronizationSample createTimeSynchronizationSample(
			final KeyPair keyPair,
			final long localSendTimeStamp,
			final long localReceiveTimeStamp,
			final long remoteSendTimeStamp,
			final long remoteReceiveTimeStamp) {
		return new TimeSynchronizationSample(
				new Node(new NodeIdentity(keyPair, "node"), new NodeEndpoint("http", "10.10.10.12", 13), null),
				new CommunicationTimeStamps(new NetworkTimeStamp(localSendTimeStamp), new NetworkTimeStamp(localReceiveTimeStamp)),
				new CommunicationTimeStamps(new NetworkTimeStamp(remoteSendTimeStamp), new NetworkTimeStamp(remoteReceiveTimeStamp)));
	}
}
