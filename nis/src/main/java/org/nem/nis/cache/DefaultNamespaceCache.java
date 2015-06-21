package org.nem.nis.cache;

import org.nem.core.model.namespace.*;

import java.util.*;

/**
 * General class for holding namespaces.
 */
public class DefaultNamespaceCache implements NamespaceCache, CopyableCache<DefaultNamespaceCache> {
	private final HashMap<NamespaceId, Namespace> hashMap;
	private final HashMap<NamespaceId, List<Namespace>> rootMap;

	/**
	 * Creates a new namespace cache.
	 */
	public DefaultNamespaceCache() {
		this(1000);
	}

	private DefaultNamespaceCache(final int size) {
		this.hashMap = new HashMap<>(size);
		this.rootMap = new HashMap<>(size / 10);
	}

	@Override
	public int size() {
		return this.hashMap.size();
	}

	@Override
	public void add(final Namespace namespace) {
		if (!namespace.getId().isRoot()) {
			if (this.contains(namespace.getId())) {
				throw new IllegalArgumentException(String.format("namespace with id '%s' already exists in cache", namespace.getId()));
			}

			// inherit owner and height from root.
			final Namespace root = this.hashMap.get(namespace.getId().getRoot());
			final Namespace newNamespace = new Namespace(namespace.getId(), root.getOwner(), root.getExpiryHeight());
			this.hashMap.put(namespace.getId(), newNamespace);
			return;
		}

		if (!rootMap.containsKey(namespace.getId())) {
			this.rootMap.put(namespace.getId(), new ArrayList<>(Collections.singletonList(namespace)));
			this.hashMap.put(namespace.getId(), namespace);
			return;
		}

		final List<Namespace> roots = this.rootMap.get(namespace.getId());
		roots.add(namespace);
		this.updateNamespaces(namespace);
	}

	@Override
	public void remove(final NamespaceId id) {
		if (!this.contains(id)) {
			throw new IllegalArgumentException(String.format("namespace with id '%s' not found in cache", id));
		}

		this.hashMap.remove(id);
		if (id.isRoot()) {
			final List<Namespace> roots = this.rootMap.get(id);
			assert !roots.isEmpty();
			roots.remove(roots.size() - 1);
			if (!roots.isEmpty()) {
				this.updateNamespaces(roots.get(roots.size() - 1));
			} else {
				this.hashMap.remove(id);
			}
		}
	}

	private void updateNamespaces(final Namespace root) {
		final HashMap<NamespaceId, Namespace> alteredNamespaces = new HashMap<>();
		this.hashMap.entrySet().stream()
				.filter(e -> e.getKey().getRoot().equals(root.getId()))
				.forEach(e -> {
					final Namespace newNamespace = new Namespace(e.getValue().getId(), root.getOwner(), root.getExpiryHeight());
					alteredNamespaces.put(newNamespace.getId(), newNamespace);
				});
		this.hashMap.putAll(alteredNamespaces);
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
		cache.rootMap.clear();
		cache.hashMap.putAll(this.hashMap);
		cache.rootMap.putAll(this.rootMap);
	}

	@Override
	public DefaultNamespaceCache copy() {
		// note that this is really creating a shallow copy for the hashMap property, which has the effect of a deep copy
		// because hash map keys and values are immutable.
		final DefaultNamespaceCache copy = new DefaultNamespaceCache(this.hashMap.size());
		copy.hashMap.putAll(this.hashMap);
		this.rootMap.entrySet().stream().forEach(e -> copy.rootMap.put(e.getKey(), new ArrayList<>(e.getValue())));
		return copy;
	}
}
