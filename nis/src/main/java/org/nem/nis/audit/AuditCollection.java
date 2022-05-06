package org.nem.nis.audit;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeProvider;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A collection of audit entries.
 */
public class AuditCollection implements SerializableEntity {
	private final int maxEntries;
	private final TimeProvider timeProvider;
	private final List<AuditEntry> outstandingEntries = new ArrayList<>();
	private final Deque<AuditEntry> mostRecentEntries = new ArrayDeque<>();
	private final AtomicInteger counter = new AtomicInteger(0);

	/**
	 * Creates a new audit collection.
	 *
	 * @param maxEntries The maximum number of recent entries to keep.
	 * @param timeProvider The time provider.
	 */
	public AuditCollection(final int maxEntries, final TimeProvider timeProvider) {
		this.maxEntries = maxEntries;
		this.timeProvider = timeProvider;
	}

	/**
	 * Gets all outstanding entries that haven't been completed.
	 *
	 * @return All outstanding entries.
	 */
	public Collection<AuditEntry> getOutstandingEntries() {
		return this.outstandingEntries;
	}

	/**
	 * Gets the most recent entries that have been added.
	 *
	 * @return All outstanding entries.
	 */
	public Collection<AuditEntry> getMostRecentEntries() {
		return this.mostRecentEntries;
	}

	/**
	 * Adds an entry to the audit collection.
	 *
	 * @param host The host.
	 * @param path The path.
	 */
	public void add(final String host, final String path) {
		final AuditEntry entry = new AuditEntry(this.counter.incrementAndGet(), host, path, this.timeProvider);

		synchronized (this.mostRecentEntries) {
			if (this.mostRecentEntries.size() >= this.maxEntries) {
				this.mostRecentEntries.removeLast();
			}

			this.mostRecentEntries.addFirst(entry);
		}

		synchronized (this.outstandingEntries) {
			this.outstandingEntries.add(entry);
		}
	}

	/**
	 * Removes an entry from the audit collection.
	 *
	 * @param host The host.
	 * @param path The path.
	 */
	public void remove(final String host, final String path) {
		final AuditEntry entry = new AuditEntry(-1, host, path, this.timeProvider);

		synchronized (this.outstandingEntries) {
			this.outstandingEntries.remove(entry);
		}
	}

	@Override
	public void serialize(final Serializer serializer) {
		synchronized (this.outstandingEntries) {
			serializer.writeObjectArray("outstanding", this.outstandingEntries);
		}

		synchronized (this.mostRecentEntries) {
			serializer.writeObjectArray("most-recent", this.mostRecentEntries);
		}
	}
}
