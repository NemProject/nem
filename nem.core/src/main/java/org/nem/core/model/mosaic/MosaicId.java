package org.nem.core.model.mosaic;

import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

import java.util.regex.*;

/**
 * The (case-insensitive) mosaic unique identifier.
 */
public class MosaicId implements SerializableEntity {
	// TODO 20150830 J-*: for static finals, we should pick either UpperCamelCase or ALL_CAPS :)
	// TODO 20150831 BR -> J: usually we have UpperCamelCase for enums and ALL_CAPS for static finals.
	private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9 '_-]*");

	// TODO 20150830 J-G: why don't you want spaces in names? i added them back but if you don't like we can discuss
	private static final Pattern MOSAIC_ID_PATTERN = Pattern.compile(
			"([a-zA-Z0-9._-]+) \\* ([a-zA-Z0-9'_-]+([a-zA-Z0-9 '_-]*[a-zA-Z0-9'_-])?)");

	private final NamespaceId namespaceId;
	private final String name;

	/**
	 * Creates a mosaic id.
	 *
	 * @param namespaceId The namespace id.
	 * @param name The name.
	 */
	public MosaicId(final NamespaceId namespaceId, final String name) {
		this.namespaceId = namespaceId;
		this.name = name;
		this.validate();
	}

	/**
	 * Creates a mosaic id from a string.
	 *
	 * @param mosaicId The mosaic id as string.
	 */
	public static MosaicId parse(final String mosaicId) {
		final Matcher matcher = MOSAIC_ID_PATTERN.matcher(mosaicId);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format("pattern '%s' could not be parsed", mosaicId));
		}

		return new MosaicId(new NamespaceId(matcher.group(1)), matcher.group(2));
	}

	/**
	 * Deserializes a mosaic id.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicId(final Deserializer deserializer) {
		this.namespaceId = NamespaceId.readFrom(deserializer, "namespaceId");
		this.name = deserializer.readString("name");
		this.validate();
	}

	private void validate() {
		final int maxNameLength = 32;
		MustBe.match(this.name, "name", NAME_PATTERN, maxNameLength);
		MustBe.notNull(this.namespaceId, "namespaceId");
	}

	/**
	 * Gets the namespace id.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.namespaceId;
	}

	/**
	 * Gets the name.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public void serialize(final Serializer serializer) {
		NamespaceId.writeTo(serializer, "namespaceId", this.namespaceId);
		serializer.writeString("name", this.name);
	}

	@Override
	public String toString() {
		return String.format("%s * %s", this.namespaceId, this.name);
	}

	@Override
	public int hashCode() {
		return String.format("%s * %s", this.namespaceId, this.name.toLowerCase()).hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicId)) {
			return false;
		}

		final MosaicId rhs = (MosaicId)obj;

		// should not be case sensitive
		return this.namespaceId.equals(rhs.namespaceId) &&
				this.name.toLowerCase().equals(rhs.name.toLowerCase());
	}
}
