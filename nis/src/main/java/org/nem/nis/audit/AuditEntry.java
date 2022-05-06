package org.nem.nis.audit;

import org.nem.core.serialization.*;
import org.nem.core.time.*;

/**
 * An entry in an audit log.
 */
public class AuditEntry implements SerializableEntity {
	private final int id;
	private final String host;
	private final String path;
	private final TimeProvider timeProvider;
	private final TimeInstant startTime;

	/**
	 * Creates a new entry.
	 *
	 * @param id The id.
	 * @param host The host.
	 * @param path The path.
	 * @param timeProvider The time provider.
	 */
	public AuditEntry(final int id, final String host, final String path, final TimeProvider timeProvider) {
		this.id = id;
		this.host = host;
		this.path = path;
		this.timeProvider = timeProvider;
		this.startTime = timeProvider.getCurrentTime();
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gets the host.
	 *
	 * @return The host.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Gets the path.
	 *
	 * @return The path.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Gets the start time.
	 *
	 * @return The start time.
	 */
	public TimeInstant getStartTime() {
		return this.startTime;
	}

	/**
	 * Gets the elapsed time.
	 *
	 * @return The elapsed time.
	 */
	public int getElapsedTime() {
		return this.timeProvider.getCurrentTime().subtract(this.startTime);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("id", this.id);
		serializer.writeString("host", this.host);
		serializer.writeString("path", this.path);
		TimeInstant.writeTo(serializer, "start-time", this.startTime);
		serializer.writeInt("elapsed-time", this.getElapsedTime());
	}

	@Override
	public int hashCode() {
		return this.host.hashCode() ^ this.path.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AuditEntry)) {
			return false;
		}

		final AuditEntry rhs = (AuditEntry) obj;
		return this.host.equals(rhs.host) && this.path.equals(rhs.path);
	}

	@Override
	public String toString() {
		return String.format("#%d (%s -> %s)", this.id, this.host, this.path);
	}
}
