package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.NamespaceConstants;
import org.nem.nis.cache.delta.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * General class for holding namespaces. Note that the namespace with id "nem" is handled in a special way.
 */
public class DefaultNamespaceCache implements ExtendedNamespaceCache<DefaultNamespaceCache> {
	private final MutableObjectAwareDeltaMap<NamespaceId, RootNamespaceHistory> rootMap;
	private boolean isCopy = false;

	/**
	 * Creates a new namespace cache.
	 */
	public DefaultNamespaceCache() {
		this(100);
	}

	private DefaultNamespaceCache(final int size) {
		this.rootMap = new MutableObjectAwareDeltaMap<>(size);
	}

	private DefaultNamespaceCache(final MutableObjectAwareDeltaMap<NamespaceId, RootNamespaceHistory> rootMap) {
		this.rootMap = rootMap;
	}

	// region ReadOnlyNamespaceCache

	@Override
	public int size() {
		return 1 + this.rootMap.readOnlyEntrySet().stream().map(Map.Entry::getValue).map(nh -> 1 + nh.numActiveRootSubNamespaces())
				.reduce(0, Integer::sum);
	}

	@Override
	public int deepSize() {
		return 1 + this.rootMap.streamValues().map(nh -> nh.historyDepth() + nh.numAllHistoricalSubNamespaces()).reduce(0, Integer::sum);
	}

	@Override
	public Collection<NamespaceId> getRootNamespaceIds() {
		return this.rootMap.readOnlyEntrySet().stream().map(Map.Entry<NamespaceId, RootNamespaceHistory>::getKey)
				.collect(Collectors.toList());
	}

	public NamespaceEntry get(final NamespaceId id) {
		if (id.equals(MosaicConstants.NAMESPACE_ID_NEM)) {
			return NamespaceConstants.NAMESPACE_ENTRY_NEM;
		}

		final RootNamespaceHistory history = this.getHistory(id);
		if (null == history) {
			return null;
		}

		return id.isRoot() ? history.last().root() : history.last().get(id);
	}

	@Override
	public Collection<NamespaceId> getSubNamespaceIds(final NamespaceId rootId) {
		final RootNamespaceHistory history = this.rootMap.get(rootId);
		if (null == history || history.isEmpty()) {
			return Collections.emptyList();
		}

		return history.last().children().stream().map(cn -> cn.id).collect(Collectors.toList());
	}

	@Override
	public boolean contains(final NamespaceId id) {
		return id.equals(MosaicConstants.NAMESPACE_ID_NEM) || null != this.get(id);
	}

	@Override
	public boolean isActive(final NamespaceId id, final BlockHeight height) {
		if (id.equals(MosaicConstants.NAMESPACE_ID_NEM)) {
			return true;
		}

		final RootNamespace root = this.getRootAtHeight(id, height);
		return null != root && (root.rootNamespace().getId().equals(id) || null != root.get(id));
	}

	private RootNamespaceHistory getHistory(final NamespaceId id) {
		return this.rootMap.get(id.getRoot());
	}

	private RootNamespace getRootAtHeight(final NamespaceId id, final BlockHeight height) {
		final RootNamespaceHistory history = this.getHistory(id);
		return null == history ? null : history.firstActiveOrDefault(height);
	}

	// endregion

	// region NamespaceCache

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

	// endregion

	// region CopyableCache

	@Override
	public void shallowCopyTo(final DefaultNamespaceCache cache) {
		cache.rootMap.clear();
		this.rootMap.readOnlyEntrySet().forEach(e -> cache.rootMap.put(e.getKey(), e.getValue()));
	}

	@Override
	public DefaultNamespaceCache copy() {
		if (this.isCopy) {
			// TODO 20151013 J-J: add test for this case
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all.
		final MutableObjectAwareDeltaMap<NamespaceId, RootNamespaceHistory> rebasedDeltaMap = this.rootMap.rebase();
		final DefaultNamespaceCache copy = new DefaultNamespaceCache(rebasedDeltaMap);
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void commit() {
		// TODO 20151013 J-J: add test for commit
		this.rootMap.commit();
	}

	// endregion

	public DefaultNamespaceCache deepCopy() {
		// TODO 20151013 J-J: add test for deepCopy
		// note that hash keys are immutable
		final DefaultNamespaceCache copy = new DefaultNamespaceCache(this.size());
		this.rootMap.readOnlyEntrySet().forEach(e -> copy.rootMap.put(e.getKey(), e.getValue().copy()));
		return copy;
	}

	// region ChildNamespace

	private static class ChildNamespace {
		public final NamespaceId id;
		public final Mosaics mosaics;

		public ChildNamespace(final NamespaceId id) {
			this(id, new Mosaics(id));
		}

		public ChildNamespace(final NamespaceId id, final Mosaics mosaics) {
			this.id = id;
			this.mosaics = mosaics;
		}

		public ChildNamespace copy() {
			return new ChildNamespace(this.id, this.mosaics.copy());
		}
	}

	// endregion

	// region RootNamespace

	private static class RootNamespace {
		private final NamespaceEntry root;
		private final HashMap<NamespaceId, ChildNamespace> children;

		public RootNamespace(final NamespaceEntry root) {
			this.root = root;
			this.children = new HashMap<>();
		}

		public RootNamespace(final Namespace root, final Mosaics rootMosaics, final Collection<ChildNamespace> children) {
			this.root = new NamespaceEntry(root, rootMosaics);
			this.children = new HashMap<>(children.stream().collect(Collectors.toMap(cn -> cn.id, cn -> cn)));
		}

		public int size() {
			return this.children.size();
		}

		public NamespaceEntry root() {
			return this.root;
		}

		public Namespace rootNamespace() {
			return this.root.getNamespace();
		}

		public Collection<ChildNamespace> children() {
			return Collections.unmodifiableCollection(this.children.values());
		}

		public NamespaceEntry get(final NamespaceId id) {
			final ChildNamespace childNamespace = this.children.get(id);
			if (null == childNamespace) {
				return null;
			}

			final Namespace namespace = new Namespace(id, this.rootNamespace().getOwner(), this.rootNamespace().getHeight());
			return new NamespaceEntry(namespace, childNamespace.mosaics);
		}

		public void add(final Namespace namespace) {
			// sub-namespace is not renewable
			final NamespaceId id = namespace.getId();
			if (this.children.containsKey(id)) {
				throw new IllegalArgumentException(String.format("namespace with id '%s' already exists in cache", id));
			}

			// parent must exist
			final NamespaceId parentId = id.getParent();
			if (!this.children.containsKey(parentId) && !parentId.equals(this.rootNamespace().getId())) {
				throw new IllegalArgumentException(String.format("parent '%s' does not exist in cache", parentId));
			}

			// must have same owner as root
			if (!this.rootNamespace().getOwner().equals(namespace.getOwner())) {
				throw new IllegalArgumentException(
						String.format("cannot add sub-namespace '%s' with different owner than root namespace", id));
			}

			this.children.put(id, new ChildNamespace(id));
		}

		public void remove(final NamespaceId id) {
			final boolean hasDescendants = this.children.keySet().stream().anyMatch(fid -> id.equals(fid.getParent()));

			if (hasDescendants) {
				throw new IllegalArgumentException(String.format("namespace '%s' cannot be removed because it has descendants", id));
			}

			this.children.remove(id);
		}

		public RootNamespace copy() {
			// note that namespace ids are immutable
			final RootNamespace copy = new RootNamespace(this.root.copy());
			copy.children.putAll(this.children.values().stream().collect(Collectors.toMap(cn -> cn.id, ChildNamespace::copy)));
			return copy;
		}
	}

	// endregion

	// region RootNamespaceHistory

	private static class RootNamespaceHistory implements Copyable<RootNamespaceHistory> {
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
			return this.namespaces.isEmpty() ? 0 : this.last().size();
		}

		public int historyDepth() {
			return this.namespaces.size();
		}

		public int numAllHistoricalSubNamespaces() {
			return this.namespaces.stream().map(RootNamespace::size).reduce(0, Integer::sum);
		}

		public void push(final Namespace namespace) {
			Collection<ChildNamespace> children = Collections.emptySet();
			Mosaics rootMosaics = new Mosaics(namespace.getId());
			if (!this.namespaces.isEmpty()) {
				// if the new namespace has the same owner as the previous, carry over the root mosaics and sub-namespaces
				final RootNamespace previousNamespace = this.last();
				if (namespace.getOwner().equals(previousNamespace.rootNamespace().getOwner())) {
					rootMosaics = previousNamespace.root().getMosaics();
					children = previousNamespace.children();
				}
			}

			final RootNamespace newNamespace = new RootNamespace(namespace, rootMosaics, children);
			this.namespaces.add(newNamespace);
		}

		public void pop() {
			this.namespaces.remove(this.namespaces.size() - 1);
		}

		public RootNamespace last() {
			return this.namespaces.get(this.namespaces.size() - 1);
		}

		public RootNamespace firstActiveOrDefault(final BlockHeight height) {
			return this.namespaces.stream().filter(rn -> rn.rootNamespace().isActive(height)).findFirst().orElse(null);
		}

		public void prune(final BlockHeight height) {
			this.namespaces.removeIf(rn -> rn.rootNamespace().getHeight().compareTo(height) < 0);
		}

		@Override
		public RootNamespaceHistory copy() {
			final RootNamespaceHistory copy = new RootNamespaceHistory();
			this.namespaces.forEach(rn -> copy.namespaces.add(rn.copy()));
			return copy;
		}
	}

	// endregion
}
