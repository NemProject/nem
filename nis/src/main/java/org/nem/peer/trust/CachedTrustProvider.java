package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.time.*;

/**
 * TrustProvider decorator that caches trust calculation results for a specified period of time.
 */
public class CachedTrustProvider implements TrustProvider {
	private final TrustProvider trustProvider;
	private final int cacheTime;
	private final TimeProvider timeProvider;
	private final Object lock = new Object();
	private TimeInstant lastCacheTime;
	private ColumnVector lastTrustValues;

	/**
	 * Creates a new trust provider mask decorator.
	 *
	 * @param trustProvider The trust provider.
	 * @param cacheTime The amount of time to cache trust values
	 * @param timeProvider The time provider.
	 */
	public CachedTrustProvider(
			final TrustProvider trustProvider,
			final int cacheTime,
			final TimeProvider timeProvider) {
		this.trustProvider = trustProvider;
		this.cacheTime = cacheTime;
		this.timeProvider = timeProvider;
	}

	@Override
	public ColumnVector computeTrust(final TrustContext context) {
		// there shouldn't be much contention on this lock and it's better to prevent
		// multiple simultaneous trust calculations
		synchronized (this.lock) {
			final TimeInstant currentTime = this.timeProvider.getCurrentTime();
			if (null == this.lastTrustValues || currentTime.subtract(this.lastCacheTime) > this.cacheTime) {
				this.lastCacheTime = currentTime;
				this.lastTrustValues = this.trustProvider.computeTrust(context);
				this.lastTrustValues.normalize();
			}

			// return a copy of the trust values
			return this.lastTrustValues.add(0);
		}
	}
}
