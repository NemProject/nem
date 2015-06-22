package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A synchronized namespace cache implementation.
 */
public class SynchronizedNamespaceCache implements NamespaceCache, CopyableCache<SynchronizedNamespaceCache> {
	private final DefaultNamespaceCache cache;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param cache The wrapped cache.
	 */
	public SynchronizedNamespaceCache(final DefaultNamespaceCache cache) {
		this.cache = cache;
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.cache.size();
		}
	}

	@Override
	public void add(final Namespace namespace) {
		synchronized (this.lock) {
			this.cache.add(namespace);
		}
	}

	@Override
	public void remove(final NamespaceId namespaceId) {
		synchronized (this.lock) {
			this.cache.remove(namespaceId);
		}
	}

	@Override
	public boolean isActive(final NamespaceId id, final BlockHeight height) {
		synchronized (this.lock) {
			return this.cache.isActive(id, height);
		}
	}


	@Override
	public Namespace get(final NamespaceId id) {
		synchronized (this.lock) {
			return this.cache.get(id);
		}
	}

	@Override
	public boolean contains(final NamespaceId id) {
		synchronized (this.lock) {
			return this.cache.contains(id);
		}
	}
	@Override
	public void shallowCopyTo(final SynchronizedNamespaceCache rhs) {
		synchronized (rhs.lock) {
			this.cache.shallowCopyTo(rhs.cache);
		}
	}

	@Override
	public SynchronizedNamespaceCache copy() {
		synchronized (this.lock) {
			return new SynchronizedNamespaceCache(this.cache.copy());
		}
	}
}
