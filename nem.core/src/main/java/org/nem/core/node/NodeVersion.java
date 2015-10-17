package org.nem.core.node;

import org.nem.core.serialization.*;

import java.util.Objects;
import java.util.regex.*;

/**
 * Represents a node version.
 */
public class NodeVersion implements SerializableEntity {
	/**
	 * Zero version.
	 */
	public static final NodeVersion ZERO = new NodeVersion(0, 0, 0, null);

	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-(.*))?");

	private final int majorVersion;
	private final int minorVersion;
	private final int buildVersion;
	private final String tag;

	/**
	 * Creates a new node version.
	 *
	 * @param majorVersion The major version.
	 * @param minorVersion The minor version.
	 * @param buildVersion The build version.
	 */
	public NodeVersion(
			final int majorVersion,
			final int minorVersion,
			final int buildVersion) {
		this(majorVersion, minorVersion, buildVersion, null);
	}

	/**
	 * Creates a new node version.
	 *
	 * @param majorVersion The major version.
	 * @param minorVersion The minor version.
	 * @param buildVersion The build version.
	 * @param tag The optional version tag.
	 */
	public NodeVersion(
			final int majorVersion,
			final int minorVersion,
			final int buildVersion,
			final String tag) {
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
		this.buildVersion = buildVersion;
		this.tag = tag;
	}

	/**
	 * Deserializes a node version.
	 * TODO 20151017 J-B: why did you add this vs using readFrom/writeTo?
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeVersion(final Deserializer deserializer) {
		this.majorVersion = deserializer.readInt("majorVersion");
		this.minorVersion = deserializer.readInt("minorVersion");
		this.buildVersion = deserializer.readInt("buildVersion");
		this.tag = deserializer.readOptionalString("tag");
	}

	/**
	 * Parses a string into a node version.
	 *
	 * @param s The string.
	 * @return The node version.
	 */
	public static NodeVersion parse(final String s) {
		final Matcher matcher = VERSION_PATTERN.matcher(s);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format("pattern '%s' could not be parsed", s));
		}

		return new NodeVersion(
				Integer.parseInt(matcher.group(1)),
				Integer.parseInt(matcher.group(2)),
				Integer.parseInt(matcher.group(3)),
				matcher.group(5));
	}

	/**
	 * Gets the major version.
	 *
	 * @return The major version.
	 */
	public int getMajorVersion() {
		return this.majorVersion;
	}

	/**
	 * Gets the minor version.
	 *
	 * @return The minor version.
	 */
	public int getMinorVersion() {
		return this.minorVersion;
	}

	/**
	 * Gets the build version.
	 *
	 * @return The build version.
	 */
	public int getBuildVersion() {
		return this.buildVersion;
	}

	/**
	 * Gets the version tag.
	 *
	 * @return The version tag.
	 */
	public String getTag() {
		return this.tag;
	}

	@Override
	public int hashCode() {
		return this.majorVersion ^ this.minorVersion ^ this.buildVersion;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeVersion)) {
			return false;
		}

		final NodeVersion rhs = (NodeVersion)obj;
		return this.majorVersion == rhs.majorVersion &&
				this.minorVersion == rhs.minorVersion &&
				this.buildVersion == rhs.buildVersion &&
				Objects.equals(this.tag, rhs.tag);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%d.%d.%d", this.majorVersion, this.minorVersion, this.buildVersion));
		if (null != this.tag) {
			builder.append('-');
			builder.append(this.tag);
		}

		return builder.toString();
	}

	/**
	 * Writes a version object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param version The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final NodeVersion version) {
		serializer.writeString(label, version.toString());
	}

	/**
	 * Reads a version object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static NodeVersion readFrom(final Deserializer deserializer, final String label) {
		final String versionString = deserializer.readString(label);
		return NodeVersion.parse(versionString);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("majorVersion", this.majorVersion);
		serializer.writeInt("minorVersion", this.minorVersion);
		serializer.writeInt("buildVersion", this.buildVersion);
		serializer.writeString("tag", this.tag);
	}
}
