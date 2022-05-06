package org.nem.nis.cache;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.nem.core.model.mosaic.*;
import org.nem.nis.dbmodel.DbMosaicId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Cache which holds a bidirectional MosaicId <--> DbMosaicId map.
 */
public class DefaultMosaicIdCache implements MosaicIdCache {
	private final Map<DbMosaicId, DbMosaicIds> dbMosaicIdsMap = new ConcurrentHashMap<>();
	private final BidiMap<MosaicId, DbMosaicId> map = new DualHashBidiMap<>();

	/**
	 * Creates a cache.
	 */
	public DefaultMosaicIdCache() {
		this.clear();
	}

	// region ReadOnlyMosaicIdCache

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public int deepSize() {
		return (int) this.dbMosaicIdsMap.values().stream().flatMap(DbMosaicIds::stream).distinct().count();
	}

	@Override
	public DbMosaicId get(final MosaicId mosaicId) {
		return this.map.get(mosaicId);
	}

	@Override
	public MosaicId get(final DbMosaicId dbMosaicId) {
		final DbMosaicIds dbMosaicIds = this.dbMosaicIdsMap.get(dbMosaicId);
		return null == dbMosaicIds ? null : this.map.inverseBidiMap().get(dbMosaicIds.last());
	}

	@Override
	public boolean contains(final MosaicId mosaicId) {
		return this.map.containsKey(mosaicId);
	}

	@Override
	public boolean contains(final DbMosaicId dbMosaicId) {
		return this.dbMosaicIdsMap.containsKey(dbMosaicId);
	}

	// endregion

	// region MosaicIdCache

	@Override
	public void add(final MosaicId mosaicId, final DbMosaicId dbMosaicId) {
		if (this.contains(dbMosaicId)) {
			final String message = String.format("mapping for db mosaic id '%d' is already known", dbMosaicId.getId());
			throw new IllegalArgumentException(message);
		}

		final DbMosaicId curDbMosaicId = this.map.get(mosaicId);
		if (null == curDbMosaicId) {
			this.dbMosaicIdsMap.put(dbMosaicId, new DbMosaicIds(dbMosaicId));
		} else {
			final DbMosaicIds dbMosaicIds = this.dbMosaicIdsMap.get(curDbMosaicId);
			dbMosaicIds.add(dbMosaicId);
			this.dbMosaicIdsMap.put(dbMosaicId, dbMosaicIds);
		}

		this.map.put(mosaicId, dbMosaicId);
	}

	@Override
	public void remove(final MosaicId mosaicId) {
		final DbMosaicId dbMosaicId = this.map.get(mosaicId);
		if (null == dbMosaicId) {
			return;
		}

		final DbMosaicIds dbMosaicIds = this.dbMosaicIdsMap.get(dbMosaicId);
		dbMosaicIds.stream().forEach(this.dbMosaicIdsMap::remove);
		this.map.remove(mosaicId);
	}

	@Override
	public void remove(final DbMosaicId dbMosaicId) {
		final DbMosaicIds dbMosaicIds = this.dbMosaicIdsMap.get(dbMosaicId);
		if (null == dbMosaicIds) {
			return;
		}

		final MosaicId mosaicId = this.map.inverseBidiMap().get(dbMosaicIds.last());
		dbMosaicIds.remove(dbMosaicId);
		if (dbMosaicIds.isEmpty()) {
			this.map.remove(mosaicId);
		} else {
			this.map.put(mosaicId, dbMosaicIds.last());
		}

		this.dbMosaicIdsMap.remove(dbMosaicId);
	}

	@Override
	public void clear() {
		this.dbMosaicIdsMap.clear();
		this.map.clear();

		// add a mapping for nem.xem that maps it to a non-existent db entity (id 0)
		this.add(MosaicConstants.MOSAIC_ID_XEM, new DbMosaicId(0L));
	}

	// endregion

	private class DbMosaicIds {
		private final List<DbMosaicId> ids;

		private DbMosaicIds(final DbMosaicId id) {
			this.ids = new ArrayList<>(Collections.singletonList(id));
		}

		private boolean isEmpty() {
			return this.ids.isEmpty();
		}

		private DbMosaicId last() {
			return this.ids.get(this.ids.size() - 1);
		}

		private void add(final DbMosaicId id) {
			this.ids.add(id);
		}

		private void remove(final DbMosaicId id) {
			this.ids.remove(id);
		}

		private Stream<DbMosaicId> stream() {
			return this.ids.stream();
		}
	}
}
