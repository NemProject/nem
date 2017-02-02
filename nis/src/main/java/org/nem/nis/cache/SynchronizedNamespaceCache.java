package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.NamespaceEntry;

import java.util.Collection;

/**
 * A synchronized namespace cache implementation.
 */
public class SynchronizedNamespaceCache implements ExtendedNamespaceCache<SynchronizedNamespaceCache> {
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
	public int deepSize() {
		synchronized (this.lock) {
			return this.cache.deepSize();
		}
	}

	@Override
	public Collection<NamespaceId> getRootNamespaceIds() {
		synchronized (this.lock) {
			return this.cache.getRootNamespaceIds();
		}
	}

	@Override
	public NamespaceEntry get(final NamespaceId id) {
		synchronized (this.lock) {
			return this.cache.get(id);
		}
	}

	@Override
	public Collection<NamespaceId> getSubNamespaceIds(NamespaceId rootId) {
		synchronized (this.lock) {
			return this.cache.getSubNamespaceIds(rootId);
		}
	}

	@Override
	public boolean contains(final NamespaceId id) {
		synchronized (this.lock) {
			return this.cache.contains(id);
		}
	}

	@Override
	public boolean isActive(final NamespaceId id, final BlockHeight height) {
		synchronized (this.lock) {
			return this.cache.isActive(id, height);
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
	public void prune(final BlockHeight height) {
		synchronized (this.lock) {
			this.cache.prune(height);
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

	@Override
	public void commit() {
		synchronized (this.lock) {
			this.cache.commit();
		}
	}

	public SynchronizedNamespaceCache deepCopy() {
		synchronized (this.lock) {
			return new SynchronizedNamespaceCache(this.cache.deepCopy());
		}
	}
}
