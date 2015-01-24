package org.nem.core.node;

import org.nem.core.serialization.*;

import java.util.Arrays;

/**
 * Meta data about a node.
 */
public class NodeMetaData implements SerializableEntity {
	private final String platform;
	private final String application;
	private final NodeVersion version;

	/**
	 * Creates a new node meta data.
	 *
	 * @param platform The platform.
	 * @param application The application.
	 * @param version The version.
	 */
	public NodeMetaData(
			final String platform,
			final String application,
			final NodeVersion version) {
		this.platform = platform;
		this.application = application;
		this.version = null == version ? NodeVersion.ZERO : version;
	}

	/**
	 * Deserializes a node meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeMetaData(final Deserializer deserializer) {
		this.version = NodeVersion.readFrom(deserializer, "version");
		this.platform = deserializer.readOptionalString("platform");
		this.application = deserializer.readOptionalString("application");
	}

	/**
	 * Gets the (optional) platform.
	 *
	 * @return The platform.
	 */
	public String getPlatform() {
		return this.platform;
	}

	/**
	 * Gets the (optional) application.
	 *
	 * @return The application.
	 */
	public String getApplication() {
		return this.application;
	}

	/**
	 * Gets the version.
	 *
	 * @return The version.
	 */
	public NodeVersion getVersion() {
		return this.version;
	}

	@Override
	public void serialize(final Serializer serializer) {
		NodeVersion.writeTo(serializer, "version", this.version);
		serializer.writeString("platform", this.platform);
		serializer.writeString("application", this.application);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.getParts());
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeMetaData)) {
			return false;
		}

		final NodeMetaData rhs = (NodeMetaData)obj;
		return Arrays.equals(this.getParts(), rhs.getParts());
	}

	private Object[] getParts() {
		return new Object[] {
				this.platform,
				this.application,
				this.version
		};
	}
}
