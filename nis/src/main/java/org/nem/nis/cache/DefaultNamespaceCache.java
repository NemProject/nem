package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
import java.util.stream.*;

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

	//region ReadOnlyNamespaceCache

	@Override
	public int size() {
		return this.hashMap.size();
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
	public boolean isActive(final NamespaceId id, final BlockHeight height) {
		final Namespace root = this.getRootAtHeight(id, height);
		final Namespace namespace = id.isRoot() ? root : this.hashMap.get(id);
		return null != namespace && null != root && root.getOwner().equals(namespace.getOwner());
	}

	private Namespace getRootAtHeight(final NamespaceId id, final BlockHeight height) {
		final List<Namespace> roots = this.rootMap.get(id.getRoot());
		return null == roots
				? null
				: roots.stream().filter(r -> r.isActive(height)).findFirst().orElse(null);
	}

	//endregion

	//region NamespaceCache

	@Override
	public void add(final Namespace namespace) {
		if (!namespace.getId().isRoot()) {
			// inherit owner and height from root
			final Namespace root = this.checkSubNamespaceAndGetRoot(namespace);
			if (!root.getOwner().equals(namespace.getOwner())) {
				throw new IllegalArgumentException(String.format("cannot add sub-namespace '%s' with different owner than root namespace", namespace.getId()));
			}

			final Namespace newNamespace = new Namespace(namespace.getId(), root.getOwner(), root.getHeight());
			this.hashMap.put(namespace.getId(), newNamespace);
			return;
		}

		if (!this.rootMap.containsKey(namespace.getId())) {
			this.rootMap.put(namespace.getId(), new ArrayList<>(Collections.singletonList(namespace)));
			this.hashMap.put(namespace.getId(), namespace);
			return;
		}

		final List<Namespace> roots = this.rootMap.get(namespace.getId());
		roots.add(namespace);
		this.updateNamespaces(namespace);
	}

	private Namespace checkSubNamespaceAndGetRoot(final Namespace namespace) {
		// sub-namespace is not renewable
		if (this.contains(namespace.getId())) {
			throw new IllegalArgumentException(String.format("namespace with id '%s' already exists in cache", namespace.getId()));
		}

		// root and parent must exist
		final NamespaceId rootId = namespace.getId().getRoot();
		final Namespace root = this.hashMap.get(rootId);
		if (null == root) {
			throw new IllegalArgumentException(String.format("root '%s' does not exist in cache", rootId));
		}

		final NamespaceId parentId = namespace.getId().getParent();
		if (!this.contains(parentId)) {
			throw new IllegalArgumentException(String.format("parent '%s' does not exist in cache", parentId));
		}

		return root;
	}

	@Override
	public void remove(final NamespaceId id) {
		if (!this.contains(id)) {
			throw new IllegalArgumentException(String.format("namespace with id '%s' not found in cache", id));
		}

		if (id.isRoot()) {
			if (!this.removeRoot(id)) {
				return;
			}
		} else {
			if (this.filterDescendantsByParent(id).findAny().isPresent()) {
				throw new IllegalArgumentException(String.format("namespace '%s' cannot be removed because it has descendants", id));
			}
		}

		this.hashMap.remove(id);
	}

	private boolean removeRoot(final NamespaceId rootId) {
		final List<Namespace> roots = this.rootMap.get(rootId);
		assert !roots.isEmpty();

		if (1 == roots.size() && this.filterDescendantsByRoot(rootId).filter(e -> !e.getKey().equals(rootId)).findAny().isPresent()) {
			throw new IllegalArgumentException(String.format("root '%s' cannot be removed because it has descendants", rootId));
		}

		roots.remove(roots.size() - 1);
		if (!roots.isEmpty()) {
			this.updateNamespaces(roots.get(roots.size() - 1));
			return false;
		}

		return true;
	}

	private void updateNamespaces(final Namespace root) {
		// allow the root owner to change, but don't allow any sub-namespace owners to change
		// only update sub-namespaces that have a root equal to the new (potentially changed) root
		final HashMap<NamespaceId, Namespace> alteredNamespaces = new HashMap<>();
		this.filterDescendantsByRoot(root.getId())
				.filter(e -> e.getValue().getOwner().equals(root.getOwner()) || e.getValue().getId().isRoot())
				.forEach(e -> {
					final Namespace newNamespace = new Namespace(e.getValue().getId(), root.getOwner(), root.getHeight());
					alteredNamespaces.put(newNamespace.getId(), newNamespace);
				});
		this.hashMap.putAll(alteredNamespaces);
	}

	private Stream<Map.Entry<NamespaceId, Namespace>> filterDescendantsByRoot(final NamespaceId rootId) {
		return this.hashMap.entrySet().stream()
				.filter(e -> rootId.equals(e.getKey().getRoot()));
	}

	private Stream<Map.Entry<NamespaceId, Namespace>> filterDescendantsByParent(final NamespaceId parentId) {
		return this.hashMap.entrySet().stream()
				.filter(e -> parentId.equals(e.getKey().getParent()));
	}

	@Override
	public void prune(final BlockHeight height) {
		final HashSet<NamespaceId> rootsToRemove = new HashSet<>();
		for (final Map.Entry<NamespaceId, List<Namespace>> entry : this.rootMap.entrySet()) {
			final NamespaceId rootId = entry.getKey();
			entry.getValue().removeIf(n -> n.getHeight().compareTo(height) < 0);

			if (entry.getValue().isEmpty()) {
				rootsToRemove.add(rootId);
			}
		}

		for (final NamespaceId rootId : rootsToRemove) {
			final List<NamespaceId> descendants = this.filterDescendantsByRoot(rootId).map(Map.Entry::getKey).collect(Collectors.toList());
			descendants.forEach(this.hashMap::remove);
			this.rootMap.remove(rootId);
		}
	}

	//endregion

	//region CopyableCache

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

	//endregion
}
