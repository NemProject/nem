package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A synchronized hash cache implementation.
 */
public class SynchronizedHashCache implements HashCache, CopyableCache<SynchronizedHashCache>, CommittableCache {
	private final DefaultHashCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache The wrapped cache.
	 */
	public SynchronizedHashCache(final DefaultHashCache cache) {
		this.cache = cache;
	}

	@Override
	public void prune(final TimeInstant timeStamp) {
		synchronized (this.lock) {
			this.cache.prune(timeStamp);
		}
	}

	@Override
	public void put(final HashMetaDataPair pair) {
		synchronized (this.lock) {
			this.cache.put(pair);
		}
	}

	@Override
	public void putAll(final List<HashMetaDataPair> pairs) {
		synchronized (this.lock) {
			this.cache.putAll(pairs);
		}
	}

	@Override
	public void remove(final Hash hash) {
		synchronized (this.lock) {
			this.cache.remove(hash);
		}
	}

	@Override
	public void removeAll(final List<Hash> hashes) {
		synchronized (this.lock) {
			this.cache.removeAll(hashes);
		}
	}

	@Override
	public void clear() {
		synchronized (this.lock) {
			this.cache.clear();
		}
	}

	@Override
	public int getRetentionTime() {
		synchronized (this.lock) {
			return this.cache.getRetentionTime();
		}
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.cache.size();
		}
	}

	@Override
	public HashMetaData get(final Hash hash) {
		synchronized (this.lock) {
			return this.cache.get(hash);
		}
	}

	@Override
	public boolean hashExists(final Hash hash) {
		synchronized (this.lock) {
			return this.cache.hashExists(hash);
		}
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes) {
		synchronized (this.lock) {
			return this.cache.anyHashExists(hashes);
		}
	}

	// region SynchronizedHashCache

	@Override
	public void shallowCopyTo(final SynchronizedHashCache rhs) {
		synchronized (rhs.lock) {
			this.cache.shallowCopyTo(rhs.cache);
		}
	}

	@Override
	public SynchronizedHashCache copy() {
		synchronized (this.lock) {
			return new SynchronizedHashCache(this.cache.copy());
		}
	}

	// endregion

	// region CommitableCache

	@Override
	public void commit() {
		synchronized (this.lock) {
			this.cache.commit();
		}
	}

	// endregion

	public SynchronizedHashCache deepCopy() {
		synchronized (this.lock) {
			return new SynchronizedHashCache(this.cache.deepCopy());
		}
	}
}
