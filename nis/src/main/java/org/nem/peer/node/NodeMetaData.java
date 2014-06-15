package org.nem.peer.node;

import org.nem.core.serialization.*;

import java.util.Arrays;

/**
 * Meta data about a node.
 */
public class NodeMetaData implements SerializableEntity {
	public static final ObjectDeserializer<NodeMetaData> DESERIALIZER = new ObjectDeserializer<NodeMetaData>() {
		@Override
		public NodeMetaData deserialize(Deserializer deserializer) {
			return new NodeMetaData(deserializer);
		}
	};

	private final String platform;
	private final String application;
	private final String version;

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
			final String version) {
		this.platform = platform;
		this.application = application;
		this.version = version;
	}

	/**
	 * Deserializes a node meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeMetaData(final Deserializer deserializer) {
		this.platform = deserializer.readString("platform");
		this.application = deserializer.readString("application");
		this.version = deserializer.readString("version");
	}

	/**
	 * Gets the platform.
	 *
	 * @return The platform.
	 */
	public String getPlatform() {
		return this.platform;
	}

	/**
	 * Gets the application.
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
	public String getVersion() {
		return this.version;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("platform", this.platform);
		serializer.writeString("application", this.application);
		serializer.writeString("version", this.version);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.getParts());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NodeMetaData))
			return false;

		final NodeMetaData rhs = (NodeMetaData)obj;
		return Arrays.equals(this.getParts(), rhs.getParts());
	}

	private String[] getParts() {
		return new String[] {
			this.platform,
			this.application,
			this.version
		};
	}
}
