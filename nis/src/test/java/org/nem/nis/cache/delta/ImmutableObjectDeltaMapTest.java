package org.nem.nis.cache.delta;

import java.util.Map;

public class ImmutableObjectDeltaMapTest extends DeltaMapTest<ImmutableObjectDeltaMap<Integer, String>> {

	@Override
	protected ImmutableObjectDeltaMap<Integer, String> createMapWithCapacity(final int capacity) {
		return new ImmutableObjectDeltaMap<>(capacity);
	}

	@Override
	protected ImmutableObjectDeltaMap<Integer, String> createMapWithValues(final Map<Integer, String> initialValues) {
		return new ImmutableObjectDeltaMap<>(initialValues);
	}
}
