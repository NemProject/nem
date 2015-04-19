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
	private final int networkId;
	private final int featuresBitmask;

	/**
	 * Creates a new node meta data.
	 *
	 * @param platform The platform.
	 * @param application The application.
	 */
	public NodeMetaData(
			final String platform,
			final String application) {
		this(platform, application, null, 0, 0);
	}

	/**
	 * Creates a new node meta data.
	 *
	 * @param platform The platform.
	 * @param application The application.
	 * @param version The version.
	 * @param networkId The network id.
	 * @param featuresBitmask A bitmask of enabled node features.
	 */
	public NodeMetaData(
			final String platform,
			final String application,
			final NodeVersion version,
			final int networkId,
			final int featuresBitmask) {
		this.platform = platform;
		this.application = application;
		this.version = null == version ? NodeVersion.ZERO : version;
		this.networkId = networkId;
		this.featuresBitmask = featuresBitmask;
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
		final Integer bitmask = deserializer.readOptionalInt("features");
		this.featuresBitmask = null == bitmask ? 0 : bitmask;
		final Integer networkVersion = deserializer.readOptionalInt("networkId");
		this.networkId = null == networkVersion ? 0 : networkVersion;
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

	/**
	 * Gets the network id.
	 *
	 * @return The network id.
	 */
	public int getNetworkId() {
		return this.networkId;
	}

	/**
	 * Gets the bitmask of enabled features.
	 *
	 * @return The bitmask of enabled features.
	 */
	public int getFeaturesBitmask() {
		return this.featuresBitmask;
	}

	@Override
	public void serialize(final Serializer serializer) {
		NodeVersion.writeTo(serializer, "version", this.version);
		serializer.writeString("platform", this.platform);
		serializer.writeString("application", this.application);
		serializer.writeInt("features", this.featuresBitmask);
		serializer.writeInt("networkId", this.networkId);
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
				this.version,
				this.networkId,
				this.featuresBitmask
		};
	}
}
