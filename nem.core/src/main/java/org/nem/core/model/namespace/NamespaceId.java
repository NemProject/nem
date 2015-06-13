package org.nem.core.model.namespace;

import com.sun.deploy.util.StringUtils;
import org.nem.core.serialization.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents a fully qualified namespace name
 */
public class NamespaceId implements SerializableEntity {
	private static final int MAX_ROOT_LENGTH = 16;
	private static final int MAX_SUBLEVEL_LENGTH = 40;
	private static final int MAX_DEPTH = 3;

	private final NamespaceIdPart[] namespaceIdParts;

	/**
	 * Creates a namespace id.
	 *
	 * @param name The fully qualified name.
	 */
	public NamespaceId(final String name) {
		this.namespaceIdParts = parse(name);
		if (!this.isValid()) {
			throw new IllegalArgumentException(String.format("%s is not a valid namespace.", name));
		}
	}

	/**
	 * Deserializes a namespace id.
	 *
	 * @param deserializer The deserializer.
	 */
	public NamespaceId(final Deserializer deserializer) {
		this(deserializer.readString("id"));
	}

	private NamespaceId(final NamespaceIdPart[] namespaceIdParts) {
		this.namespaceIdParts = namespaceIdParts;
		if (!isValid()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid namespace.", this.toString()));
		}
	}

	private static NamespaceIdPart[] parse(final String name) {
		return Arrays.stream(name.split("\\.", -1)).map(NamespaceIdPart::new).toArray(NamespaceIdPart[]::new);
	}

	private boolean isValid() {
		if (null == this.namespaceIdParts ||
			MAX_DEPTH < this.namespaceIdParts.length ||
			0 == this.namespaceIdParts.length) {
			return false;
		}

		for (int i = 0; i < this.namespaceIdParts.length; i++) {
			if (!this.namespaceIdParts[i].isValid() ||
					this.namespaceIdParts[i].toString().length() > getMaxAllowedLength(i)) {
				return false;
			}
		}

		return true;
	}

	private static int getMaxAllowedLength(final int level) {
		return 0 == level ? MAX_ROOT_LENGTH : MAX_SUBLEVEL_LENGTH;
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
	 * Creates a new namespace id by concatenating the given namespace id part to this namespace id.
	 *
	 * @param part the namespace id part.
	 * @return The concatenated namespace id.
	 */
	public NamespaceId concat(final NamespaceIdPart part) {
		return new NamespaceId(this + "." + part);
	}

	@Override
	public String toString() {
		return StringUtils.join(Arrays.stream(this.namespaceIdParts)
				.map(NamespaceIdPart::toString)
				.collect(Collectors.toList()), ".");
	}

	@Override
	public int hashCode() {
		return Arrays.stream(this.namespaceIdParts).map(NamespaceIdPart::hashCode).reduce((h1, h2) -> h1 ^ h2).get();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof NamespaceId)) {
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

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("id", this.toString());
	}
}
