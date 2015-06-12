package org.nem.nis.cache;

import org.nem.core.model.namespace.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * General class for holding namespaces.
 */
public class DefaultNamespaceCache implements NamespaceCache, CopyableCache<DefaultNamespaceCache> {

	private final ConcurrentHashMap<NamespaceId, Namespace> hashMap;

	public DefaultNamespaceCache() {
		this(1000);
	}

	private DefaultNamespaceCache(final int size) {
		this.hashMap = new ConcurrentHashMap<>(size);
	}

	@Override
	public void set(final Namespace namespace) {
		if (this.contains(namespace.getId())) {
			throw new IllegalArgumentException(String.format("namespace with id %s already exists in cache", namespace.getId()));
		}

		this.hashMap.put(namespace.getId(), namespace);
	}

	@Override
	public Namespace get(final NamespaceId id) {
		return this.hashMap.get(id);
	}

	@Override
	public boolean contains(final NamespaceId id) {
		return this.hashMap.containsKey(id);
	}

	@Override
	public void shallowCopyTo(final DefaultNamespaceCache cache) {
		cache.hashMap.clear();
		cache.hashMap.putAll(this.hashMap);
	}

	@Override
	public DefaultNamespaceCache copy() {
		// note that this is really creating a shallow copy, which has the effect of a deep copy
		// because hash map keys and values are immutable
		final DefaultNamespaceCache copy = new DefaultNamespaceCache(this.hashMap.size());
		copy.hashMap.putAll(this.hashMap);
		return copy;
	}
}
