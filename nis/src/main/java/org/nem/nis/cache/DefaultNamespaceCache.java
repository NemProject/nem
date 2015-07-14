package org.nem.nis.cache;

import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

/**
 * General class for holding namespaces.
 * Note that the namespace with id "nem" is handled in a special way.
 */
public class DefaultNamespaceCache implements NamespaceCache, CopyableCache<DefaultNamespaceCache> {
	private final HashMap<NamespaceId, RootNamespaceHistory> rootMap;

	/**
	 * Creates a new namespace cache.
	 */
	public DefaultNamespaceCache() {
		this(100);
	}

	private DefaultNamespaceCache(final int size) {
		this.rootMap = new HashMap<>(size);
	}

	//region ReadOnlyNamespaceCache

	@Override
	public int size() {
		return 1 + this.rootMap.values().stream()
				.map(nh -> 1 + nh.numActiveRootSubNamespaces())
				.reduce(0, Integer::sum);
	}

	@Override
	public int deepSize() {
		return 1 + this.rootMap.values().stream()
				.map(nh -> nh.historyDepth() + nh.numAllHistoricalSubNamespaces())
				.reduce(0, Integer::sum);
	}

	@Override
	public Namespace get(final NamespaceId id) {
		if (id.equals(NamespaceConstants.NAMESPACE_ID_NEM)) {
			return NamespaceConstants.ROOT_NAMESPACE_NEM;
		}

		final RootNamespaceHistory history = this.getHistory(id);
		if (null == history) {
			return null;
		}

		return id.isRoot() ? history.last().root() : history.last().get(id);
	}

	@Override
	public boolean contains(final NamespaceId id) {
		return id.equals(NamespaceConstants.NAMESPACE_ID_NEM) || null != this.get(id);
	}

	@Override
	public boolean isActive(final NamespaceId id, final BlockHeight height) {
		if (id.equals(NamespaceConstants.NAMESPACE_ID_NEM)) {
			return true;
		}

		final RootNamespace root = this.getRootAtHeight(id, height);
		return null != root && (root.root().getId().equals(id) || null != root.get(id));
	}

	private RootNamespaceHistory getHistory(final NamespaceId id) {
		return this.rootMap.get(id.getRoot());
	}

	private RootNamespace getRootAtHeight(final NamespaceId id, final BlockHeight height) {
		final RootNamespaceHistory history = this.getHistory(id);
		return null == history ? null : history.firstActiveOrDefault(height);
	}

	//endregion

	//region NamespaceCache

	@Override
	public void add(final Namespace namespace) {
		if (!namespace.getId().isRoot()) {
			final RootNamespace root = this.checkSubNamespaceAndGetRoot(namespace);
			root.add(namespace);
			return;
		}

		if (!this.rootMap.containsKey(namespace.getId())) {
			this.rootMap.put(namespace.getId(), new RootNamespaceHistory(namespace));
			return;
		}

		final RootNamespaceHistory roots = this.rootMap.get(namespace.getId());
		roots.push(namespace);
	}

	private RootNamespace checkSubNamespaceAndGetRoot(final Namespace namespace) {
		// root must exist
		final NamespaceId rootId = namespace.getId().getRoot();
		final RootNamespaceHistory root = this.rootMap.get(rootId);
		if (null == root) {
			throw new IllegalArgumentException(String.format("root '%s' does not exist in cache", rootId));
		}

		return root.last();
	}

	@Override
	public void remove(final NamespaceId id) {
		if (!this.contains(id)) {
			throw new IllegalArgumentException(String.format("namespace with id '%s' not found in cache", id));
		}

		if (id.isRoot()) {
			this.removeRoot(id);
		} else {
			final RootNamespaceHistory root = this.rootMap.get(id.getRoot());
			root.last().remove(id);
		}
	}

	private void removeRoot(final NamespaceId rootId) {
		final RootNamespaceHistory roots = this.rootMap.get(rootId);
		assert !roots.isEmpty();

		if (1 == roots.historyDepth() && 0 != roots.last().size()) {
			throw new IllegalArgumentException(String.format("root '%s' cannot be removed because it has descendants", rootId));
		}

		roots.pop();
		if (roots.isEmpty()) {
			this.rootMap.remove(rootId);
		}
	}

	@Override
	public void prune(final BlockHeight height) {
		final HashSet<NamespaceId> rootsToRemove = new HashSet<>();
		for (final Map.Entry<NamespaceId, RootNamespaceHistory> entry : this.rootMap.entrySet()) {
			final NamespaceId rootId = entry.getKey();
			entry.getValue().prune(height);

			if (entry.getValue().isEmpty()) {
				rootsToRemove.add(rootId);
			}
		}

		rootsToRemove.forEach(this.rootMap::remove);
	}

	//endregion

	//region CopyableCache

	@Override
	public void shallowCopyTo(final DefaultNamespaceCache cache) {
		cache.rootMap.clear();
		cache.rootMap.putAll(this.rootMap);
	}

	@Override
	public DefaultNamespaceCache copy() {
		// note that hash keys are immutable
		final DefaultNamespaceCache copy = new DefaultNamespaceCache(this.size());
		this.rootMap.entrySet().stream().forEach(e -> copy.rootMap.put(e.getKey(), e.getValue().copy()));
		return copy;
	}

	//endregion

	//region RootNamespace

	private static class RootNamespace {
		private final Namespace root;
		private final Set<NamespaceId> children;

		public RootNamespace(final Namespace root) {
			this.root = root;
			this.children = new HashSet<>();
		}

		public RootNamespace(final Namespace root, final Collection<NamespaceId> children) {
			this.root = root;
			this.children = new HashSet<>(children);
		}

		public int size() {
			return this.children.size();
		}

		public Namespace root() {
			return this.root;
		}

		public Set<NamespaceId> children() {
			return Collections.unmodifiableSet(this.children);
		}

		public Namespace get(final NamespaceId id) {
			return this.children.contains(id)
					? new Namespace(id, this.root.getOwner(), this.root.getHeight())
					: null;
		}

		public void add(final Namespace namespace) {
			// sub-namespace is not renewable
			if (this.children.contains(namespace.getId())) {
				throw new IllegalArgumentException(String.format("namespace with id '%s' already exists in cache", namespace.getId()));
			}

			// parent must exist
			final NamespaceId parentId = namespace.getId().getParent();
			if (!this.children.contains(parentId) && !parentId.equals(this.root.getId())) {
				throw new IllegalArgumentException(String.format("parent '%s' does not exist in cache", parentId));
			}

			// must have same owner as root
			if (!this.root().getOwner().equals(namespace.getOwner())) {
				throw new IllegalArgumentException(String.format("cannot add sub-namespace '%s' with different owner than root namespace", namespace.getId()));
			}

			this.children.add(namespace.getId());
		}

		public void remove(final NamespaceId id) {
			final boolean hasDescendants = this.children.stream()
					.filter(fid -> id.equals(fid.getParent()))
					.findAny()
					.isPresent();

			if (hasDescendants) {
				throw new IllegalArgumentException(String.format("namespace '%s' cannot be removed because it has descendants", id));
			}

			this.children.remove(id);
		}

		public RootNamespace copy() {
			// note that namespaces and namespace ids are immutable
			final RootNamespace copy = new RootNamespace(this.root);
			copy.children.addAll(this.children);
			return copy;
		}
	}

	//endregion

	//region RootNamespaceHistory

	private static class RootNamespaceHistory {
		private final List<RootNamespace> namespaces = new ArrayList<>();

		public RootNamespaceHistory(final Namespace namespace) {
			this.push(namespace);
		}

		private RootNamespaceHistory() {
		}

		public boolean isEmpty() {
			return this.namespaces.isEmpty();
		}

		public int numActiveRootSubNamespaces() {
			return this.namespaces.isEmpty()
					? 0
					: this.last().size();
		}

		public int historyDepth() {
			return this.namespaces.size();
		}

		public int numAllHistoricalSubNamespaces() {
			return this.namespaces.stream().map(RootNamespace::size).reduce(0, Integer::sum);
		}

		public void push(final Namespace namespace) {
			Set<NamespaceId> children = Collections.emptySet();
			if (!this.namespaces.isEmpty()) {
				// if the new namespace has the same owner as the previous, carry over the subnamespaces
				final RootNamespace previousNamespace = this.last();
				if (namespace.getOwner().equals(previousNamespace.root.getOwner())) {
					children = previousNamespace.children();
				}
			}

			final RootNamespace newNamespace = new RootNamespace(namespace, children);
			this.namespaces.add(newNamespace);
		}

		public void pop() {
			this.namespaces.remove(this.namespaces.size() - 1);
		}

		public RootNamespace last() {
			return this.namespaces.get(this.namespaces.size() - 1);
		}

		public RootNamespace firstActiveOrDefault(final BlockHeight height) {
			return this.namespaces.stream()
					.filter(rn -> rn.root().isActive(height))
					.findFirst()
					.orElse(null);
		}

		public void prune(final BlockHeight height) {
			this.namespaces.removeIf(rn -> rn.root().getHeight().compareTo(height) < 0);
		}

		public RootNamespaceHistory copy() {
			final RootNamespaceHistory copy = new RootNamespaceHistory();
			this.namespaces.forEach(rn -> copy.namespaces.add(rn.copy()));
			return copy;
		}
	}

	//endregion
}
