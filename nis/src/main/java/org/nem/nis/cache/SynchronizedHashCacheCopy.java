package org.nem.nis.cache;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A synchronized hash cache copy implementation
 */
public class SynchronizedHashCacheCopy implements HashCache, CommittableCache {
	private final DefaultHashCacheCopy copy;
	private final Object lock = new Object();

	/**
	 * Creates a new copy.
	 *
	 * @param copy The wrapped copy.
	 */
	public SynchronizedHashCacheCopy(final DefaultHashCacheCopy copy) {
		this.copy = copy;
	}

	@Override
	public void commit() {
		synchronized (this.lock) {
			this.copy.commit();
		}
	}

	@Override
	public void prune(TimeInstant timeStamp) {
		synchronized (this.lock) {
			this.copy.prune(timeStamp);
		}
	}

	@Override
	public void put(HashMetaDataPair pair) {
		synchronized (this.lock) {
			this.copy.put(pair);
		}
	}

	@Override
	public void putAll(List<HashMetaDataPair> pairs) {
		synchronized (this.lock) {
			this.copy.putAll(pairs);
		}
	}

	@Override
	public void remove(Hash hash) {
		synchronized (this.lock) {
			this.copy.remove(hash);
		}
	}

	@Override
	public void removeAll(List<Hash> hashes) {
		synchronized (this.lock) {
			this.copy.removeAll(hashes);
		}
	}

	@Override
	public void clear() {
		synchronized (this.lock) {
			this.copy.clear();
		}
	}

	@Override
	public int getRetentionTime() {
		synchronized (this.lock) {
			return this.copy.getRetentionTime();
		}
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.copy.size();
		}
	}

	@Override
	public HashMetaData get(Hash hash) {
		synchronized (this.lock) {
			return this.copy.get(hash);
		}
	}

	@Override
	public boolean hashExists(Hash hash) {
		synchronized (this.lock) {
			return this.copy.hashExists(hash);
		}
	}

	@Override
	public boolean anyHashExists(Collection<Hash> hashes) {
		synchronized (this.lock) {
			return this.copy.anyHashExists(hashes);
		}
	}
}
