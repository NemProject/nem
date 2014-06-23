package org.nem.nis.audit;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * A collection of audit entries.
 */
public class AuditCollection implements SerializableEntity {
	private final List<AuditEntry> outstandingEntries = new ArrayList<>();
	private final Deque<AuditEntry> mostRecentEntries = new ArrayDeque<>();
	final int maxEntries;

	/**
	 * Creates a new audit collection.
	 *
	 * @param maxEntries The maximum number of recent entries to keep.
	 */
	public AuditCollection(final int maxEntries) {
		this.maxEntries = maxEntries;
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
	 * @param entry The entry.
	 */
	public void add(final AuditEntry entry) {
		synchronized (this.mostRecentEntries) {
			if (this.mostRecentEntries.size() >= this.maxEntries)
				this.mostRecentEntries.removeLast();

			this.mostRecentEntries.addFirst(entry);
		}

		synchronized (this.outstandingEntries) {
			this.outstandingEntries.add(entry);
		}
	}

	/**
	 * Removes an entry from the audit collection.
	 *
	 * @param entry The entry.
	 */
	public void remove(final AuditEntry entry) {
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
