package org.nem.nis.audit;

import org.nem.core.serialization.*;

/**
 * An entry in an audit log.
 */
public class AuditEntry implements SerializableEntity {
	private final String host;
	private final String path;

	/**
	 * Creates a new entry.
	 *
	 * @param host The host.
	 * @param path The path.
	 */
	public AuditEntry(final String host, final String path) {
		this.host = host;
		this.path = path;
	}

	/**
	 * Deserializes an entry.
	 *
	 * @param deserializer The deserializer.
	 */
	public AuditEntry(final Deserializer deserializer) {
		this.host = deserializer.readString("host");
		this.path = deserializer.readString("path");
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

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("host", this.host);
		serializer.writeString("path", this.path);
	}

	@Override
	public int hashCode() {
		return this.host.hashCode() ^ this.path.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AuditEntry))
			return false;

		final AuditEntry rhs = (AuditEntry)obj;
		return this.host.equals(rhs.host) && this.path.equals(rhs.path);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.host, this.path);
	}
}
