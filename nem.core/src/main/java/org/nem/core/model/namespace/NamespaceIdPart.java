package org.nem.core.model.namespace;

import org.nem.core.serialization.*;

import java.util.regex.Pattern;

/**
 * Represents a part of a namespace id
 */
public class NamespaceIdPart {
	private final String id;
	private static final Pattern IsValidPattern = Pattern.compile("[^a-zA-Z0-9_-]");

	/**
	 * Creates a namespace id part from a string.
	 *
	 * @param id The id.
	 */
	public NamespaceIdPart(final String id) {
		this.id = id.toLowerCase();
		if (!this.isValid()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid namespace id part.", this.id));
		}
	}

	/**
	 * Deserializes a namespace id.
	 *
	 * @param deserializer The deserializer.
	 */
	public NamespaceIdPart(final Deserializer deserializer) {
		this(deserializer.readString("id"));
	}

	/**
	 * Gets a value indicating whether or not the namespace id part is valid.
	 *
	 * @return true if valid, false otherwise.
	 */
	public boolean isValid() {
		return !this.id.isEmpty() && !IsValidPattern.matcher(this.id).find();
	}

	@Override
	public String toString() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NamespaceIdPart)) {
			return false;
		}

		final NamespaceIdPart rhs = (NamespaceIdPart)obj;
		return this.id.equals(rhs.id);
	}
}
