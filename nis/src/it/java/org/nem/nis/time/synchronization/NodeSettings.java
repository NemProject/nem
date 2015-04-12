package org.nem.nis.time.synchronization;

/**
 * General settings for a time aware node.
 */
public class NodeSettings {

	private final int timeOffsetSpread;
	private final boolean delayCommunication;
	private final boolean asymmetricChannels;
	private final boolean unstableClock;
	private final boolean clockAdjustment;
	private final int percentageEvilNodes;
	private final double evilNodesCumulativeImportance;

	/**
	 * Creates a new node settings object.
	 *
	 * @param timeOffsetSpread The desired time offset spread.
	 * @param delayCommunication Value indicating if there should be a delay between receive and send timestamps.
	 * @param asymmetricChannels Value indicating whether the time that a packet needs to travel to/from a partner node is asymmetric.
	 * @param unstableClock Value indicating whether the node's clock is drifting with respect to real time.
	 * @param clockAdjustment Value indicating whether the node's clock is adjusted from time to time.
	 * @param percentageEvilNodes Value indicating what percentage of all nodes is evil.
	 */
	public NodeSettings(
			final int timeOffsetSpread,
			final boolean delayCommunication,
			final boolean asymmetricChannels,
			final boolean unstableClock,
			final boolean clockAdjustment,
			final int percentageEvilNodes,
			final double evilNodesCumulativeImportance) {
		this.timeOffsetSpread = timeOffsetSpread;
		this.delayCommunication = delayCommunication;
		this.asymmetricChannels = asymmetricChannels;
		this.unstableClock = unstableClock;
		this.clockAdjustment = clockAdjustment;
		this.percentageEvilNodes = percentageEvilNodes;
		this.evilNodesCumulativeImportance = evilNodesCumulativeImportance;
	}

	/**
	 * Gets the spread for the time offset.
	 */
	public int getTimeOffsetSpread() {
		return this.timeOffsetSpread;
	}

	/**
	 * Gets the value indicating if there should be a delay between receive and send timestamps.
	 */
	public boolean doesDelayCommunication() {
		return this.delayCommunication;
	}

	/**
	 * Gets the value indicating whether the time that a packet needs to travel to/from a partner node is asymmetric.
	 */
	public boolean hasAsymmetricChannels() {
		return this.asymmetricChannels;
	}

	/**
	 * Gets the value indicating whether the node's clock is drifting with respect to real time.
	 */
	public boolean hasUnstableClock() {
		return this.unstableClock;
	}

	/**
	 * Gets the value indicating whether the node's clock is adjusted from time to time.
	 */
	public boolean hasClockAdjustment() {
		return this.clockAdjustment;
	}

	/**
	 * Gets the value indicating what percentage of all nodes is evil.
	 */
	public int getPercentageEvilNodes() {
		return this.percentageEvilNodes;
	}

	/**
	 * Gets the cumulative importance of all evil nodes.
	 */
	public double getEvilNodesCumulativeImportance() {
		return this.evilNodesCumulativeImportance;
	}
}
