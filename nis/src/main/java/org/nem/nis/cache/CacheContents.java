package org.nem.nis.cache;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents the contents of a cache.
 *
 * @param <T> The item type.
 */
public class CacheContents<T> implements Iterable<T> {
	private final Collection<T> contents;

	/**
	 * Creates a new cache contents object around the specified collection.
	 *
	 * @param source The source collection.
	 */
	public CacheContents(final Collection<? extends T> source) {
		this.contents = new ArrayList<>(source);
	}

	@Override
	public Iterator<T> iterator() {
		return this.contents.iterator();
	}

	/**
	 * Returns a collection containing the cache contents.
	 *
	 * @return The collection.
	 */
	public Collection<T> asCollection() {
		return this.contents;
	}

	/**
	 * Streams the cache contents.
	 *
	 * @return The contents.
	 */
	public Stream<T> stream() {
		return this.contents.stream();
	}
}
