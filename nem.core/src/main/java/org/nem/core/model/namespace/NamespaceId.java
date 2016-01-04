package org.nem.core.model.namespace;

import org.nem.core.serialization.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents a fully qualified namespace name (immutable).
 */
public class NamespaceId {
	public static final int MAX_ROOT_LENGTH = 16;
	public static final int MAX_SUBLEVEL_LENGTH = 64;
	private static final int MAX_DEPTH = 3;

	private final NamespaceIdPart[] namespaceIdParts;

	/**
	 * Creates a namespace id.
	 *
	 * @param name The fully qualified name.
	 */
	public NamespaceId(final String name) {
		this(parse(name));
	}

	private NamespaceId(final NamespaceIdPart[] namespaceIdParts) {
		this.namespaceIdParts = namespaceIdParts;
		if (!this.isValid()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid namespace.", this.toString()));
		}
	}

	private static NamespaceIdPart[] parse(final String name) {
		return Arrays.stream(name.split("\\.", -1)).map(NamespaceIdPart::new).toArray(NamespaceIdPart[]::new);
	}

	private boolean isValid() {
		if (MAX_DEPTH < this.namespaceIdParts.length || 0 == this.namespaceIdParts.length) {
			return false;
		}

		for (int i = 0; i < this.namespaceIdParts.length; i++) {
			final NamespaceIdPart part = this.namespaceIdParts[i];
			if (!part.isValid() || part.toString().length() > getMaxAllowedLength(i)) {
				return false;
			}
		}

		return true;
	}

	private static int getMaxAllowedLength(final int level) {
		return 0 == level ? MAX_ROOT_LENGTH : MAX_SUBLEVEL_LENGTH;
	}

	/**
	 * Gets a value indicating whether or not the id represents a root id.
	 *
	 * @return True if it is a root id, false otherwise.
	 */
	public boolean isRoot() {
		return 1 == this.namespaceIdParts.length;
	}

	/**
	 * Gets the root namespace id.
	 *
	 * @return The root namespace id.
	 */
	public NamespaceId getRoot() {
		return new NamespaceId(this.namespaceIdParts[0].toString());
	}

	/**
	 * Gets the parent of this namespace id.
	 *
	 * @return The parent namespace id.
	 */
	public NamespaceId getParent() {
		return 1 == this.namespaceIdParts.length
				? null :
				new NamespaceId(Arrays.copyOfRange(this.namespaceIdParts, 0, this.namespaceIdParts.length - 1));
	}

	/**
	 * Gets the last part of this namespace id (example: last part of foo.bar.baz is baz).
	 *
	 * @return The last part of this namespace id.
	 */
	public NamespaceIdPart getLastPart() {
		return this.namespaceIdParts[this.namespaceIdParts.length - 1];
	}

	/**
	 * Gets the namespace id level (level 0 is root).
	 *
	 * @return The level.
	 */
	public int getLevel() {
		return this.namespaceIdParts.length - 1;
	}

	/**
	 * Creates a new namespace id by concatenating the given namespace id part to this namespace id.
	 *
	 * @param part the namespace id part.
	 * @return The concatenated namespace id.
	 */
	public NamespaceId concat(final NamespaceIdPart part) {
		final NamespaceIdPart[] parts = new NamespaceIdPart[this.namespaceIdParts.length + 1];
		System.arraycopy(this.namespaceIdParts, 0, parts, 0, this.namespaceIdParts.length);
		parts[this.namespaceIdParts.length] = part;
		return new NamespaceId(parts);
	}

	// region inline serialization

	/**
	 * Writes a namespace id.
	 *
	 * @param serializer The serializer to use.
	 * @param label The label.
	 * @param namespaceId The namespace id.
	 */
	public static void writeTo(final Serializer serializer, final String label, final NamespaceId namespaceId) {
		serializer.writeString(label, namespaceId.toString());
	}

	/**
	 * Reads a namespace id.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The label.
	 * @return The namespace id.
	 */
	public static NamespaceId readFrom(final Deserializer deserializer, final String label) {
		return new NamespaceId(deserializer.readString(label));
	}

	// endregion

	@Override
	public String toString() {
		return Arrays.stream(this.namespaceIdParts)
				.map(NamespaceIdPart::toString)
				.collect(Collectors.joining("."));
	}

	@Override
	public int hashCode() {
		return Arrays.stream(this.namespaceIdParts).map(NamespaceIdPart::hashCode).reduce((h1, h2) -> h1 ^ h2).get();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NamespaceId)) {
			return false;
		}

		final NamespaceId rhs = (NamespaceId)obj;
		if (this.namespaceIdParts.length != rhs.namespaceIdParts.length) {
			return false;
		}

		for (int i = 0; i < this.namespaceIdParts.length; i++) {
			if (!this.namespaceIdParts[i].equals(rhs.namespaceIdParts[i])) {
				return false;
			}
		}

		return true;
	}
}
