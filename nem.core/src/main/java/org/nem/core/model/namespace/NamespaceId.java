package org.nem.core.model.namespace;

import com.sun.deploy.util.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a fully qualified namespace name
 */
public class NamespaceId {
	private static final int MAX_ROOT_LENGTH = 16;
	private static final int MAX_SUBLEVEL_LENGTH = 40;
	private static final int MAX_DEPTH = 3;

	private final String[] fields;

	/**
	 * Creates a namespace id.
	 *
	 * @param name The fully qualified name.
	 */
	public NamespaceId(final String name) {
		this.fields = parse(name);
		if (!validate(this.fields)) {
			throw new IllegalArgumentException(String.format("%s is not a valid namespace.", name));
		}
	}

	private NamespaceId(final String[] fields) {
		this.fields = fields;
		if (!validate(this.fields)) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid namespace.", this.toString()));
		}
	}

	private static String[] parse(final String name) {
		return name.toLowerCase().split("\\.", -1);
	}

	private static boolean validate(final String[] fields) {
		if (MAX_DEPTH < fields.length ||
			0 == fields.length) {
			return false;
		}

		Pattern p = Pattern.compile("[^a-zA-Z0-9_-]");
		for (int i = 0; i < fields.length; i++) {
			if (getMaxAllowedLength(i) < fields[i].length() ||
				fields[i].isEmpty() ||
				p.matcher(fields[i]).find()) {
				return false;
			}
		}

		return true;
	}

	private static int getMaxAllowedLength(final int level) {
		return 0 == level ? MAX_ROOT_LENGTH : MAX_SUBLEVEL_LENGTH;
	}

	/**
	 * Gets the root namespace
	 *
	 * @return The root namespace id.
	 */
	public NamespaceId getRoot() {
		return new NamespaceId(this.fields[0]);
	}

	/**
	 * Gets the parent of this namespace id.
	 *
	 * @return the parent namespace id.
	 */
	public NamespaceId getParent() {
		return 1 == this.fields.length ? null : new NamespaceId(Arrays.copyOfRange(this.fields, 0, this.fields.length - 1));
	}

	@Override
	public String toString() {
		return StringUtils.join(Arrays.stream(this.fields).collect(Collectors.toList()), ".");
	}

	@Override
	public int hashCode() {
		return Arrays.stream(this.fields).map(String::hashCode).reduce((h1, h2) -> h1 ^ h2).get();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof NamespaceId)) {
			return false;
		}

		final NamespaceId rhs = (NamespaceId)obj;
		if (this.fields.length != rhs.fields.length) {
			return false;
		}

		for (int i = 0; i < this.fields.length; i++) {
			if (!this.fields[i].equals(rhs.fields[i])) {
				return false;
			}
		}

		return true;
	}
}
